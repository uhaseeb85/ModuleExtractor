package com.extractor.core.model;

/**
 * Represents a DEPENDS_ON relationship between two Maven/Gradle artifacts.
 *
 * @param fromGroupId     groupId of the artifact declaring the dependency.
 * @param fromArtifactId  artifactId of the artifact declaring the dependency.
 * @param fromVersion     version of the declaring artifact.
 * @param toGroupId       groupId of the dependency.
 * @param toArtifactId    artifactId of the dependency.
 * @param toVersion       resolved version of the dependency.
 * @param scope           Maven/Gradle scope: compile, test, provided, runtime.
 * @param isTransitive    Whether this dependency was resolved transitively.
 * @param repoName        Repository the declaring artifact belongs to.
 */
public record DependsOnEdge(
        String fromGroupId,
        String fromArtifactId,
        String fromVersion,
        String toGroupId,
        String toArtifactId,
        String toVersion,
        String scope,
        boolean isTransitive,
        String repoName
) {}
