package com.extractor.graph.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

/**
 * Runs Neo4j schema constraints and indexes on application startup.
 * Constraints are defined with {@code IF NOT EXISTS} so they are safely idempotent.
 */
@Component
public class Neo4jSchemaInitializer {

    private static final Logger log = LoggerFactory.getLogger(Neo4jSchemaInitializer.class);

    private final Neo4jClient neo4jClient;

    public Neo4jSchemaInitializer(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initSchema() {
        log.info("Initializing Neo4j schema constraints and indexes...");

        // Uniqueness constraints
        runCypher("CREATE CONSTRAINT repo_name IF NOT EXISTS FOR (r:Repository) REQUIRE r.name IS UNIQUE");
        runCypher("CREATE CONSTRAINT artifact_coords IF NOT EXISTS FOR (a:Artifact) REQUIRE (a.groupId, a.artifactId, a.version) IS NODE KEY");
        runCypher("CREATE CONSTRAINT class_fqn IF NOT EXISTS FOR (c:Class) REQUIRE c.fullyQualifiedName IS UNIQUE");
        runCypher("CREATE CONSTRAINT table_name IF NOT EXISTS FOR (t:Table) REQUIRE t.name IS UNIQUE");

        // Indexes for fast lookups
        runCypher("CREATE INDEX class_repo IF NOT EXISTS FOR (c:Class) ON (c.repoName)");
        runCypher("CREATE INDEX method_repo IF NOT EXISTS FOR (m:Method) ON (m.repoName)");
        runCypher("CREATE INDEX package_fqn IF NOT EXISTS FOR (p:Package) ON (p.fullyQualifiedName)");
        runCypher("CREATE INDEX artifact_repo IF NOT EXISTS FOR (a:Artifact) ON (a.repoName)");

        log.info("Neo4j schema initialization complete.");
    }

    private void runCypher(String cypher) {
        try {
            neo4jClient.query(cypher).run();
            log.debug("Executed: {}", cypher);
        } catch (Exception e) {
            log.warn("Schema statement failed (may already exist): {} — {}", cypher, e.getMessage());
        }
    }
}
