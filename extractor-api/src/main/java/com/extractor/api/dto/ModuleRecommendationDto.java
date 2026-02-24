package com.extractor.api.dto;

import java.util.List;

/**
 * A proposed extracted module: one or more cohesive packages grouped under a common root,
 * with their full class list and an overall extraction recommendation.
 */
public record ModuleRecommendationDto(
        /** Short human-readable module name derived from the package root, e.g. "auth" */
        String moduleName,
        /** Common package prefix of all packages in this module, e.g. "com.bank.ivr.auth" */
        String modulePackageRoot,
        String repoName,
        List<String> packages,
        List<String> classes,
        int totalClasses,
        int totalInboundDeps,
        int totalOutboundDeps,
        double avgCompositeScore,
        double minIsolationScore,
        /** "Extract now" / "Extract with refactoring" / "Low priority" */
        String recommendation,
        List<String> blockers
) {}
