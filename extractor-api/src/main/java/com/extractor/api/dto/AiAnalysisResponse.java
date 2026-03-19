package com.extractor.api.dto;

/**
 * Response wrapper for AI analysis results.
 */
public final class AiAnalysisResponse {

    private final String content;
    private final String modelUsed;
    private final int promptTokens;
    private final int completionTokens;
    private final String error;

    public AiAnalysisResponse(String content, String modelUsed, int promptTokens, int completionTokens, String error) {
        this.content = content;
        this.modelUsed = modelUsed;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.error = error;
    }

    public String getContent() { return content; }
    public String getModelUsed() { return modelUsed; }
    public int getPromptTokens() { return promptTokens; }
    public int getCompletionTokens() { return completionTokens; }
    public String getError() { return error; }
}
