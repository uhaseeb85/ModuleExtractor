package com.extractor.graph.entity;

import com.extractor.core.enums.ClassType;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Neo4j node representing a Java class, interface, enum, annotation, or record.
 */
@Node("Class")
public class ClassEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Property(name = "fullyQualifiedName")
    private String fullyQualifiedName;

    @Property(name = "simpleName")
    private String simpleName;

    @Property(name = "classType")
    private String classType;

    @Property(name = "isAbstract")
    private boolean isAbstract;

    @Property(name = "repoName")
    private String repoName;

    @Property(name = "packageName")
    private String packageName;

    @Property(name = "lineNumber")
    private int lineNumber;

    @Property(name = "javadoc")
    private String javadoc;

    // ── Relationships ──────────────────────────────────────────────────

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<MethodEntity> methods = new ArrayList<>();

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    private List<FieldEntity> fields = new ArrayList<>();

    @Relationship(type = "IMPORTS", direction = Relationship.Direction.OUTGOING)
    private List<ClassEntity> imports = new ArrayList<>();

    @Relationship(type = "EXTENDS", direction = Relationship.Direction.OUTGOING)
    private ClassEntity superClass;

    @Relationship(type = "IMPLEMENTS", direction = Relationship.Direction.OUTGOING)
    private List<ClassEntity> implementedInterfaces = new ArrayList<>();

    @Relationship(type = "ANNOTATED_WITH", direction = Relationship.Direction.OUTGOING)
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

    public Long getId() { return id; }
    public String getFullyQualifiedName() { return fullyQualifiedName; }
    public String getSimpleName() { return simpleName; }
    public String getClassType() { return classType; }
    public boolean isAbstract() { return isAbstract; }
    public String getRepoName() { return repoName; }
    public String getPackageName() { return packageName; }
    public int getLineNumber() { return lineNumber; }
    public String getJavadoc() { return javadoc; }
    public void setJavadoc(String javadoc) { this.javadoc = javadoc; }
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
        if (!(o instanceof ClassEntity that)) return false;
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
