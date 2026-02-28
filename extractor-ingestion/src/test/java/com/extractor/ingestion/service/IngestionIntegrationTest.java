package com.extractor.ingestion.service;

import com.extractor.core.enums.BuildTool;
import com.extractor.core.enums.SyncStatus;
import com.extractor.core.model.RepoConfig;
import com.extractor.graph.repository.ClassEntityRepository;
import com.extractor.ingestion.model.SyncJobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link IngestionOrchestrator}.
 * Ingests a local sample-repo directory into Neo4j via Testcontainers.
 */
@SpringBootTest(classes = com.extractor.ingestion.IngestionTestApplication.class)
@Testcontainers
class IngestionIntegrationTest {

    @TempDir
    static Path tempRepoDir;

    @Container
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5-community")
            .withoutAuthentication();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws IOException {
        registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "");

        // Write a real .java file to the temp dir so the ingestion has something to parse
        Path srcDir = tempRepoDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.writeString(srcDir.resolve("Widget.java"),
                "package com.example;\npublic class Widget {\n  private String name;\n}\n");

        registry.add("extractor.repos[0].name", () -> "local-test-repo");
        registry.add("extractor.repos[0].url", () -> tempRepoDir.toUri().toString());
        registry.add("extractor.repos[0].branch", () -> "main");
        registry.add("extractor.repos[0].buildTool", () -> "MAVEN");
        registry.add("extractor.repos[0].localPath", tempRepoDir::toString);
        registry.add("extractor.git.cloneOnStartup", () -> "false");
    }

    @Autowired
    IngestionOrchestrator orchestrator;

    @Autowired
    ClassEntityRepository classRepository;

    @Test
    void singleRepoSyncParsesJavaFiles() {
        String jobId = orchestrator.triggerSingleRepoSync("local-test-repo")
                .map(SyncJobStatus::getJobId)
                .orElseThrow(() -> new AssertionError("Repo 'local-test-repo' not found"));
        assertThat(jobId).isNotBlank();

        // Wait until job completes (max 30s)
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            SyncJobStatus status = orchestrator.getJobStatus(jobId).orElseThrow();
            assertThat(status.getStatus()).isIn(SyncStatus.COMPLETED, SyncStatus.FAILED);
        });

        SyncJobStatus finalStatus = orchestrator.getJobStatus(jobId).orElseThrow();
        assertThat(finalStatus.getStatus()).isEqualTo(SyncStatus.COMPLETED);

        // The com.example.Widget class should now be in the graph
        assertThat(classRepository.findByFullyQualifiedName("com.example.Widget")).isPresent();
    }
}
