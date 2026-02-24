package com.extractor.ingestion.model;

import com.extractor.core.enums.SyncStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Tracks the state of an async repository sync job.
 */
public class SyncJobStatus {

    private final String jobId;
    private volatile SyncStatus status;
    private volatile int progressPercent;
    private volatile String currentRepo;
    private final List<String> errors = new ArrayList<>();
    private final Instant startedAt;
    private volatile Instant completedAt;

    public SyncJobStatus() {
        this.jobId = UUID.randomUUID().toString();
        this.status = SyncStatus.PENDING;
        this.progressPercent = 0;
        this.startedAt = Instant.now();
    }

    public String getJobId() { return jobId; }
    public SyncStatus getStatus() { return status; }
    public void setStatus(SyncStatus status) { this.status = status; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }
    public String getCurrentRepo() { return currentRepo; }
    public void setCurrentRepo(String currentRepo) { this.currentRepo = currentRepo; }
    public List<String> getErrors() { return errors; }
    public void addError(String error) { this.errors.add(error); }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
}
