package com.extractor.api.dto;

public final class AiHealthResponse {

    private final boolean available;
    private final long latencyMs;
    private final String error;

    public AiHealthResponse(boolean available, long latencyMs, String error) {
        this.available = available;
        this.latencyMs = latencyMs;
        this.error = error;
    }

    public boolean isAvailable() { return available; }
    public long getLatencyMs() { return latencyMs; }
    public String getError() { return error; }
}
