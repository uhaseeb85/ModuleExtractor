package com.extractor.api.dto;

/**
 * DTO for a single model in the model list response.
 */
public final class AiModelDto {

    private final String id;
    private final String name;
    private final String description;
    private final int contextLength;
    private final String promptPrice;
    private final String completionPrice;

    public AiModelDto(String id, String name, String description, int contextLength,
                      String promptPrice, String completionPrice) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.contextLength = contextLength;
        this.promptPrice = promptPrice;
        this.completionPrice = completionPrice;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getContextLength() { return contextLength; }
    public String getPromptPrice() { return promptPrice; }
    public String getCompletionPrice() { return completionPrice; }
}
