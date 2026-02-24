package com.extractor.ingestion.build;

import com.extractor.core.enums.BuildTool;
import com.extractor.core.exceptions.BuildParseException;
import com.extractor.core.interfaces.BuildFileParser;
import com.extractor.core.model.DependsOnEdge;
import com.extractor.core.model.RepoConfig;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Maven Invoker API-backed implementation of {@link BuildFileParser}.
 *
 * <p>Invokes {@code mvn dependency:list} with an output file, then parses the
 * artifact lines to produce {@link DependsOnEdge} objects with scope and transitivity info.
 *
 * <p>Requires Maven to be installed and on the PATH (or set via {@code MAVEN_HOME}).
 * Per spec §12.3, the Docker image adds {@code RUN apt-get install -y maven}.
 */
@Component
public class MavenBuildParserImpl implements BuildFileParser {

    private static final Logger log = LoggerFactory.getLogger(MavenBuildParserImpl.class);

    @Override
    public boolean supports(BuildTool buildTool) {
        return BuildTool.MAVEN == buildTool;
    }

    @Override
    public List<DependsOnEdge> resolveDependencies(Path projectDir, RepoConfig repo)
            throws BuildParseException {
        Path outputFile = runDependencyList(projectDir, repo);
        return parseDependencyListOutput(outputFile, projectDir, repo);
    }

    @Override
    public List<Path> resolveJarPaths(Path projectDir, RepoConfig repo)
            throws BuildParseException {
        Path outputFile = runDependencyList(projectDir, repo);
        return parseJarPaths(outputFile);
    }

    // ── Private implementation ──────────────────────────────────────────

    private Path runDependencyList(Path projectDir, RepoConfig repo) throws BuildParseException {
        try {
            Path outputFile = Files.createTempFile("maven-deps-" + repo.name() + "-", ".txt");

            InvocationRequest request = new DefaultInvocationRequest();
            request.setPomFile(projectDir.resolve("pom.xml").toFile());
            request.setGoals(Arrays.asList(
                    "dependency:list",
                    "-DincludeScope=compile",
                    "-DoutputAbsoluteArtifactFilename=true",
                    "-DoutputFile=" + outputFile.toAbsolutePath(),
                    "-DappendOutput=false",
                    "-q"
            ));
            request.setBatchMode(true);

            Invoker invoker = new DefaultInvoker();
            // Allow Maven home to be configured via env or use default PATH resolution
            String mavenHome = System.getenv("MAVEN_HOME");
            if (mavenHome != null && !mavenHome.isBlank()) {
                invoker.setMavenHome(new File(mavenHome));
            }

            InvocationResult result = invoker.execute(request);
            if (result.getExitCode() != 0) {
                throw new BuildParseException(
                        "Maven dependency:list failed with exit code " + result.getExitCode()
                                + " for project: " + projectDir,
                        BuildTool.MAVEN, projectDir);
            }

            return outputFile;
        } catch (IOException e) {
            throw new BuildParseException("IO error running Maven for: " + projectDir, BuildTool.MAVEN, projectDir, e);
        } catch (MavenInvocationException e) {
            throw new BuildParseException("Maven invocation failed for: " + projectDir, BuildTool.MAVEN, projectDir, e);
        }
    }

    private List<DependsOnEdge> parseDependencyListOutput(Path outputFile, Path projectDir, RepoConfig repo)
            throws BuildParseException {
        List<DependsOnEdge> edges = new ArrayList<>();

        // Read the declaring artifact from pom.xml (simple parsing)
        String[] fromCoords = readPomCoords(projectDir);

        try {
            List<String> lines = Files.readAllLines(outputFile);
            for (String line : lines) {
                line = line.strip();
                // Maven dependency:list format: groupId:artifactId:type:version:scope[:absPath]
                if (line.isBlank() || line.startsWith("[INFO]") || line.startsWith("The following")) continue;

                String[] parts = line.split(":");
                if (parts.length < 5) continue;

                String groupId = parts[0];
                String artifactId = parts[1];
                String type = parts[2];
                String version = parts[3];
                String scope = parts[4];
                boolean isTransitive = isTransitiveDependency(line);

                edges.add(new DependsOnEdge(
                        fromCoords[0], fromCoords[1], fromCoords[2],
                        groupId, artifactId, version, scope, isTransitive, repo.name()));
            }
        } catch (IOException e) {
            throw new BuildParseException("Could not read Maven output file: " + outputFile, BuildTool.MAVEN, projectDir, e);
        }

        log.debug("Resolved {} Maven dependencies for repo '{}'", edges.size(), repo.name());
        return edges;
    }

    private List<Path> parseJarPaths(Path outputFile) throws BuildParseException {
        List<Path> jarPaths = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(outputFile);
            for (String line : lines) {
                line = line.strip();
                if (line.isBlank() || line.startsWith("[INFO]") || line.startsWith("The following")) continue;

                String[] parts = line.split(":");
                // With -DoutputAbsoluteArtifactFilename=true the path is the last field
                if (parts.length >= 6) {
                    Path jarPath = Path.of(parts[parts.length - 1].strip());
                    if (jarPath.toFile().exists() && jarPath.toString().endsWith(".jar")) {
                        jarPaths.add(jarPath);
                    }
                }
            }
        } catch (IOException e) {
            throw new BuildParseException("Could not read Maven dependency output", BuildTool.MAVEN, Path.of("."), e);
        }
        return jarPaths;
    }

    private String[] readPomCoords(Path projectDir) {
        // Minimal POM coordinate extraction — production code should use Maven Model
        try {
            String pom = Files.readString(projectDir.resolve("pom.xml"));
            String groupId = extractXmlValue(pom, "groupId", "com.unknown");
            String artifactId = extractXmlValue(pom, "artifactId", "unknown");
            String version = extractXmlValue(pom, "version", "0.0.0");
            return new String[]{groupId, artifactId, version};
        } catch (IOException e) {
            return new String[]{"com.unknown", "unknown", "0.0.0"};
        }
    }

    private String extractXmlValue(String xml, String tag, String defaultValue) {
        int start = xml.indexOf("<" + tag + ">");
        int end = xml.indexOf("</" + tag + ">");
        if (start < 0 || end < 0) return defaultValue;
        return xml.substring(start + tag.length() + 2, end).strip();
    }

    /**
     * Heuristically determines if a dependency is transitive.
     * Maven dependency:list indents transitive deps with extra whitespace.
     */
    private boolean isTransitiveDependency(String rawLine) {
        return rawLine.startsWith("   ");
    }
}
