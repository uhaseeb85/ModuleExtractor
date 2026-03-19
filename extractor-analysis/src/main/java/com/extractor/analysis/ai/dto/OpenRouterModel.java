package com.extractor.analysis.ai.dto;

/**
 * Represents a model entry from the OpenRouter /models endpoint.
 */
public final class OpenRouterModel {

    private String id;
    private String name;
    private String description;
    private Pricing pricing;
    private int context_length;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Pricing getPricing() { return pricing; }
    public void setPricing(Pricing pricing) { this.pricing = pricing; }
    public int getContext_length() { return context_length; }
    public void setContext_length(int context_length) { this.context_length = context_length; }

    public static final class Pricing {
        private String prompt;
        private String completion;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getCompletion() { return completion; }
        public void setCompletion(String completion) { this.completion = completion; }
    }
}
