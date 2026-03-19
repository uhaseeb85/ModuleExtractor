package com.extractor.graph.entity;

import com.extractor.core.enums.ClassType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Plain-POJO representing a Java class, interface, enum, annotation, or record.
 * Stored in-memory via {@link com.extractor.graph.store.GraphStore}.
 */
public class ClassEntity {

    private String fullyQualifiedName;
    private String simpleName;
    private String classType;
    private boolean isAbstract;
    private String repoName;
    private String packageName;
    private int lineNumber;
    private String javadoc;
    /** Absolute path to the original .java source file on disk. */
    private String sourceFilePath;

    // ── Relationships (in-memory lists) ────────────────────────────────

    private List<MethodEntity> methods = new ArrayList<>();
    private List<FieldEntity> fields = new ArrayList<>();

    /** Direct import targets (resolved lazily during graph build). */
    private List<ClassEntity> imports = new ArrayList<>();

    private ClassEntity superClass;
    private List<ClassEntity> implementedInterfaces = new ArrayList<>();

    /** Annotation classes applied to this class (e.g. @Entity, @Service). */
    private List<ClassEntity> annotations = new ArrayList<>();

    protected ClassEntity() {}

    public ClassEntity(String fullyQualifiedName, String simpleName, ClassType classType,
                       boolean isAbstract, String repoName, String packageName, int lineNumber) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.simpleName = simpleName;
        this.classType = classType.name();
        this.isAbstract = isAbstract;
        this.repoName = repoName;
        this.packageName = packageName;
        this.lineNumber = lineNumber;
    }

    /** Synthetic ID derived from FQN hashCode (replaces Neo4j generated ID). */
    public Long getId() { return fullyQualifiedName != null ? (long) fullyQualifiedName.hashCode() : null; }
    public String getFullyQualifiedName() { return fullyQualifiedName; }
    public String getSimpleName() { return simpleName; }
    public String getClassType() { return classType; }
    public boolean isAbstract() { return isAbstract; }
    public String getRepoName() { return repoName; }
    public String getPackageName() { return packageName; }
    public int getLineNumber() { return lineNumber; }
    public String getJavadoc() { return javadoc; }
    public void setJavadoc(String javadoc) { this.javadoc = javadoc; }
    public String getSourceFilePath() { return sourceFilePath; }
    public void setSourceFilePath(String sourceFilePath) { this.sourceFilePath = sourceFilePath; }
    public List<MethodEntity> getMethods() { return methods; }
    public void setMethods(List<MethodEntity> methods) { this.methods = methods; }
    public List<FieldEntity> getFields() { return fields; }
    public void setFields(List<FieldEntity> fields) { this.fields = fields; }
    public List<ClassEntity> getImports() { return imports; }
    public void setImports(List<ClassEntity> imports) { this.imports = imports; }
    public ClassEntity getSuperClass() { return superClass; }
    public void setSuperClass(ClassEntity superClass) { this.superClass = superClass; }
    public List<ClassEntity> getImplementedInterfaces() { return implementedInterfaces; }
    public void setImplementedInterfaces(List<ClassEntity> implementedInterfaces) { this.implementedInterfaces = implementedInterfaces; }
    public List<ClassEntity> getAnnotations() { return annotations; }
    public void setAnnotations(List<ClassEntity> annotations) { this.annotations = annotations; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClassEntity)) return false;
        ClassEntity that = (ClassEntity) o;
        return Objects.equals(fullyQualifiedName, that.fullyQualifiedName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullyQualifiedName);
    }

    @Override
    public String toString() {
        return "ClassEntity{fqn='" + fullyQualifiedName + "', repo='" + repoName + "'}";
    }
}
