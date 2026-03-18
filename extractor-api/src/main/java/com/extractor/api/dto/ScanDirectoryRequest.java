package com.extractor.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/ingestion/scan-directory}.
 *
 * <p>Points the extractor at a local filesystem directory that may contain
 * one or more Git repositories as immediate sub-directories (or is itself a
 * Git repository). Every discovered repo is registered for module extraction
 * using the same pipeline as manually-added GitHub repositories.
 */
public class ScanDirectoryRequest {

    /**
     * Absolute path to the directory to scan.
     * The directory itself may be a git repo, or it may contain git repos
     * as direct sub-directories.
     */
    @NotBlank(message = "directoryPath is required")
    private String directoryPath;

    /**
     * Default build tool to assume when auto-detection fails.
     * Accepts {@code MAVEN} or {@code GRADLE}. Defaults to {@code MAVEN}.
     */
    private String buildTool = "MAVEN";

    /**
     * Default branch name to record for each discovered repo.
     * Defaults to {@code main}.
     */
    private String branch = "main";

    // ── Getters & setters ──────────────────────────────────────────────

    public String getDirectoryPath() { return directoryPath; }
    public void setDirectoryPath(String directoryPath) { this.directoryPath = directoryPath; }

    public String getBuildTool() {
        return (buildTool == null || buildTool.isBlank()) ? "MAVEN" : buildTool;
    }
    public void setBuildTool(String buildTool) {
        this.buildTool = (buildTool == null || buildTool.isBlank()) ? "MAVEN" : buildTool.toUpperCase();
    }

    public String getBranch() {
        return (branch == null || branch.isBlank()) ? "main" : branch;
    }
    public void setBranch(String branch) { this.branch = branch; }
}
