package com.extractor.analysis.ai;

import com.extractor.analysis.ai.dto.*;
import com.extractor.analysis.service.CandidateScoringService;
import com.extractor.analysis.service.CandidateScoringService.ModuleRecommendation;
import com.extractor.analysis.service.CandidateScoringService.PackageScore;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Orchestrates the four AI-assisted analysis capabilities by combining graph data
 * from {@link CandidateScoringService} with LLM calls via {@link OpenRouterClient}.
 */
@Service
public class AiAnalysisService {

    private final CandidateScoringService scoringService;
    private final OpenRouterClient openRouter;

    public AiAnalysisService(CandidateScoringService scoringService,
                              OpenRouterClient openRouter) {
        this.scoringService = scoringService;
        this.openRouter = openRouter;
    }

    /**
     * Lists available models from OpenRouter (proxied to avoid CORS).
     */
    public List<OpenRouterModel> listModels(String apiKey) {
        return openRouter.listModels(apiKey);
    }

    /**
     * AI capability 1: Refine extraction boundaries for a specific module.
     */
    public AiResult refineBoundaries(String apiKey, String model, String moduleName,
                                      int groupDepth, double minScore) {
        ModuleRecommendation module = findModule(moduleName, groupDepth, minScore);
        if (module == null) {
            return AiResult.error("Module not found: " + moduleName);
        }

        List<PackageScore> allScores = scoringService.rankCandidates(1, Integer.MAX_VALUE);
        String prompt = PromptTemplates.refineBoundaries(module, allScores);
        return callLlm(apiKey, model, prompt);
    }

    /**
     * AI capability 2: Generate a migration plan for a specific module.
     */
    public AiResult migrationPlan(String apiKey, String model, String moduleName,
                                   int groupDepth, double minScore) {
        ModuleRecommendation module = findModule(moduleName, groupDepth, minScore);
        if (module == null) {
            return AiResult.error("Module not found: " + moduleName);
        }

        String prompt = PromptTemplates.migrationPlan(module);
        return callLlm(apiKey, model, prompt);
    }

    /**
     * AI capability 3: Identify DDD bounded contexts across all modules.
     */
    public AiResult boundedContexts(String apiKey, String model,
                                     int groupDepth, double minScore) {
        List<ModuleRecommendation> modules = scoringService.recommendModules(groupDepth, minScore);
        if (modules.isEmpty()) {
            return AiResult.error("No module recommendations found with current parameters.");
        }

        String prompt = PromptTemplates.boundedContexts(modules);
        return callLlm(apiKey, model, prompt);
    }

    /**
     * AI capability 4: Suggest optimised scoring weights.
     */
    public AiResult optimiseWeights(String apiKey, String model) {
        List<PackageScore> scores = scoringService.rankCandidates(2, 100);
        if (scores.isEmpty()) {
            return AiResult.error("No scored packages found. Sync a repository first.");
        }

        String prompt = PromptTemplates.optimiseWeights(scores);
        return callLlm(apiKey, model, prompt);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private ModuleRecommendation findModule(String moduleName, int groupDepth, double minScore) {
        List<ModuleRecommendation> modules = scoringService.recommendModules(groupDepth, minScore);
        for (ModuleRecommendation m : modules) {
            if (m.getModuleName().equals(moduleName)) {
                return m;
            }
        }
        return null;
    }

    private AiResult callLlm(String apiKey, String model, String userPrompt) {
        ChatCompletionRequest req = new ChatCompletionRequest(
                model,
                Arrays.asList(
                        ChatMessage.system(PromptTemplates.SYSTEM),
                        ChatMessage.user(userPrompt)
                ),
                0.3
        );

        ChatCompletionResponse resp = openRouter.chatCompletion(apiKey, req);
        if (resp == null) {
            return AiResult.error("Empty response from OpenRouter");
        }

        int promptTokens = 0;
        int completionTokens = 0;
        if (resp.getUsage() != null) {
            promptTokens = resp.getUsage().getPrompt_tokens();
            completionTokens = resp.getUsage().getCompletion_tokens();
        }

        return new AiResult(resp.firstContent(), resp.getModel(), promptTokens, completionTokens, null);
    }

    /**
     * Immutable result container for AI analysis calls.
     */
    public static final class AiResult {
        private final String content;
        private final String modelUsed;
        private final int promptTokens;
        private final int completionTokens;
        private final String error;

        public AiResult(String content, String modelUsed, int promptTokens, int completionTokens, String error) {
            this.content = content;
            this.modelUsed = modelUsed;
            this.promptTokens = promptTokens;
            this.completionTokens = completionTokens;
            this.error = error;
        }

        public static AiResult error(String message) {
            return new AiResult(null, null, 0, 0, message);
        }

        public boolean hasError() { return error != null; }
        public String getContent() { return content; }
        public String getModelUsed() { return modelUsed; }
        public int getPromptTokens() { return promptTokens; }
        public int getCompletionTokens() { return completionTokens; }
        public String getError() { return error; }
    }
}
