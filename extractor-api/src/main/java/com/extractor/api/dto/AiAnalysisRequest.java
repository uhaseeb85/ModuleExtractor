package com.extractor.api.dto;

/**
 * Request body for AI analysis endpoints.
 */
public final class AiAnalysisRequest {

    private String model;
    private String moduleName;
    private int groupDepth = 4;
    private double minScore = 0.4;

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getModuleName() { return moduleName; }
    public void setModuleName(String moduleName) { this.moduleName = moduleName; }
    public int getGroupDepth() { return groupDepth; }
    public void setGroupDepth(int groupDepth) { this.groupDepth = groupDepth; }
    public double getMinScore() { return minScore; }
    public void setMinScore(double minScore) { this.minScore = minScore; }
}
