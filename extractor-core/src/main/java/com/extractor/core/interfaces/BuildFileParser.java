package com.extractor.core.interfaces;

import com.extractor.core.enums.BuildTool;
import com.extractor.core.exceptions.BuildParseException;
import com.extractor.core.model.DependsOnEdge;
import com.extractor.core.model.RepoConfig;

import java.nio.file.Path;
import java.util.List;

/**
 * Resolves the full dependency tree of a Maven or Gradle project and returns
 * it as a list of {@link DependsOnEdge} objects ready to be stored in the graph.
 *
 * <p>A separate implementation exists for each build tool (Maven Invoker API and
 * Gradle Tooling API). The correct implementation is selected via {@link #supports(BuildTool)}.
 */
public interface BuildFileParser {

    /**
     * Returns {@code true} if this parser supports the given build tool.
     *
     * @param buildTool The build tool to check.
     * @return {@code true} if this implementation handles the given build tool.
     */
    boolean supports(BuildTool buildTool);

    /**
     * Resolve the full (including transitive) dependency tree of the project rooted at
     * {@code projectDir}.
     *
     * <p>The resolved JAR paths must be accessible on the local filesystem so that
     * {@link JavaSourceParser} can create {@code JarTypeSolver} instances from them.
     *
     * @param projectDir Root directory of the Maven/Gradle project (contains pom.xml or build.gradle).
     * @param repo       Configuration of the repository this project belongs to.
     * @return Flat list of all dependency edges, both direct and transitive.
     * @throws BuildParseException If dependency resolution fails (network error, missing Maven/Gradle).
     */
    List<DependsOnEdge> resolveDependencies(Path projectDir, RepoConfig repo) throws BuildParseException;

    /**
     * Returns the absolute paths of all resolved dependency JARs for use with
     * JavaParser's {@code JarTypeSolver}.
     *
     * @param projectDir Root directory of the Maven/Gradle project.
     * @param repo       Repository configuration.
     * @return List of absolute paths to all resolved JAR files.
     * @throws BuildParseException If dependency resolution fails.
     */
    List<java.nio.file.Path> resolveJarPaths(Path projectDir, RepoConfig repo) throws BuildParseException;
}
