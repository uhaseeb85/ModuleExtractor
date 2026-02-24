package com.extractor.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end Spring Boot integration tests for the REST API.
 * Uses Testcontainers for Neo4j and MockMvc for HTTP assertions.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class ApiIntegrationTest {

    @Container
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5-community")
            .withoutAuthentication();

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "");
        // No repos configured — keeps test lightweight
        registry.add("extractor.repos", () -> "");
    }

    @Autowired
    MockMvc mockMvc;

    // ── /api/v1/graph/nodes ─────────────────────────────────────────

    @Test
    void graphNodesReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/graph/nodes").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void graphNodesFilteredByRepo() throws Exception {
        mockMvc.perform(get("/api/v1/graph/nodes")
                        .param("repo", "no-such-repo")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── /api/v1/graph/edges ─────────────────────────────────────────

    @Test
    void graphEdgesReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/graph/edges").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── /api/v1/graph/node/{fqn} ────────────────────────────────────

    @Test
    void graphNodeNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/graph/node/com.example.DoesNotExist")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ── /api/v1/ingestion/repos ─────────────────────────────────────

    @Test
    void ingestionReposReturnsOk() throws Exception {
        mockMvc.perform(get("/api/v1/ingestion/repos").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ── /api/v1/ingestion/sync ──────────────────────────────────────

    @Test
    void ingestionSyncTriggerReturns202() throws Exception {
        mockMvc.perform(post("/api/v1/ingestion/sync").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    // ── /api/v1/ingestion/jobs/{jobId} ──────────────────────────────

    @Test
    void jobStatusNotFoundReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/ingestion/jobs/00000000-0000-0000-0000-000000000000")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // ── Actuator health ─────────────────────────────────────────────

    @Test
    void actuatorHealthReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
