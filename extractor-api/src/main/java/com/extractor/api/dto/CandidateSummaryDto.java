package com.extractor.api.dto;

/**
 * Lightweight summary of one package's extraction-candidate scores.
 * Returned by {@code GET /api/v1/analysis/candidates}.
 */
public record CandidateSummaryDto(
        String packageFqn,
        String repoName,
        int classCount,
        int inboundDeps,
        int outboundDeps,
        double isolationScore,
        double stabilityScore,
        double sizeScore,
        double compositeScore,
        String recommendation
) {}
