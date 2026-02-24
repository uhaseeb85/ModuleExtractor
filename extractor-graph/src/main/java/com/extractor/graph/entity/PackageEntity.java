package com.extractor.graph.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Neo4j node representing a Java package (e.g. {@code com.example.billing}).
 */
@Node("Package")
public class PackageEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Property(name = "fullyQualifiedName")
    private String fullyQualifiedName;

    @Property(name = "repoName")
    private String repoName;

    @Property(name = "artifactId")
    private String artifactId;

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<ClassEntity> classes = new ArrayList<>();

    protected PackageEntity() {}

    public PackageEntity(String fullyQualifiedName, String repoName, String artifactId) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.repoName = repoName;
        this.artifactId = artifactId;
    }

    public Long getId() { return id; }
    public String getFullyQualifiedName() { return fullyQualifiedName; }
    public String getRepoName() { return repoName; }
    public String getArtifactId() { return artifactId; }
    public List<ClassEntity> getClasses() { return classes; }
    public void setClasses(List<ClassEntity> classes) { this.classes = classes; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PackageEntity that)) return false;
        return Objects.equals(fullyQualifiedName, that.fullyQualifiedName)
                && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullyQualifiedName, repoName);
    }
}
