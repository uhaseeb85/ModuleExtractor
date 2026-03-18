package com.extractor.ingestion.service;

import com.extractor.graph.repository.ClassEntityRepository;
import com.extractor.ingestion.model.SyncJobStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for {@link IngestionOrchestrator}.
 * Ingests a local sample-repo directory into the in-memory JGraphT store.
 */
@SpringBootTest(classes = com.extractor.ingestion.IngestionTestApplication.class)
class IngestionIntegrationTest {

    @TempDir
    static Path tempRepoDir;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) throws IOException {
        // Write a real .java file to the temp dir so the ingestion has something to parse
        Path srcDir = tempRepoDir.resolve("src/main/java/com/example");
        Files.createDirectories(srcDir);
        Files.write(srcDir.resolve("Widget.java"),
                "package com.example;\npublic class Widget {\n  private String name;\n}\n".getBytes(StandardCharsets.UTF_8));

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
        SyncJobStatus job = orchestrator.triggerSingleRepoSync("local-test-repo").get();
        String jobId = job.getJobId();
        assertThat(jobId).isNotBlank();

        // Wait until job completes (max 30s)
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            SyncJobStatus status = orchestrator.getJobStatus(jobId).get();
            assertThat(status.getStatus().name()).isIn("COMPLETED", "FAILED");
        });

        SyncJobStatus finalStatus = orchestrator.getJobStatus(jobId).get();
        assertThat(finalStatus.getStatus().name()).isEqualTo("COMPLETED");

        // The com.example.Widget class should now be in the graph
        assertThat(classRepository.findByFullyQualifiedName("com.example.Widget")).isPresent();
    }
}

