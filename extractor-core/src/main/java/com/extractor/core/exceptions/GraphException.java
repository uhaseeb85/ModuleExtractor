package com.extractor.core.exceptions;

/**
 * Thrown when a Neo4j graph persistence or query operation fails.
 */
public class GraphException extends RuntimeException {

    public GraphException(String message) {
        super(message);
    }

    public GraphException(String message, Throwable cause) {
        super(message, cause);
    }
}
