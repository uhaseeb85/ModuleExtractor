package com.extractor.graph.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Plain-POJO representing a Java method declaration.
 * Stored in-memory via {@link com.extractor.graph.store.GraphStore}.
 */
public class MethodEntity {

    private String signature;
    private String name;
    private String returnType;
    private String visibility;
    private boolean isStatic;
    private String repoName;
    private int lineNumber;
    private String javadoc;

    private List<MethodEntity> calledMethods = new ArrayList<>();

    protected MethodEntity() {}

    public MethodEntity(String signature, String name, String returnType,
                        String visibility, boolean isStatic, String repoName, int lineNumber) {
        this.signature = signature;
        this.name = name;
        this.returnType = returnType;
        this.visibility = visibility;
        this.isStatic = isStatic;
        this.repoName = repoName;
        this.lineNumber = lineNumber;
    }

    /** Synthetic ID derived from signature+repo hashCode. */
    public Long getId() { return (long) Objects.hash(signature, repoName); }
    public String getSignature() { return signature; }
    public String getName() { return name; }
    public String getReturnType() { return returnType; }
    public String getVisibility() { return visibility; }
    public boolean isStatic() { return isStatic; }
    public String getRepoName() { return repoName; }
    public int getLineNumber() { return lineNumber; }
    public String getJavadoc() { return javadoc; }
    public void setJavadoc(String javadoc) { this.javadoc = javadoc; }
    public List<MethodEntity> getCalledMethods() { return calledMethods; }
    public void setCalledMethods(List<MethodEntity> calledMethods) { this.calledMethods = calledMethods; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodEntity)) return false;
        MethodEntity that = (MethodEntity) o;
        return Objects.equals(signature, that.signature) && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signature, repoName);
    }
}
