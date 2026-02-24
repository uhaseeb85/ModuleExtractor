package com.extractor.core.enums;

/**
 * All Neo4j node label types in the dependency graph.
 */
public enum NodeType {
    REPOSITORY,
    ARTIFACT,
    PACKAGE,
    CLASS,
    METHOD,
    FIELD,
    TABLE,
    SPRING_BEAN,
    FEIGN_CLIENT
}
