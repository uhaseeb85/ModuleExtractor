package com.extractor.ingestion.service;

import com.extractor.core.enums.BuildTool;
import com.extractor.core.enums.SyncStatus;
import com.extractor.core.interfaces.BuildFileParser;
import com.extractor.core.interfaces.GraphBuilder;
import com.extractor.core.interfaces.JavaSourceParser;
import com.extractor.core.interfaces.RepoScanner;
import com.extractor.core.model.DependsOnEdge;
import com.extractor.core.model.ParseResult;
import com.extractor.core.model.RepoConfig;
import com.extractor.ingestion.config.ExtractorProperties;
import com.extractor.ingestion.model.SyncJobStatus;
import com.extractor.ingestion.parser.JavaSourceParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Orchestrates the full ingestion pipeline:
 * <ol>
 *   <li>Clone / pull each configured repository via {@link RepoScanner}</li>
 *   <li>Resolve build dependencies via the appropriate {@link BuildFileParser}</li>
 *   <li>Register source roots and JARs with {@link JavaSourceParserImpl}</li>
 *   <li>Parse each {@code .java} file via {@link JavaSourceParser}</li>
 *   <li>Persist results to Neo4j via {@link GraphBuilder}</li>
 * </ol>
 *
 * <p>Each sync is executed asynchronously. Job state is tracked in a
 * {@link ConcurrentHashMap} and exposed via the REST API.
 */
@Service
public class IngestionOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(IngestionOrchestrator.class);

    private final RepoScanner repoScanner;
    private final JavaSourceParserImpl javaSourceParser;
    private final List<BuildFileParser> buildFileParsers;
    private final GraphBuilder graphBuilder;
    private final ExtractorProperties properties;

    private final Map<String, SyncJobStatus> jobs = new ConcurrentHashMap<>();

    /** Runtime-mutable list of repos — seeded from config, then editable via API. */
    private final CopyOnWriteArrayList<RepoConfig> configuredRepos = new CopyOnWriteArrayList<>();

    public IngestionOrchestrator(RepoScanner repoScanner,
                                 JavaSourceParserImpl javaSourceParser,
                                 List<BuildFileParser> buildFileParsers,
                                 GraphBuilder graphBuilder,
                                 ExtractorProperties properties) {
        this.repoScanner = repoScanner;
        this.javaSourceParser = javaSourceParser;
        this.buildFileParsers = buildFileParsers;
        this.graphBuilder = graphBuilder;
        this.properties = properties;
        this.configuredRepos.addAll(properties.toRepoConfigs());
    }

    /**
     * Triggers an asynchronous full sync of all configured repositories.
     * Returns immediately with a job handle; the pipeline runs in a background thread.
     *
     * @return The {@link SyncJobStatus} tracking this job (use {@code jobId} to poll).
     */
    public SyncJobStatus triggerFullSync() {
        SyncJobStatus job = new SyncJobStatus();
        jobs.put(job.getJobId(), job);
        List<RepoConfig> snapshot = List.copyOf(configuredRepos);
        CompletableFuture.runAsync(() -> runSync(job, snapshot));
        return job;
    }

    /**
     * Triggers an asynchronous sync of a single named repository.
     * Returns immediately with a job handle; the pipeline runs in a background thread.
     *
     * @param repoName Name as defined in configuration or added via API.
     * @return The job status, or empty if no repo with that name is configured.
     */
    public Optional<SyncJobStatus> triggerSingleRepoSync(String repoName) {
        Optional<RepoConfig> repoConfig = configuredRepos.stream()
                .filter(r -> r.name().equals(repoName))
                .findFirst();

        if (repoConfig.isEmpty()) {
            log.warn("Sync requested for unknown repo '{}'", repoName);
            return Optional.empty();
        }

        SyncJobStatus job = new SyncJobStatus();
        jobs.put(job.getJobId(), job);
        RepoConfig cfg = repoConfig.get();
        CompletableFuture.runAsync(() -> runSync(job, List.of(cfg)));
        return Optional.of(job);
    }

    /**
     * Returns the status of a sync job by ID.
     */
    public Optional<SyncJobStatus> getJobStatus(String jobId) {
        return Optional.ofNullable(jobs.get(jobId));
    }

    /**
     * Returns the runtime list of all configured repos.
     */
    public List<RepoConfig> getConfiguredRepos() {
        return List.copyOf(configuredRepos);
    }

    /**
     * Adds a new repository to the runtime list.
     * Throws {@link IllegalArgumentException} if a repo with the same name already exists.
     */
    public RepoConfig addRepo(RepoConfig config) {
        boolean exists = configuredRepos.stream().anyMatch(r -> r.name().equals(config.name()));
        if (exists) {
            throw new IllegalArgumentException("Repository '" + config.name() + "' is already registered.");
        }
        configuredRepos.add(config);
        log.info("Registered new repo '{}' ({})", config.name(), config.url());
        return config;
    }

    /**
     * Removes a repository by name from the runtime list.
     *
     * @return {@code true} if removed, {@code false} if not found.
     */
    public boolean removeRepo(String name) {
        boolean removed = configuredRepos.removeIf(r -> r.name().equals(name));
        if (removed) {
            log.info("Removed repo '{}'", name);
        }
        return removed;
    }

    /**
     * Nightly scheduled re-sync of all repositories (default: 2am daily).
     */
    @Scheduled(cron = "${extractor.ingestion.cron:0 0 2 * * *}")
    public void scheduledSync() {
        log.info("Starting scheduled nightly sync...");
        triggerFullSync();
    }

    // ── Core pipeline ────────────────────────────────────────────────────

    private void runSync(SyncJobStatus job, List<RepoConfig> repos) {
        job.setStatus(SyncStatus.RUNNING);
        int total = repos.size();
        int done = 0;

        try {
            // Phase 1: Sync all repos and register all source roots / JARs with the parser
            // This must happen before parsing so CombinedTypeSolver has the full classpath
            List<String> headShas = new ArrayList<>();
            for (RepoConfig repo : repos) {
                job.setCurrentRepo(repo.name());
                try {
                    log.info("Syncing repo '{}'", repo.name());
                    String sha = repoScanner.syncRepo(repo);
                    headShas.add(sha);

                    // Register source root for cross-repo type resolution
                    Path sourceRoot = Path.of(repo.localPath(), "src", "main", "java");
                    javaSourceParser.registerSourceRoot(sourceRoot);

                } catch (Exception e) {
                    log.error("Failed to sync repo '{}': {}", repo.name(), e.getMessage());
                    job.addError("Sync failed for " + repo.name() + ": " + e.getMessage());
                }
            }

            // Phase 2: Resolve build dependencies and register JARs
            for (int i = 0; i < repos.size(); i++) {
                RepoConfig repo = repos.get(i);
                try {
                    BuildFileParser parser = findParser(repo.buildTool());
                    Path projectDir = Path.of(repo.localPath());

                    List<Path> jarPaths = parser.resolveJarPaths(projectDir, repo);
                    jarPaths.forEach(javaSourceParser::registerJar);

                    List<DependsOnEdge> deps = parser.resolveDependencies(projectDir, repo);
                    graphBuilder.persistDependencies(deps);

                    log.info("Resolved {} dependencies for '{}'", deps.size(), repo.name());
                } catch (Exception e) {
                    log.error("Build parse failed for repo '{}': {}", repo.name(), e.getMessage());
                    job.addError("Build parse failed for " + repo.name() + ": " + e.getMessage());
                }
            }

            // Phase 3: Parse Java source files and persist to graph
            for (RepoConfig repo : repos) {
                job.setCurrentRepo(repo.name());
                try {
                    List<Path> javaFiles = repoScanner.getChangedFiles(repo, null);
                    log.info("Parsing {} Java files for repo '{}'", javaFiles.size(), repo.name());

                    List<ParseResult> batch = new ArrayList<>();
                    for (Path javaFile : javaFiles) {
                        try {
                            ParseResult result = javaSourceParser.parse(javaFile, repo);
                            batch.add(result);
                            if (batch.size() >= 100) {
                                graphBuilder.persistBatch(batch, repo);
                                batch.clear();
                            }
                        } catch (Exception e) {
                            log.warn("Parse failed for '{}': {}", javaFile, e.getMessage());
                        }
                    }
                    if (!batch.isEmpty()) {
                        graphBuilder.persistBatch(batch, repo);
                    }

                } catch (Exception e) {
                    log.error("Ingestion failed for repo '{}': {}", repo.name(), e.getMessage());
                    job.addError("Ingestion failed for " + repo.name() + ": " + e.getMessage());
                }

                done++;
                job.setProgressPercent((done * 100) / total);
            }

            job.setStatus(job.getErrors().isEmpty() ? SyncStatus.COMPLETED : SyncStatus.FAILED);

        } catch (Exception e) {
            log.error("Unexpected error during sync: {}", e.getMessage(), e);
            job.addError("Unexpected error: " + e.getMessage());
            job.setStatus(SyncStatus.FAILED);
        } finally {
            job.setCompletedAt(Instant.now());
            job.setProgressPercent(100);
            job.setCurrentRepo(null);
        }
    }

    private BuildFileParser findParser(BuildTool buildTool) {
        return buildFileParsers.stream()
                .filter(p -> p.supports(buildTool))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No BuildFileParser found for " + buildTool));
    }
}
