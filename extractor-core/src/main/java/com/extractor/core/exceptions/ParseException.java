package com.extractor.core.exceptions;

import java.nio.file.Path;

/**
 * Thrown when a Java source file cannot be parsed by JavaParser.
 */
public class ParseException extends RuntimeException {

    private final Path sourceFile;

    public ParseException(String message, Path sourceFile) {
        super(message);
        this.sourceFile = sourceFile;
    }

    public ParseException(String message, Path sourceFile, Throwable cause) {
        super(message, cause);
        this.sourceFile = sourceFile;
    }

    /** Returns the path of the file that failed to parse. */
    public Path getSourceFile() {
        return sourceFile;
    }
}
