package com.extractor.api.dto;

public final class AiPipelineResponse {

    private final AiAnalysisResponse boundaries;
    private final AiAnalysisResponse migration;
    private final AiAnalysisResponse contexts;

    public AiPipelineResponse(AiAnalysisResponse boundaries,
                               AiAnalysisResponse migration,
                               AiAnalysisResponse contexts) {
        this.boundaries = boundaries;
        this.migration = migration;
        this.contexts = contexts;
    }

    public AiAnalysisResponse getBoundaries() { return boundaries; }
    public AiAnalysisResponse getMigration() { return migration; }
    public AiAnalysisResponse getContexts() { return contexts; }
}
