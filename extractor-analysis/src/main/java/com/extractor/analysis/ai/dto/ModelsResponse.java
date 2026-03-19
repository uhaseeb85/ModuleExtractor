package com.extractor.analysis.ai.dto;

import java.util.List;

/**
 * Wrapper for the OpenRouter GET /models response.
 */
public final class ModelsResponse {

    private List<OpenRouterModel> data;

    public List<OpenRouterModel> getData() { return data; }
    public void setData(List<OpenRouterModel> data) { this.data = data; }
}
