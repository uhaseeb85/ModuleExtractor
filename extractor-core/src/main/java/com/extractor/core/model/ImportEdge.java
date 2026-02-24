package com.extractor.core.model;

/**
 * Represents an IMPORTS relationship between two classes.
 *
 * @param importerFqn  FQN of the class that declares the import.
 * @param importedFqn  FQN of the class being imported.
 * @param isStatic     Whether this is a static import.
 * @param isWildcard   Whether this is a wildcard import (e.g. {@code import com.example.*}).
 */
public record ImportEdge(
        String importerFqn,
        String importedFqn,
        boolean isStatic,
        boolean isWildcard
) {}
