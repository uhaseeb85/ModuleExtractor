package com.extractor.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/ingestion/repos}.
 */
public class AddRepoRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "url is required")
    private String url;

    private String branch = "main";

    /** {@code MAVEN} or {@code GRADLE}. Defaults to MAVEN. */
    private String buildTool = "MAVEN";

    /**
     * Absolute path inside the container where the repo will be cloned.
     * Defaults to {@code /repos/<name>} if omitted.
     */
    private String localPath;

    // ── Getters & setters ──────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getBranch() { return branch == null || branch.isBlank() ? "main" : branch; }
    public void setBranch(String branch) { this.branch = branch; }

    public String getBuildTool() { return buildTool == null || buildTool.isBlank() ? "MAVEN" : buildTool.toUpperCase(); }
    public void setBuildTool(String buildTool) { this.buildTool = buildTool; }

    public String getLocalPath() {
        return (localPath == null || localPath.isBlank()) ? "/repos/" + name : localPath;
    }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
}
