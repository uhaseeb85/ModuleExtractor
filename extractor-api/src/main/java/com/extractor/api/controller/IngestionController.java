package com.extractor.api.controller;

import com.extractor.api.dto.AddRepoRequest;
import com.extractor.api.dto.RepoSummaryResponse;
import com.extractor.api.dto.ScanDirectoryRequest;
import com.extractor.api.dto.SyncJobResponse;
import com.extractor.core.enums.BuildTool;
import com.extractor.core.model.RepoConfig;
import com.extractor.graph.repository.ClassEntityRepository;
import com.extractor.graph.repository.RepositoryEntityRepository;
import com.extractor.ingestion.model.SyncJobStatus;
import com.extractor.ingestion.service.IngestionOrchestrator;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for repository ingestion / sync operations.
 *
 * <p>Base path: {@code /api/v1/ingestion}
 */
@RestController
@RequestMapping("/api/v1/ingestion")
public class IngestionController {

    private final IngestionOrchestrator orchestrator;
    private final RepositoryEntityRepository repoEntityRepository;
    private final ClassEntityRepository classEntityRepository;

    public IngestionController(IngestionOrchestrator orchestrator,
                               RepositoryEntityRepository repoEntityRepository,
                               ClassEntityRepository classEntityRepository) {
        this.orchestrator = orchestrator;
        this.repoEntityRepository = repoEntityRepository;
        this.classEntityRepository = classEntityRepository;
    }

    /**
     * POST /api/v1/ingestion/sync
     * Trigger a full asynchronous sync of all configured repositories.
     */
    @PostMapping("/sync")
    public ResponseEntity<SyncJobResponse> triggerFullSync() {
        SyncJobStatus job = orchestrator.triggerFullSync();
        return ResponseEntity.accepted().body(toResponse(job));
    }

    /**
     * POST /api/v1/ingestion/sync/{repoName}
     * Trigger a sync for a single repository.
     */
    @PostMapping("/sync/{repoName}")
    public ResponseEntity<?> triggerSingleSync(@PathVariable String repoName) {
        return orchestrator.triggerSingleRepoSync(repoName)
                .<ResponseEntity<?>>map(job -> ResponseEntity.accepted().body(toResponse(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/ingestion/jobs/{jobId}
     * Poll the status of a sync job.
     */
    @GetMapping("/jobs/{jobId}")
    public ResponseEntity<SyncJobResponse> getJobStatus(@PathVariable String jobId) {
        return orchestrator.getJobStatus(jobId)
                .map(job -> ResponseEntity.ok(toResponse(job)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/ingestion/repos
     * List configured repositories with their sync state.
     */
    @GetMapping("/repos")
    public List<RepoSummaryResponse> listRepos() {
        return orchestrator.getConfiguredRepos().stream()
                .map(repo -> {
                    var entity = repoEntityRepository.findByName(repo.name());
                    long nodeCount = classEntityRepository.findByRepoName(repo.name()).size();
                    return new RepoSummaryResponse(
                            repo.name(),
                            repo.url(),
                            repo.branch(),
                            repo.buildTool().name(),
                            entity.map(e -> e.getLastSyncSha()).orElse(null),
                            entity.map(e -> e.getSyncedAt()).orElse(null),
                            nodeCount
                    );
                })
                .toList();
    }

    /**
     * POST /api/v1/ingestion/repos
     * Register a new repository at runtime (no restart required).
     * Optionally pass {@code ?sync=true} to immediately trigger ingestion.
     */
    @PostMapping("/repos")
    public ResponseEntity<?> addRepo(@Valid @RequestBody AddRepoRequest req,
                                     @RequestParam(value = "sync", defaultValue = "false") boolean sync) {
        try {
            RepoConfig config = new RepoConfig(
                    req.getName(),
                    req.getUrl(),
                    req.getBranch(),
                    BuildTool.valueOf(req.getBuildTool().toUpperCase()),
                    req.getLocalPath()
            );
            orchestrator.addRepo(config);

            // Optionally kick off immediate ingestion
            if (sync) {
                SyncJobStatus job = orchestrator.triggerFullSync();
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                        "repo", req.getName(),
                        "syncJobId", job.getJobId()
                ));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("repo", req.getName()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/v1/ingestion/scan-directory
     * Scan a local filesystem directory for Git repositories and register each one.
     * The directory may itself be a Git repo, or contain multiple Git repos as
     * immediate sub-directories.  Build tool is auto-detected (pom.xml → Maven,
     * build.gradle → Gradle) with the supplied {@code buildTool} as a fallback.
     * Optionally pass {@code ?sync=true} to trigger ingestion after registration.
     */
    @PostMapping("/scan-directory")
    public ResponseEntity<?> scanDirectory(@Valid @RequestBody ScanDirectoryRequest req,
                                           @RequestParam(value = "sync", defaultValue = "false") boolean sync) {
        try {
            List<com.extractor.core.model.RepoConfig> found = orchestrator.scanLocalDirectory(
                    req.getDirectoryPath(),
                    BuildTool.valueOf(req.getBuildTool()),
                    req.getBranch()
            );

            if (found.isEmpty()) {
                return ResponseEntity.ok(Map.of(
                        "message", "No new Git repositories found under: " + req.getDirectoryPath(),
                        "registered", List.of()
                ));
            }

            List<String> names = found.stream().map(com.extractor.core.model.RepoConfig::name).toList();

            if (sync) {
                SyncJobStatus job = orchestrator.triggerFullSync();
                return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                        "registered", names,
                        "syncJobId", job.getJobId()
                ));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("registered", names));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/v1/ingestion/repos/{name}
     * Remove a repository from the runtime list (does not delete cloned files).
     */
    @DeleteMapping("/repos/{name}")
    public ResponseEntity<Void> removeRepo(@PathVariable String name) {
        boolean removed = orchestrator.removeRepo(name);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private SyncJobResponse toResponse(SyncJobStatus job) {
        return new SyncJobResponse(
                job.getJobId(),
                job.getStatus(),
                job.getProgressPercent(),
                job.getCurrentRepo(),
                List.copyOf(job.getErrors()),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }
}
