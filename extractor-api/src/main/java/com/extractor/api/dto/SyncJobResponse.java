package com.extractor.api.dto;

import com.extractor.core.enums.SyncStatus;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for sync job status queries.
 */
public record SyncJobResponse(
        String jobId,
        SyncStatus status,
        int progressPercent,
        String currentRepo,
        List<String> errors,
        Instant startedAt,
        Instant completedAt
) {}
