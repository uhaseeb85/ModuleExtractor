package com.extractor.analysis.ai;

import com.extractor.analysis.ai.dto.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * HTTP client for the OpenRouter API.
 * API key is passed per-request (stored in browser, forwarded via header).
 */
@Component
public class OpenRouterClient {

    private static final String BASE_URL = "https://openrouter.ai/api/v1";
    private final RestTemplate rest;

    public OpenRouterClient() {
        this.rest = new RestTemplate();
    }

    /**
     * Fetches available models from OpenRouter.
     */
    public List<OpenRouterModel> listModels(String apiKey) {
        HttpHeaders headers = buildHeaders(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<ModelsResponse> resp = rest.exchange(
                BASE_URL + "/models",
                HttpMethod.GET,
                entity,
                ModelsResponse.class
        );

        ModelsResponse body = resp.getBody();
        return body != null && body.getData() != null ? body.getData() : Collections.<OpenRouterModel>emptyList();
    }

    /**
     * Sends a chat completion request to OpenRouter.
     */
    public ChatCompletionResponse chatCompletion(String apiKey, ChatCompletionRequest request) {
        HttpHeaders headers = buildHeaders(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(request, headers);

        ResponseEntity<ChatCompletionResponse> resp = rest.exchange(
                BASE_URL + "/chat/completions",
                HttpMethod.POST,
                entity,
                ChatCompletionResponse.class
        );

        return resp.getBody();
    }

    private HttpHeaders buildHeaders(String apiKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.set("HTTP-Referer", "https://module-extractor.local");
        headers.set("X-Title", "ModuleExtractor");
        return headers;
    }
}
