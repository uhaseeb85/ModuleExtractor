package com.extractor.core.model;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable representation of a method declared on a Java class.
 */
public final class MethodNode {

    private final String signature;
    private final String name;
    private final String returnType;
    private final String visibility;
    private final boolean isStatic;
    private final String repoName;
    private final int lineNumber;
    private final Optional<String> javadoc;

    public MethodNode(String signature, String name, String returnType, String visibility,
                      boolean isStatic, String repoName, int lineNumber, Optional<String> javadoc) {
        this.signature = signature;
        this.name = name;
        this.returnType = returnType;
        this.visibility = visibility;
        this.isStatic = isStatic;
        this.repoName = repoName;
        this.lineNumber = lineNumber;
        this.javadoc = javadoc == null ? Optional.<String>empty() : javadoc;
    }

    public String getSignature() { return signature; }
    public String getName() { return name; }
    public String getReturnType() { return returnType; }
    public String getVisibility() { return visibility; }
    public boolean isStatic() { return isStatic; }
    public String getRepoName() { return repoName; }
    public int getLineNumber() { return lineNumber; }
    public Optional<String> getJavadoc() { return javadoc; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MethodNode)) return false;
        MethodNode that = (MethodNode) o;
        return Objects.equals(signature, that.signature) && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() { return Objects.hash(signature, repoName); }

    @Override
    public String toString() {
        return "MethodNode{signature='" + signature + "', repo='" + repoName + "'}";
    }
}
