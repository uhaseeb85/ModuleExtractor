package com.extractor.core.interfaces;

import com.extractor.core.exceptions.IngestionException;
import com.extractor.core.model.RepoConfig;

import java.nio.file.Path;
import java.util.List;

/**
 * Responsible for cloning or updating a Git repository and detecting changed files.
 * Implementations must be read-only with respect to repository contents; they must
 * never modify source files.
 */
public interface RepoScanner {

    /**
     * Clone the repository if it does not exist locally, or pull the latest changes
     * from the configured branch otherwise.
     *
     * @param config Repository configuration.
     * @return The HEAD commit SHA after the sync operation.
     * @throws IngestionException If cloning/pulling fails or SSH authentication fails.
     */
    String syncRepo(RepoConfig config) throws IngestionException;

    /**
     * Returns the paths of all {@code .java} files that changed since the given commit SHA.
     * If {@code lastSha} is {@code null}, returns all {@code .java} files in the repository.
     *
     * @param config  Repository configuration (must already be synced/cloned locally).
     * @param lastSha The previous HEAD SHA to diff against. Pass {@code null} to return all files.
     * @return Immutable list of absolute {@link Path} objects for changed Java source files.
     * @throws IngestionException If git history traversal fails.
     */
    List<Path> getChangedFiles(RepoConfig config, String lastSha) throws IngestionException;
}
