package com.extractor.graph.entity;

import com.extractor.core.enums.BuildTool;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Neo4j node representing a Git repository being analysed.
 */
@Node("Repository")
public class RepositoryEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Property(name = "name")
    private String name;

    @Property(name = "url")
    private String url;

    @Property(name = "branch")
    private String branch;

    @Property(name = "localPath")
    private String localPath;

    @Property(name = "buildTool")
    private String buildTool;

    @Property(name = "lastSyncSha")
    private String lastSyncSha;

    @Property(name = "syncedAt")
    private String syncedAt;

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<ArtifactEntity> artifacts = new ArrayList<>();

    protected RepositoryEntity() {}

    public RepositoryEntity(String name, String url, String branch, String localPath, BuildTool buildTool) {
        this.name = name;
        this.url = url;
        this.branch = branch;
        this.localPath = localPath;
        this.buildTool = buildTool.name();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getBranch() { return branch; }
    public String getLocalPath() { return localPath; }
    public String getBuildTool() { return buildTool; }
    public String getLastSyncSha() { return lastSyncSha; }
    public void setLastSyncSha(String lastSyncSha) { this.lastSyncSha = lastSyncSha; }
    public String getSyncedAt() { return syncedAt; }
    public void setSyncedAt(String syncedAt) { this.syncedAt = syncedAt; }
    public List<ArtifactEntity> getArtifacts() { return artifacts; }
    public void setArtifacts(List<ArtifactEntity> artifacts) { this.artifacts = artifacts; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepositoryEntity that)) return false;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
