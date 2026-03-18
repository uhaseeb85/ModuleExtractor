package com.extractor.api.dto;

import java.util.List;
import java.util.Objects;

/**
 * Detailed view of one package candidate with class list and blockers.
 */
public final class CandidateDetailDto {

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
    private final List<String> blockers;
    private final List<String> classNames;

    public CandidateDetailDto(String packageFqn, String repoName, int classCount,
                               int inboundDeps, int outboundDeps,
                               double isolationScore, double stabilityScore,
                               double sizeScore, double compositeScore, String recommendation,
                               List<String> blockers, List<String> classNames) {
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
        this.blockers = blockers;
        this.classNames = classNames;
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
    public List<String> getBlockers() { return blockers; }
    public List<String> getClassNames() { return classNames; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CandidateDetailDto)) return false;
        CandidateDetailDto that = (CandidateDetailDto) o;
        return Objects.equals(packageFqn, that.packageFqn) && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() { return Objects.hash(packageFqn, repoName); }
}
