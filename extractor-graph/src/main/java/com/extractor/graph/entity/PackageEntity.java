package com.extractor.graph.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Plain-POJO representing a Java package (e.g. {@code com.example.billing}).
 * Stored in-memory via {@link com.extractor.graph.store.GraphStore}.
 */
public class PackageEntity {

    private String fullyQualifiedName;
    private String repoName;
    private String artifactId;

    private List<ClassEntity> classes = new ArrayList<>();

    protected PackageEntity() {}

    public PackageEntity(String fullyQualifiedName, String repoName, String artifactId) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.repoName = repoName;
        this.artifactId = artifactId;
    }

    public Long getId() { return (long) Objects.hash(fullyQualifiedName, repoName); }
    public String getFullyQualifiedName() { return fullyQualifiedName; }
    public String getRepoName() { return repoName; }
    public String getArtifactId() { return artifactId; }
    public List<ClassEntity> getClasses() { return classes; }
    public void setClasses(List<ClassEntity> classes) { this.classes = classes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PackageEntity)) return false;
        PackageEntity that = (PackageEntity) o;
        return Objects.equals(fullyQualifiedName, that.fullyQualifiedName)
                && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullyQualifiedName, repoName);
    }
}
