package com.extractor.ingestion.build;

import com.extractor.core.enums.BuildTool;
import com.extractor.core.exceptions.BuildParseException;
import com.extractor.core.interfaces.BuildFileParser;
import com.extractor.core.model.DependsOnEdge;
import com.extractor.core.model.RepoConfig;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Gradle Tooling API-backed implementation of {@link BuildFileParser}.
 *
 * <p>Opens a {@link ProjectConnection} to the Gradle project, fetches the
 * {@link EclipseProject} model, and maps classpath entries to {@link DependsOnEdge} objects.
 */
@Component
public class GradleBuildParserImpl implements BuildFileParser {

    private static final Logger log = LoggerFactory.getLogger(GradleBuildParserImpl.class);

    @Override
    public boolean supports(BuildTool buildTool) {
        return BuildTool.GRADLE == buildTool;
    }

    @Override
    public List<DependsOnEdge> resolveDependencies(Path projectDir, RepoConfig repo)
            throws BuildParseException {
        List<DependsOnEdge> edges = new ArrayList<>();
        EclipseProject eclipseProject = fetchEclipseModel(projectDir, repo);

        String fromArtifactId = projectDir.getFileName().toString();

        for (var classpathEntry : eclipseProject.getClasspath()) {
            Path jarPath = classpathEntry.getFile().toPath();
            // Derive coordinates from the JAR file name heuristically;
            // production code should use the Dependency model from Gradle tooling
            String[] coords = deriveCoordinatesFromJarPath(jarPath, fromArtifactId, repo);

            edges.add(new DependsOnEdge(
                    "com.unknown", fromArtifactId, "unspecified",
                    coords[0], coords[1], coords[2],
                    "compile", false, repo.name()));
        }

        log.debug("Resolved {} Gradle dependencies for repo '{}'", edges.size(), repo.name());
        return edges;
    }

    @Override
    public List<Path> resolveJarPaths(Path projectDir, RepoConfig repo)
            throws BuildParseException {
        List<Path> jarPaths = new ArrayList<>();
        EclipseProject eclipseProject = fetchEclipseModel(projectDir, repo);

        for (var classpathEntry : eclipseProject.getClasspath()) {
            File file = classpathEntry.getFile();
            if (file != null && file.exists() && file.getName().endsWith(".jar")) {
                jarPaths.add(file.toPath());
            }
        }
        log.debug("Resolved {} JAR paths for repo '{}'", jarPaths.size(), repo.name());
        return jarPaths;
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private EclipseProject fetchEclipseModel(Path projectDir, RepoConfig repo)
            throws BuildParseException {
        try {
            ProjectConnection connection = GradleConnector.newConnector()
                    .forProjectDirectory(projectDir.toFile())
                    .connect();
            try {
                return connection.getModel(EclipseProject.class);
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            throw new BuildParseException(
                    "Gradle Tooling API failed for project: " + projectDir, BuildTool.GRADLE, projectDir, e);
        }
    }

    /**
     * Attempts to derive Maven-style coordinates from a JAR file path.
     * In a Gradle local cache the path typically contains groupId/artifactId/version segments.
     *
     * <p>Example: {@code ~/.gradle/caches/modules-2/files-2.1/org.springframework/spring-core/6.1.1/...}
     */
    private String[] deriveCoordinatesFromJarPath(Path jarPath, String defaultArtifactId, RepoConfig repo) {
        try {
            // Walk up the path to find version, artifactId, groupId segments
            Path versionDir = jarPath.getParent().getParent(); // hash -> version
            Path artifactDir = versionDir.getParent();          // version -> artifactId
            Path groupDir = artifactDir.getParent();            // artifactId -> groupId

            String version = versionDir.getFileName().toString();
            String artifactId = artifactDir.getFileName().toString();
            String groupId = groupDir.getFileName().toString();

            return new String[]{groupId, artifactId, version};
        } catch (Exception e) {
            // Fall back to filename-based guess
            String fileName = jarPath.getFileName().toString().replace(".jar", "");
            return new String[]{"unknown", fileName, "unknown"};
        }
    }
}
