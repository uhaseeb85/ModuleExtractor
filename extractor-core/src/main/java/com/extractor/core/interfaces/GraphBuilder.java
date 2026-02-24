package com.extractor.core.interfaces;

import com.extractor.core.exceptions.GraphException;
import com.extractor.core.model.DependsOnEdge;
import com.extractor.core.model.ParseResult;
import com.extractor.core.model.RepoConfig;

import java.util.List;

/**
 * Persists parsed Java source data into the Neo4j dependency graph.
 *
 * <p>Implementations batch-save nodes and relationships using Spring Data Neo4j
 * repositories and {@code Neo4jTemplate}. Each call is idempotent: existing nodes
 * are merged (upserted) rather than duplicated.
 */
public interface GraphBuilder {

    /**
     * Persist a single {@link ParseResult} (class + methods + fields + edges) into Neo4j.
     * This must be idempotent: re-parsing the same file should produce the same graph state.
     *
     * @param result     The parsed result for one Java source file.
     * @param repoConfig The repository configuration this result belongs to.
     * @throws GraphException If persistence fails.
     */
    void persist(ParseResult result, RepoConfig repoConfig) throws GraphException;

    /**
     * Persist a batch of {@link ParseResult} objects in a single transaction.
     * Prefer this over calling {@link #persist} in a loop for better performance.
     *
     * @param results    List of parse results (typically one batch from the ingestion orchestrator).
     * @param repoConfig The repository configuration these results belong to.
     * @throws GraphException If persistence fails.
     */
    void persistBatch(List<ParseResult> results, RepoConfig repoConfig) throws GraphException;

    /**
     * Persist artifact-level dependency edges (DEPENDS_ON relationships) into Neo4j.
     *
     * @param edges List of dependency edges resolved from Maven/Gradle build files.
     * @throws GraphException If persistence fails.
     */
    void persistDependencies(List<DependsOnEdge> edges) throws GraphException;
}
