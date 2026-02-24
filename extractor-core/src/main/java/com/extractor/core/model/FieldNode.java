package com.extractor.core.model;

import java.util.List;

/**
 * Immutable representation of a field declared on a Java class.
 *
 * @param name        Field name.
 * @param fieldType   Declared type as a string, e.g. {@code String}, {@code java.util.List<Order>}.
 * @param visibility  Visibility modifier.
 * @param annotations Simple names of annotations applied to this field.
 * @param repoName    Repository this field belongs to.
 */
public record FieldNode(
        String name,
        String fieldType,
        String visibility,
        List<String> annotations,
        String repoName
) {
    public FieldNode {
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }
}
