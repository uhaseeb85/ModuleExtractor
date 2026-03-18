package com.extractor.graph.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Plain-POJO representing a Maven/Gradle artifact (groupId:artifactId:version).
 * Stored in-memory via {@link com.extractor.graph.store.GraphStore}.
 */
public class ArtifactEntity {

    private String groupId;
    private String artifactId;
    private String version;

    /** JAR, POM, or WAR */
    private String type;

    private String repoName;

    private List<PackageEntity> packages = new ArrayList<>();
    private List<ArtifactDependency> dependencies = new ArrayList<>();

    protected ArtifactEntity() {}

    public ArtifactEntity(String groupId, String artifactId, String version, String type, String repoName) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.repoName = repoName;
    }

    public Long getId() { return (long) Objects.hash(groupId, artifactId, version); }
    public String getGroupId() { return groupId; }
    public String getArtifactId() { return artifactId; }
    public String getVersion() { return version; }
    public String getType() { return type; }
    public String getRepoName() { return repoName; }
    public List<PackageEntity> getPackages() { return packages; }
    public void setPackages(List<PackageEntity> packages) { this.packages = packages; }
    public List<ArtifactDependency> getDependencies() { return dependencies; }
    public void setDependencies(List<ArtifactDependency> dependencies) { this.dependencies = dependencies; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArtifactEntity)) return false;
        ArtifactEntity that = (ArtifactEntity) o;
        return Objects.equals(groupId, that.groupId)
                && Objects.equals(artifactId, that.artifactId)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }
}
