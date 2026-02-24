package com.extractor.core.exceptions;

import com.extractor.core.enums.BuildTool;

import java.nio.file.Path;

/**
 * Thrown when Maven or Gradle dependency resolution fails.
 */
public class BuildParseException extends RuntimeException {

    private final BuildTool buildTool;
    private final Path projectDir;

    public BuildParseException(String message, BuildTool buildTool, Path projectDir) {
        super(message);
        this.buildTool = buildTool;
        this.projectDir = projectDir;
    }

    public BuildParseException(String message, BuildTool buildTool, Path projectDir, Throwable cause) {
        super(message, cause);
        this.buildTool = buildTool;
        this.projectDir = projectDir;
    }

    public BuildTool getBuildTool() {
        return buildTool;
    }

    public Path getProjectDir() {
        return projectDir;
    }
}
