package com.extractor.api.dto;

/**
 * Lightweight DTO representing a class node in the dependency graph.
 * Used for graph visualisation API responses.
 */
public record GraphNodeResponse(
        Long id,
        String fqn,
        String simpleName,
        String classType,
        String repoName,
        String packageName,
        boolean isAbstract,
        int methodCount
) {}
