package com.extractor.api.dto;

import java.util.List;

/**
 * Detailed view of one package candidate with class list and blockers.
 * Returned by {@code GET /api/v1/analysis/candidates/{packageFqn}}.
 */
public record CandidateDetailDto(
        String packageFqn,
        String repoName,
        int classCount,
        int inboundDeps,
        int outboundDeps,
        double isolationScore,
        double stabilityScore,
        double sizeScore,
        double compositeScore,
        String recommendation,
        List<String> blockers,
        List<String> classNames
) {}
