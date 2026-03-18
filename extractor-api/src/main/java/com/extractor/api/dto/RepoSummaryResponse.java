package com.extractor.api.dto;

import java.util.Objects;

/**
 * Summary of a repository's sync state as returned by GET /api/v1/ingestion/repos.
 */
public final class RepoSummaryResponse {

    private final String name;
    private final String url;
    private final String branch;
    private final String buildTool;
    private final String lastSyncSha;
    private final String syncedAt;
    private final long nodeCount;

    public RepoSummaryResponse(String name, String url, String branch, String buildTool,
                                String lastSyncSha, String syncedAt, long nodeCount) {
        this.name = name;
        this.url = url;
        this.branch = branch;
        this.buildTool = buildTool;
        this.lastSyncSha = lastSyncSha;
        this.syncedAt = syncedAt;
        this.nodeCount = nodeCount;
    }

    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getBranch() { return branch; }
    public String getBuildTool() { return buildTool; }
    public String getLastSyncSha() { return lastSyncSha; }
    public String getSyncedAt() { return syncedAt; }
    public long getNodeCount() { return nodeCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepoSummaryResponse)) return false;
        RepoSummaryResponse that = (RepoSummaryResponse) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }
}
