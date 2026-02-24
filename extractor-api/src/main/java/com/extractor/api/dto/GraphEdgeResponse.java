package com.extractor.api.dto;

/**
 * Lightweight DTO representing a directed edge between two class nodes.
 */
public record GraphEdgeResponse(
        Long sourceId,
        String sourceFqn,
        Long targetId,
        String targetFqn,
        String relationshipType,
        boolean isCrossRepo
) {}
