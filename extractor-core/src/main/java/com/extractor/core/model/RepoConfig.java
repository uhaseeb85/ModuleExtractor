package com.extractor.core.model;

import com.extractor.core.enums.BuildTool;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * Configuration for a single repository to be scanned and ingested.
 */
public final class RepoConfig {

    @NotBlank
    private final String name;
    @NotBlank
    private final String url;
    @NotBlank
    private final String branch;
    @NotNull
    private final BuildTool buildTool;
    @NotBlank
    private final String localPath;

    public RepoConfig(String name, String url, String branch, BuildTool buildTool, String localPath) {
        this.name = name;
        this.url = url;
        this.branch = (branch == null || branch.trim().isEmpty()) ? "main" : branch;
        this.buildTool = buildTool;
        this.localPath = localPath;
    }

    public String getName() { return name; }
    public String getUrl() { return url; }
    public String getBranch() { return branch; }
    public BuildTool getBuildTool() { return buildTool; }
    public String getLocalPath() { return localPath; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RepoConfig)) return false;
        RepoConfig that = (RepoConfig) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() { return Objects.hash(name); }

    @Override
    public String toString() {
        return "RepoConfig{name='" + name + "', url='" + url + "', branch='" + branch + "'}";
    }
}
