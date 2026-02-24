package com.extractor.core.model;

/**
 * Represents an ANNOTATED_WITH relationship.
 *
 * @param targetFqn      FQN of the class/method/field bearing the annotation.
 * @param annotationFqn  FQN of the annotation class.
 * @param attributes     JSON string of annotation attribute key/value pairs.
 * @param elementType    The element type being annotated: CLASS, METHOD, or FIELD.
 */
public record AnnotationEdge(
        String targetFqn,
        String annotationFqn,
        String attributes,
        String elementType
) {}
