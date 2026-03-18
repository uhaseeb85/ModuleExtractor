package com.extractor.api.dto;

import java.util.Objects;

/**
 * Lightweight DTO representing a class node in the dependency graph.
 */
public final class GraphNodeResponse {

    private final Long id;
    private final String fqn;
    private final String simpleName;
    private final String classType;
    private final String repoName;
    private final String packageName;
    private final boolean isAbstract;
    private final int methodCount;

    public GraphNodeResponse(Long id, String fqn, String simpleName, String classType,
                              String repoName, String packageName, boolean isAbstract, int methodCount) {
        this.id = id;
        this.fqn = fqn;
        this.simpleName = simpleName;
        this.classType = classType;
        this.repoName = repoName;
        this.packageName = packageName;
        this.isAbstract = isAbstract;
        this.methodCount = methodCount;
    }

    public Long getId() { return id; }
    public String getFqn() { return fqn; }
    public String getSimpleName() { return simpleName; }
    public String getClassType() { return classType; }
    public String getRepoName() { return repoName; }
    public String getPackageName() { return packageName; }
    public boolean isAbstract() { return isAbstract; }
    public int getMethodCount() { return methodCount; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GraphNodeResponse)) return false;
        GraphNodeResponse that = (GraphNodeResponse) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
