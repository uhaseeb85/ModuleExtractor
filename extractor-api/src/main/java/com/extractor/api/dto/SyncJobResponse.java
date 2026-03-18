package com.extractor.api.dto;

import com.extractor.core.enums.SyncStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Response DTO for sync job status queries.
 */
public final class SyncJobResponse {

    private final String jobId;
    private final SyncStatus status;
    private final int progressPercent;
    private final String currentRepo;
    private final List<String> errors;
    private final Instant startedAt;
    private final Instant completedAt;

    public SyncJobResponse(String jobId, SyncStatus status, int progressPercent,
                            String currentRepo, List<String> errors,
                            Instant startedAt, Instant completedAt) {
        this.jobId = jobId;
        this.status = status;
        this.progressPercent = progressPercent;
        this.currentRepo = currentRepo;
        this.errors = errors;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
    }

    public String getJobId() { return jobId; }
    public SyncStatus getStatus() { return status; }
    public int getProgressPercent() { return progressPercent; }
    public String getCurrentRepo() { return currentRepo; }
    public List<String> getErrors() { return errors; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SyncJobResponse)) return false;
        SyncJobResponse that = (SyncJobResponse) o;
        return Objects.equals(jobId, that.jobId);
    }

    @Override
    public int hashCode() { return Objects.hash(jobId); }
}
