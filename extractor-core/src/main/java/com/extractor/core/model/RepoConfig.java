package com.extractor.core.model;

import com.extractor.core.enums.BuildTool;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Configuration for a single repository to be scanned and ingested.
 *
 * @param name      Unique logical name for the repository (used as identifier throughout the graph).
 * @param url       Remote URL (HTTPS or SSH) of the Git repository.
 * @param branch    Branch to clone/pull. Defaults to "main" if not specified.
 * @param buildTool Build tool used by this repository.
 * @param localPath Absolute path on the local filesystem where the repo should be cloned.
 */
public record RepoConfig(
        @NotBlank String name,
        @NotBlank String url,
        @NotBlank String branch,
        @NotNull BuildTool buildTool,
        @NotBlank String localPath
) {
    public RepoConfig {
        if (branch == null || branch.isBlank()) {
            branch = "main";
        }
    }
}
