package com.extractor.analysis.ai.dto;

import java.util.List;

/**
 * Response payload from OpenRouter /chat/completions endpoint.
 */
public final class ChatCompletionResponse {

    private String id;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public List<Choice> getChoices() { return choices; }
    public void setChoices(List<Choice> choices) { this.choices = choices; }
    public Usage getUsage() { return usage; }
    public void setUsage(Usage usage) { this.usage = usage; }

    /** Returns the text of the first choice, or empty string. */
    public String firstContent() {
        if (choices == null || choices.isEmpty()) return "";
        ChatMessage msg = choices.get(0).getMessage();
        return msg != null && msg.getContent() != null ? msg.getContent() : "";
    }

    public static final class Choice {
        private int index;
        private ChatMessage message;

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public ChatMessage getMessage() { return message; }
        public void setMessage(ChatMessage message) { this.message = message; }
    }

    public static final class Usage {
        private int prompt_tokens;
        private int completion_tokens;
        private int total_tokens;

        public int getPrompt_tokens() { return prompt_tokens; }
        public void setPrompt_tokens(int prompt_tokens) { this.prompt_tokens = prompt_tokens; }
        public int getCompletion_tokens() { return completion_tokens; }
        public void setCompletion_tokens(int completion_tokens) { this.completion_tokens = completion_tokens; }
        public int getTotal_tokens() { return total_tokens; }
        public void setTotal_tokens(int total_tokens) { this.total_tokens = total_tokens; }
    }
}
