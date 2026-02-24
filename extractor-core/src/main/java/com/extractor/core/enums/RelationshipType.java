package com.extractor.core.enums;

/**
 * All Neo4j relationship types used in the dependency graph.
 */
public enum RelationshipType {
    CONTAINS,
    DEPENDS_ON,
    IMPORTS,
    CALLS,
    EXTENDS,
    IMPLEMENTS,
    ANNOTATED_WITH,
    INJECTS,
    READS,
    WRITES,
    CALLS_REMOTE,
    CO_CHANGES_WITH,
    OWNED_BY
}
