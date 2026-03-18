package com.extractor.api.dto;

import java.util.Objects;

/**
 * Lightweight DTO representing a directed edge between two class nodes.
 */
public final class GraphEdgeResponse {

    private final Long sourceId;
    private final String sourceFqn;
    private final Long targetId;
    private final String targetFqn;
    private final String relationshipType;
    private final boolean isCrossRepo;

    public GraphEdgeResponse(Long sourceId, String sourceFqn, Long targetId, String targetFqn,
                              String relationshipType, boolean isCrossRepo) {
        this.sourceId = sourceId;
        this.sourceFqn = sourceFqn;
        this.targetId = targetId;
        this.targetFqn = targetFqn;
        this.relationshipType = relationshipType;
        this.isCrossRepo = isCrossRepo;
    }

    public Long getSourceId() { return sourceId; }
    public String getSourceFqn() { return sourceFqn; }
    public Long getTargetId() { return targetId; }
    public String getTargetFqn() { return targetFqn; }
    public String getRelationshipType() { return relationshipType; }
    public boolean isCrossRepo() { return isCrossRepo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GraphEdgeResponse)) return false;
        GraphEdgeResponse that = (GraphEdgeResponse) o;
        return Objects.equals(sourceId, that.sourceId) && Objects.equals(targetId, that.targetId)
                && Objects.equals(relationshipType, that.relationshipType);
    }

    @Override
    public int hashCode() { return Objects.hash(sourceId, targetId, relationshipType); }
}
