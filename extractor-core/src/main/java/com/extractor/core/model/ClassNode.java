package com.extractor.core.model;

import com.extractor.core.enums.ClassType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable representation of a parsed Java class (or interface, enum, annotation, record).
 */
public final class ClassNode {

    private final String fqn;
    private final String simpleName;
    private final ClassType classType;
    private final boolean isAbstract;
    private final String repoName;
    private final Optional<String> javadoc;
    private final List<MethodNode> methods;
    private final List<FieldNode> fields;
    private final List<String> annotations;
    private final String packageName;
    private final int lineNumber;
    /** Absolute path to the original .java source file on disk (may be null). */
    private final String sourceFilePath;

    public ClassNode(String fqn, String simpleName, ClassType classType, boolean isAbstract,
                     String repoName, Optional<String> javadoc,
                     List<MethodNode> methods, List<FieldNode> fields, List<String> annotations,
                     String packageName, int lineNumber) {
        this(fqn, simpleName, classType, isAbstract, repoName, javadoc, methods, fields, annotations,
                packageName, lineNumber, null);
    }

    public ClassNode(String fqn, String simpleName, ClassType classType, boolean isAbstract,
                     String repoName, Optional<String> javadoc,
                     List<MethodNode> methods, List<FieldNode> fields, List<String> annotations,
                     String packageName, int lineNumber, String sourceFilePath) {
        this.fqn = fqn;
        this.simpleName = simpleName;
        this.classType = classType;
        this.isAbstract = isAbstract;
        this.repoName = repoName;
        this.javadoc = javadoc == null ? Optional.<String>empty() : javadoc;
        this.methods = Collections.unmodifiableList(new ArrayList<MethodNode>(methods));
        this.fields = Collections.unmodifiableList(new ArrayList<FieldNode>(fields));
        this.annotations = Collections.unmodifiableList(new ArrayList<String>(annotations));
        this.packageName = packageName;
        this.lineNumber = lineNumber;
        this.sourceFilePath = sourceFilePath;
    }

    public String getFqn() { return fqn; }
    public String getSimpleName() { return simpleName; }
    public ClassType getClassType() { return classType; }
    public boolean isAbstract() { return isAbstract; }
    public String getRepoName() { return repoName; }
    public Optional<String> getJavadoc() { return javadoc; }
    public List<MethodNode> getMethods() { return methods; }
    public List<FieldNode> getFields() { return fields; }
    public List<String> getAnnotations() { return annotations; }
    public String getPackageName() { return packageName; }
    public int getLineNumber() { return lineNumber; }
    public String getSourceFilePath() { return sourceFilePath; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassNode)) return false;
        ClassNode that = (ClassNode) o;
        return Objects.equals(fqn, that.fqn) && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() { return Objects.hash(fqn, repoName); }

    @Override
    public String toString() {
        return "ClassNode{fqn='" + fqn + "', repo='" + repoName + "'}";
    }
}
