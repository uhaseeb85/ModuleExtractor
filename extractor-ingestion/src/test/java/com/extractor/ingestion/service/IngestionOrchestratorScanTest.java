package com.extractor.ingestion.service;

import com.extractor.core.enums.BuildTool;
import com.extractor.core.interfaces.BuildFileParser;
import com.extractor.core.interfaces.GraphBuilder;
import com.extractor.core.interfaces.RepoScanner;
import com.extractor.core.model.RepoConfig;
import com.extractor.ingestion.config.ExtractorProperties;
import com.extractor.ingestion.parser.JavaSourceParserImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link IngestionOrchestrator#scanLocalDirectory}.
 *
 * <p>These tests use real temp directories to exercise the file-system
 * scanning logic without requiring Spring or Neo4j.
 */
class IngestionOrchestratorScanTest {

    @TempDir
    Path tempDir;

    private IngestionOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        ExtractorProperties props = mock(ExtractorProperties.class);
        when(props.toRepoConfigs()).thenReturn(Collections.emptyList());

        orchestrator = new IngestionOrchestrator(
                mock(RepoScanner.class),
                mock(JavaSourceParserImpl.class),
                Arrays.asList(mock(BuildFileParser.class)),
                mock(GraphBuilder.class),
                props
        );
    }

    // ── scanLocalDirectory: invalid path ────────────────────────────────

    @Test
    void throwsWhenPathIsNotADirectory() {
        assertThatThrownBy(() ->
                orchestrator.scanLocalDirectory("/no/such/path", BuildTool.MAVEN, "main"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not a directory");
    }

    // ── scanLocalDirectory: root itself is a git repo ───────────────────

    @Test
    void rootGitRepoIsRegistered() throws IOException {
        // Make tempDir itself a git repo
        Files.createDirectory(tempDir.resolve(".git"));
        Files.write(tempDir.resolve("pom.xml"), "<project/>".getBytes(StandardCharsets.UTF_8));

        List<RepoConfig> result = orchestrator.scanLocalDirectory(
                tempDir.toString(), BuildTool.GRADLE, "main");

        assertThat(result).hasSize(1);
        RepoConfig cfg = result.get(0);
        assertThat(cfg.getName()).isEqualTo(tempDir.getFileName().toString());
        assertThat(cfg.getBuildTool()).isEqualTo(BuildTool.MAVEN);  // auto-detected
        assertThat(cfg.getLocalPath()).isEqualTo(tempDir.toAbsolutePath().toString());
        assertThat(cfg.getUrl()).isEqualTo(tempDir.toAbsolutePath().toString());
    }

    // ── scanLocalDirectory: directory containing sub-repos ──────────────

    @Test
    void subDirectoryGitReposAreRegistered() throws IOException {
        Path repoA = tempDir.resolve("service-a");
        Path repoB = tempDir.resolve("service-b");
        Files.createDirectories(repoA.resolve(".git"));
        Files.createDirectories(repoB.resolve(".git"));
        Files.write(repoA.resolve("pom.xml"), "<project/>".getBytes(StandardCharsets.UTF_8));
        Files.write(repoB.resolve("build.gradle"), "plugins {}".getBytes(StandardCharsets.UTF_8));

        List<RepoConfig> result = orchestrator.scanLocalDirectory(
                tempDir.toString(), BuildTool.MAVEN, "develop");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(RepoConfig::getName)
                .containsExactlyInAnyOrder("service-a", "service-b");
        assertThat(result).filteredOn(r -> r.getName().equals("service-a"))
                .first().extracting(RepoConfig::getBuildTool).isEqualTo(BuildTool.MAVEN);
        assertThat(result).filteredOn(r -> r.getName().equals("service-b"))
                .first().extracting(RepoConfig::getBuildTool).isEqualTo(BuildTool.GRADLE);
    }

    // ── scanLocalDirectory: empty directory returns empty list ──────────

    @Test
    void emptyDirectoryReturnsEmptyList() {
        List<RepoConfig> result = orchestrator.scanLocalDirectory(
                tempDir.toString(), BuildTool.MAVEN, "main");

        assertThat(result).isEmpty();
    }

    // ── scanLocalDirectory: non-git sub-dirs are ignored ────────────────

    @Test
    void nonGitSubDirectoriesAreIgnored() throws IOException {
        Files.createDirectories(tempDir.resolve("not-a-repo/src"));  // no .git folder
        Path realRepo = tempDir.resolve("real-repo");
        Files.createDirectories(realRepo.resolve(".git"));

        List<RepoConfig> result = orchestrator.scanLocalDirectory(
                tempDir.toString(), BuildTool.MAVEN, "main");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("real-repo");
    }

    // ── scanLocalDirectory: duplicate repos are skipped ─────────────────

    @Test
    void alreadyRegisteredRepoIsSkippedGracefully() throws IOException {
        Path repoDir = tempDir.resolve("my-repo");
        Files.createDirectories(repoDir.resolve(".git"));

        // First scan registers the repo
        List<RepoConfig> first = orchestrator.scanLocalDirectory(
                tempDir.toString(), BuildTool.MAVEN, "main");
        assertThat(first).hasSize(1);

        // Second scan with the same directory silently skips the duplicate
        List<RepoConfig> second = orchestrator.scanLocalDirectory(
                tempDir.toString(), BuildTool.MAVEN, "main");
        assertThat(second).isEmpty();

        // Total registered repos should still be just 1
        assertThat(orchestrator.getConfiguredRepos()).hasSize(1);
    }

    // ── scanLocalDirectory: build tool fallback ──────────────────────────

    @Test
    void fallbackBuildToolUsedWhenNoBuildFileFound() throws IOException {
        Path repoDir = tempDir.resolve("unknown-build-repo");
        Files.createDirectories(repoDir.resolve(".git"));
        // No pom.xml or build.gradle

        List<RepoConfig> result = orchestrator.scanLocalDirectory(
                tempDir.toString(), BuildTool.GRADLE, "main");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBuildTool()).isEqualTo(BuildTool.GRADLE);
    }

    // ── scanLocalDirectory: branch is recorded on each repo ─────────────

    @Test
    void branchIsRecordedOnEachDiscoveredRepo() throws IOException {
        Path repoDir = tempDir.resolve("my-service");
        Files.createDirectories(repoDir.resolve(".git"));

        List<RepoConfig> result = orchestrator.scanLocalDirectory(
                tempDir.toString(), BuildTool.MAVEN, "release/2.0");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBranch()).isEqualTo("release/2.0");
    }
}
