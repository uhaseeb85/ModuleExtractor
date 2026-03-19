package com.extractor.analysis.ai;

import com.extractor.analysis.service.CandidateScoringService.ModuleRecommendation;
import com.extractor.analysis.service.CandidateScoringService.PackageScore;

import java.util.List;

/**
 * Builds structured prompts for each AI analysis capability.
 */
public final class PromptTemplates {

    private PromptTemplates() {}

    // ── System ────────────────────────────────────────────────────────

    public static final String SYSTEM = "You are an expert Java/Spring architect specialising in "
            + "monolith-to-microservices decomposition. Respond with structured, actionable JSON. "
            + "Be concise — focus on concrete, prioritised suggestions backed by the dependency data provided.";

    // ── Refine Boundaries ─────────────────────────────────────────────

    public static String refineBoundaries(ModuleRecommendation module, List<PackageScore> allScores) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyse this extraction candidate and suggest refined module boundaries.\n\n");
        sb.append("MODULE: ").append(module.getModuleName()).append('\n');
        sb.append("Root package: ").append(module.getModulePackageRoot()).append('\n');
        sb.append("Packages: ").append(module.getPackages()).append('\n');
        sb.append("Total classes: ").append(module.getTotalClasses()).append('\n');
        sb.append("Inbound deps: ").append(module.getTotalInboundDeps()).append('\n');
        sb.append("Outbound deps: ").append(module.getTotalOutboundDeps()).append('\n');
        sb.append("Avg composite score: ").append(module.getAvgCompositeScore()).append('\n');
        sb.append("Current recommendation: ").append(module.getRecommendation()).append('\n');
        sb.append("Blockers: ").append(module.getBlockers()).append('\n');
        sb.append("\nClasses:\n");
        for (String cls : module.getClasses()) {
            sb.append("  - ").append(cls).append('\n');
        }
        sb.append("\nRespond ONLY with JSON:\n");
        sb.append("{\n");
        sb.append("  \"refinedBoundaries\": [\n");
        sb.append("    { \"moduleName\": \"...\", \"packages\": [...], \"reason\": \"...\" }\n");
        sb.append("  ],\n");
        sb.append("  \"classesToMove\": [\n");
        sb.append("    { \"className\": \"...\", \"from\": \"...\", \"to\": \"...\", \"reason\": \"...\" }\n");
        sb.append("  ],\n");
        sb.append("  \"sharedKernelCandidates\": [...],\n");
        sb.append("  \"summary\": \"...\"\n");
        sb.append("}");
        return sb.toString();
    }

    // ── Migration Plan ────────────────────────────────────────────────

    public static String migrationPlan(ModuleRecommendation module) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate a step-by-step migration plan for extracting this module from the monolith.\n\n");
        sb.append("MODULE: ").append(module.getModuleName()).append('\n');
        sb.append("Root package: ").append(module.getModulePackageRoot()).append('\n');
        sb.append("Packages: ").append(module.getPackages()).append('\n');
        sb.append("Classes: ").append(module.getClasses().size()).append('\n');
        sb.append("Inbound deps: ").append(module.getTotalInboundDeps()).append('\n');
        sb.append("Outbound deps: ").append(module.getTotalOutboundDeps()).append('\n');
        sb.append("Blockers: ").append(module.getBlockers()).append('\n');
        sb.append("\nRespond ONLY with JSON:\n");
        sb.append("{\n");
        sb.append("  \"phases\": [\n");
        sb.append("    {\n");
        sb.append("      \"phase\": 1,\n");
        sb.append("      \"title\": \"...\",\n");
        sb.append("      \"steps\": [\"...\"],\n");
        sb.append("      \"risks\": [\"...\"],\n");
        sb.append("      \"estimatedEffort\": \"...\"\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"prerequisites\": [\"...\"],\n");
        sb.append("  \"testingStrategy\": \"...\",\n");
        sb.append("  \"rollbackPlan\": \"...\"\n");
        sb.append("}");
        return sb.toString();
    }

    // ── Bounded Contexts ──────────────────────────────────────────────

    public static String boundedContexts(List<ModuleRecommendation> modules) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyse these module candidates and identify DDD bounded contexts.\n\n");
        sb.append("MODULES:\n");
        for (ModuleRecommendation m : modules) {
            sb.append("  - ").append(m.getModuleName())
              .append(" (root=").append(m.getModulePackageRoot())
              .append(", classes=").append(m.getTotalClasses())
              .append(", in=").append(m.getTotalInboundDeps())
              .append(", out=").append(m.getTotalOutboundDeps())
              .append(", score=").append(m.getAvgCompositeScore())
              .append(")\n");
            sb.append("    packages: ").append(m.getPackages()).append('\n');
        }
        sb.append("\nRespond ONLY with JSON:\n");
        sb.append("{\n");
        sb.append("  \"boundedContexts\": [\n");
        sb.append("    {\n");
        sb.append("      \"name\": \"...\",\n");
        sb.append("      \"description\": \"...\",\n");
        sb.append("      \"modules\": [\"...\"],\n");
        sb.append("      \"aggregateRoots\": [\"...\"],\n");
        sb.append("      \"integrationPoints\": [\"...\"]\n");
        sb.append("    }\n");
        sb.append("  ],\n");
        sb.append("  \"contextMap\": [\n");
        sb.append("    { \"upstream\": \"...\", \"downstream\": \"...\", \"relationship\": \"...\" }\n");
        sb.append("  ],\n");
        sb.append("  \"summary\": \"...\"\n");
        sb.append("}");
        return sb.toString();
    }

    // ── Optimise Scoring Weights ──────────────────────────────────────

    public static String optimiseWeights(List<PackageScore> scores) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyse these package candidate scores and suggest optimised scoring weights.\n\n");
        sb.append("Current weights: isolation=0.45, stability=0.35, size=0.20\n\n");
        sb.append("TOP PACKAGES:\n");
        int limit = Math.min(scores.size(), 20);
        for (int i = 0; i < limit; i++) {
            PackageScore s = scores.get(i);
            sb.append("  - ").append(s.getPackageFqn())
              .append(" (classes=").append(s.getClassCount())
              .append(", iso=").append(s.getIsolationScore())
              .append(", stab=").append(s.getStabilityScore())
              .append(", size=").append(s.getSizeScore())
              .append(", comp=").append(s.getCompositeScore())
              .append(", rec=").append(s.getRecommendation())
              .append(")\n");
        }
        sb.append("\nRespond ONLY with JSON:\n");
        sb.append("{\n");
        sb.append("  \"suggestedWeights\": {\n");
        sb.append("    \"isolation\": 0.0,\n");
        sb.append("    \"stability\": 0.0,\n");
        sb.append("    \"size\": 0.0\n");
        sb.append("  },\n");
        sb.append("  \"rationale\": \"...\",\n");
        sb.append("  \"impactedPackages\": [\n");
        sb.append("    { \"packageFqn\": \"...\", \"currentScore\": 0.0, \"projectedScore\": 0.0, \"change\": \"...\" }\n");
        sb.append("  ],\n");
        sb.append("  \"insights\": [\"...\"]\n");
        sb.append("}");
        return sb.toString();
    }
}
