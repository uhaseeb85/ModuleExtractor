package com.extractor.api.dto;

import java.util.List;
import java.util.Objects;

/**
 * A proposed extracted module: one or more cohesive packages grouped under a common root.
 */
public final class ModuleRecommendationDto {

    private final String moduleName;
    private final String modulePackageRoot;
    private final String repoName;
    private final List<String> packages;
    private final List<String> classes;
    private final int totalClasses;
    private final int totalInboundDeps;
    private final int totalOutboundDeps;
    private final double avgCompositeScore;
    private final double minIsolationScore;
    private final String recommendation;
    private final List<String> blockers;

    public ModuleRecommendationDto(String moduleName, String modulePackageRoot, String repoName,
                                    List<String> packages, List<String> classes,
                                    int totalClasses, int totalInboundDeps, int totalOutboundDeps,
                                    double avgCompositeScore, double minIsolationScore,
                                    String recommendation, List<String> blockers) {
        this.moduleName = moduleName;
        this.modulePackageRoot = modulePackageRoot;
        this.repoName = repoName;
        this.packages = packages;
        this.classes = classes;
        this.totalClasses = totalClasses;
        this.totalInboundDeps = totalInboundDeps;
        this.totalOutboundDeps = totalOutboundDeps;
        this.avgCompositeScore = avgCompositeScore;
        this.minIsolationScore = minIsolationScore;
        this.recommendation = recommendation;
        this.blockers = blockers;
    }

    public String getModuleName() { return moduleName; }
    public String getModulePackageRoot() { return modulePackageRoot; }
    public String getRepoName() { return repoName; }
    public List<String> getPackages() { return packages; }
    public List<String> getClasses() { return classes; }
    public int getTotalClasses() { return totalClasses; }
    public int getTotalInboundDeps() { return totalInboundDeps; }
    public int getTotalOutboundDeps() { return totalOutboundDeps; }
    public double getAvgCompositeScore() { return avgCompositeScore; }
    public double getMinIsolationScore() { return minIsolationScore; }
    public String getRecommendation() { return recommendation; }
    public List<String> getBlockers() { return blockers; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ModuleRecommendationDto)) return false;
        ModuleRecommendationDto that = (ModuleRecommendationDto) o;
        return Objects.equals(moduleName, that.moduleName) && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() { return Objects.hash(moduleName, repoName); }
}
