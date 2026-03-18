package com.extractor.core.model;

import java.util.Objects;

/**
 * Represents a DEPENDS_ON relationship between two Maven/Gradle artifacts.
 */
public final class DependsOnEdge {

    private final String fromGroupId;
    private final String fromArtifactId;
    private final String fromVersion;
    private final String toGroupId;
    private final String toArtifactId;
    private final String toVersion;
    private final String scope;
    private final boolean isTransitive;
    private final String repoName;

    public DependsOnEdge(String fromGroupId, String fromArtifactId, String fromVersion,
                         String toGroupId, String toArtifactId, String toVersion,
                         String scope, boolean isTransitive, String repoName) {
        this.fromGroupId = fromGroupId;
        this.fromArtifactId = fromArtifactId;
        this.fromVersion = fromVersion;
        this.toGroupId = toGroupId;
        this.toArtifactId = toArtifactId;
        this.toVersion = toVersion;
        this.scope = scope;
        this.isTransitive = isTransitive;
        this.repoName = repoName;
    }

    public String getFromGroupId() { return fromGroupId; }
    public String getFromArtifactId() { return fromArtifactId; }
    public String getFromVersion() { return fromVersion; }
    public String getToGroupId() { return toGroupId; }
    public String getToArtifactId() { return toArtifactId; }
    public String getToVersion() { return toVersion; }
    public String getScope() { return scope; }
    public boolean isTransitive() { return isTransitive; }
    public String getRepoName() { return repoName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DependsOnEdge)) return false;
        DependsOnEdge that = (DependsOnEdge) o;
        return Objects.equals(fromGroupId, that.fromGroupId)
                && Objects.equals(fromArtifactId, that.fromArtifactId)
                && Objects.equals(toGroupId, that.toGroupId)
                && Objects.equals(toArtifactId, that.toArtifactId);
    }

    @Override
    public int hashCode() { return Objects.hash(fromGroupId, fromArtifactId, toGroupId, toArtifactId); }

    @Override
    public String toString() {
        return "DependsOnEdge{from='" + fromGroupId + ":" + fromArtifactId + "', to='" + toGroupId + ":" + toArtifactId + "'}";
    }
}
