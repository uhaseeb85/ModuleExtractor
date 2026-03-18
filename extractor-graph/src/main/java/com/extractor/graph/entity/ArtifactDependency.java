package com.extractor.graph.entity;

/**
 * Plain-POJO representing a DEPENDS_ON relationship between two artifacts.
 */
public class ArtifactDependency {

    private ArtifactEntity dependency;
    private String scope;
    private boolean isTransitive;

    protected ArtifactDependency() {}

    public ArtifactDependency(ArtifactEntity dependency, String scope, boolean isTransitive) {
        this.dependency = dependency;
        this.scope = scope;
        this.isTransitive = isTransitive;
    }

    public ArtifactEntity getDependency() { return dependency; }
    public String getScope() { return scope; }
    public boolean isTransitive() { return isTransitive; }
}
