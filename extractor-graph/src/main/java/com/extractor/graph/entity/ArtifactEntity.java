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
 * Neo4j node representing a Maven/Gradle artifact (groupId:artifactId:version).
 */
@Node("Artifact")
public class ArtifactEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Property(name = "groupId")
    private String groupId;

    @Property(name = "artifactId")
    private String artifactId;

    @Property(name = "version")
    private String version;

    /** JAR, POM, or WAR */
    @Property(name = "type")
    private String type;

    @Property(name = "repoName")
    private String repoName;

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<PackageEntity> packages = new ArrayList<>();

    @Relationship(type = "DEPENDS_ON", direction = Relationship.Direction.OUTGOING)
    private List<ArtifactDependency> dependencies = new ArrayList<>();

    protected ArtifactEntity() {}

    public ArtifactEntity(String groupId, String artifactId, String version, String type, String repoName) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = type;
        this.repoName = repoName;
    }

    public Long getId() { return id; }
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
        if (!(o instanceof ArtifactEntity that)) return false;
        return Objects.equals(groupId, that.groupId)
                && Objects.equals(artifactId, that.artifactId)
                && Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }
}
