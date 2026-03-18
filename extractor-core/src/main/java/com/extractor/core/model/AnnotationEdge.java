package com.extractor.core.model;

import java.util.Objects;

/**
 * Represents an ANNOTATED_WITH relationship.
 */
public final class AnnotationEdge {

    private final String targetFqn;
    private final String annotationFqn;
    private final String attributes;
    private final String elementType;

    public AnnotationEdge(String targetFqn, String annotationFqn, String attributes, String elementType) {
        this.targetFqn = targetFqn;
        this.annotationFqn = annotationFqn;
        this.attributes = attributes;
        this.elementType = elementType;
    }

    public String getTargetFqn() { return targetFqn; }
    public String getAnnotationFqn() { return annotationFqn; }
    public String getAttributes() { return attributes; }
    public String getElementType() { return elementType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnnotationEdge)) return false;
        AnnotationEdge that = (AnnotationEdge) o;
        return Objects.equals(targetFqn, that.targetFqn)
                && Objects.equals(annotationFqn, that.annotationFqn)
                && Objects.equals(elementType, that.elementType);
    }

    @Override
    public int hashCode() { return Objects.hash(targetFqn, annotationFqn, elementType); }

    @Override
    public String toString() {
        return "AnnotationEdge{target='" + targetFqn + "', annotation='" + annotationFqn + "'}";
    }
}
