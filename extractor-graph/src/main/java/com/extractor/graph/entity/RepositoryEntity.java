package com.extractor.graph.entity;

import com.extractor.core.enums.BuildTool;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Plain-POJO representing a Git repository being analysed.
 * Stored in-memory via {@link com.extractor.graph.store.GraphStore}.
 */
public class RepositoryEntity {

    private String name;
    private String url;
    private String branch;
    private String localPath;
    private String buildTool;
    private String lastSyncSha;
    private String syncedAt;

    private List<ArtifactEntity> artifacts = new ArrayList<>();

    protected RepositoryEntity() {}

    public RepositoryEntity(String name, String url, String branch, String localPath, BuildTool buildTool) {
        this.name = name;
        this.url = url;
        this.branch = branch;
        this.localPath = localPath;
        this.buildTool = buildTool.name();
    }

    public Long getId() { return name != null ? (long) name.hashCode() : null; }
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
        if (!(o instanceof RepositoryEntity)) return false;
        RepositoryEntity that = (RepositoryEntity) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
