package com.extractor.core.exceptions;

/**
 * Thrown when a repository sync (clone or pull) operation fails.
 */
public class IngestionException extends RuntimeException {

    public IngestionException(String message) {
        super(message);
    }

    public IngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
