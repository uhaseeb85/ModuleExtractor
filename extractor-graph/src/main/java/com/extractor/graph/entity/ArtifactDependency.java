package com.extractor.graph.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * Relationship properties for a DEPENDS_ON edge between two artifacts.
 */
@RelationshipProperties
public class ArtifactDependency {

    @Id
    @GeneratedValue
    private Long id;

    @TargetNode
    private ArtifactEntity dependency;

    private String scope;
    private boolean isTransitive;

    protected ArtifactDependency() {}

    public ArtifactDependency(ArtifactEntity dependency, String scope, boolean isTransitive) {
        this.dependency = dependency;
        this.scope = scope;
        this.isTransitive = isTransitive;
    }

    public Long getId() { return id; }
    public ArtifactEntity getDependency() { return dependency; }
    public String getScope() { return scope; }
    public boolean isTransitive() { return isTransitive; }
}
