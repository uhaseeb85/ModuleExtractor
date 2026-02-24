package com.extractor.api.dto;

import java.util.List;

/**
 * Summary of a repository's sync state as returned by GET /api/v1/ingestion/repos.
 */
public record RepoSummaryResponse(
        String name,
        String url,
        String branch,
        String buildTool,
        String lastSyncSha,
        String syncedAt,
        long nodeCount
) {}
