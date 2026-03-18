package com.extractor.api.dto;

import java.util.Objects;

/**
 * Lightweight summary of one package's extraction-candidate scores.
 */
public final class CandidateSummaryDto {

    private final String packageFqn;
    private final String repoName;
    private final int classCount;
    private final int inboundDeps;
    private final int outboundDeps;
    private final double isolationScore;
    private final double stabilityScore;
    private final double sizeScore;
    private final double compositeScore;
    private final String recommendation;

    public CandidateSummaryDto(String packageFqn, String repoName, int classCount,
                                int inboundDeps, int outboundDeps,
                                double isolationScore, double stabilityScore,
                                double sizeScore, double compositeScore, String recommendation) {
        this.packageFqn = packageFqn;
        this.repoName = repoName;
        this.classCount = classCount;
        this.inboundDeps = inboundDeps;
        this.outboundDeps = outboundDeps;
        this.isolationScore = isolationScore;
        this.stabilityScore = stabilityScore;
        this.sizeScore = sizeScore;
        this.compositeScore = compositeScore;
        this.recommendation = recommendation;
    }

    public String getPackageFqn() { return packageFqn; }
    public String getRepoName() { return repoName; }
    public int getClassCount() { return classCount; }
    public int getInboundDeps() { return inboundDeps; }
    public int getOutboundDeps() { return outboundDeps; }
    public double getIsolationScore() { return isolationScore; }
    public double getStabilityScore() { return stabilityScore; }
    public double getSizeScore() { return sizeScore; }
    public double getCompositeScore() { return compositeScore; }
    public String getRecommendation() { return recommendation; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CandidateSummaryDto)) return false;
        CandidateSummaryDto that = (CandidateSummaryDto) o;
        return Objects.equals(packageFqn, that.packageFqn) && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() { return Objects.hash(packageFqn, repoName); }
}
