package com.extractor.api.controller;

import com.extractor.analysis.ai.AiAnalysisService;
import com.extractor.analysis.ai.AiAnalysisService.AiResult;
import com.extractor.analysis.ai.AiAnalysisService.PipelineResult;
import com.extractor.analysis.ai.dto.OpenRouterModel;
import com.extractor.api.dto.AiAnalysisRequest;
import com.extractor.api.dto.AiAnalysisResponse;
import com.extractor.api.dto.AiHealthResponse;
import com.extractor.api.dto.AiPipelineResponse;
import com.extractor.api.dto.AiModelDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST endpoints for AI-assisted analysis features.
 * The OpenRouter API key is passed via the {@code X-OpenRouter-Key} header.
 */
@RestController
@RequestMapping("/api/v1/ai")
public class AiAnalysisController {

    private final AiAnalysisService aiService;

    public AiAnalysisController(AiAnalysisService aiService) {
        this.aiService = aiService;
    }

    // ── Model listing ─────────────────────────────────────────────────

    @GetMapping("/models")
    public ResponseEntity<List<AiModelDto>> listModels(
            @RequestHeader("X-OpenRouter-Key") String apiKey) {

        List<OpenRouterModel> models = aiService.listModels(apiKey);
        List<AiModelDto> dtos = models.stream()
                .map(m -> new AiModelDto(
                        m.getId(),
                        m.getName(),
                        m.getDescription(),
                        m.getContext_length(),
                        m.getPricing() != null ? m.getPricing().getPrompt() : null,
                        m.getPricing() != null ? m.getPricing().getCompletion() : null
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // ── Health check ──────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<AiHealthResponse> health(
            @RequestHeader("X-OpenRouter-Key") String apiKey) {
        long start = System.currentTimeMillis();
        boolean available = aiService.checkHealth(apiKey);
        long latency = System.currentTimeMillis() - start;
        return ResponseEntity.ok(new AiHealthResponse(available, latency, available ? null : "API key invalid or service unreachable"));
    }

    // ── Full pipeline ─────────────────────────────────────────────────

    @PostMapping("/pipeline")
    public ResponseEntity<AiPipelineResponse> pipeline(
            @RequestHeader("X-OpenRouter-Key") String apiKey,
            @RequestBody AiAnalysisRequest req) {

        PipelineResult result = aiService.runPipeline(
                apiKey, req.getModel(), req.getModuleName(),
                req.getGroupDepth(), req.getMinScore());
        return ResponseEntity.ok(new AiPipelineResponse(
                toDto(result.getBoundaries()),
                toDto(result.getMigration()),
                toDto(result.getContexts())
        ));
    }

    // ── AI Capabilities ───────────────────────────────────────────────

    @PostMapping("/refine-boundaries")
    public ResponseEntity<AiAnalysisResponse> refineBoundaries(
            @RequestHeader("X-OpenRouter-Key") String apiKey,
            @RequestBody AiAnalysisRequest req) {

        AiResult result = aiService.refineBoundaries(
                apiKey, req.getModel(), req.getModuleName(),
                req.getGroupDepth(), req.getMinScore());
        return toResponse(result);
    }

    @PostMapping("/migration-plan")
    public ResponseEntity<AiAnalysisResponse> migrationPlan(
            @RequestHeader("X-OpenRouter-Key") String apiKey,
            @RequestBody AiAnalysisRequest req) {

        AiResult result = aiService.migrationPlan(
                apiKey, req.getModel(), req.getModuleName(),
                req.getGroupDepth(), req.getMinScore());
        return toResponse(result);
    }

    @PostMapping("/bounded-contexts")
    public ResponseEntity<AiAnalysisResponse> boundedContexts(
            @RequestHeader("X-OpenRouter-Key") String apiKey,
            @RequestBody AiAnalysisRequest req) {

        AiResult result = aiService.boundedContexts(
                apiKey, req.getModel(),
                req.getGroupDepth(), req.getMinScore());
        return toResponse(result);
    }

    @PostMapping("/optimise-weights")
    public ResponseEntity<AiAnalysisResponse> optimiseWeights(
            @RequestHeader("X-OpenRouter-Key") String apiKey,
            @RequestBody AiAnalysisRequest req) {

        AiResult result = aiService.optimiseWeights(apiKey, req.getModel());
        return toResponse(result);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private ResponseEntity<AiAnalysisResponse> toResponse(AiResult result) {
        AiAnalysisResponse resp = toDto(result);
        if (result.hasError()) {
            return ResponseEntity.badRequest().body(resp);
        }
        return ResponseEntity.ok(resp);
    }

    private AiAnalysisResponse toDto(AiResult result) {
        return new AiAnalysisResponse(
                result.getContent(),
                result.getModelUsed(),
                result.getPromptTokens(),
                result.getCompletionTokens(),
                result.getError()
        );
    }
}
