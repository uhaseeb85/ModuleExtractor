package com.extractor.graph.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Neo4j node representing a Java method declaration.
 */
@Node("Method")
public class MethodEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Property(name = "signature")
    private String signature;

    @Property(name = "name")
    private String name;

    @Property(name = "returnType")
    private String returnType;

    @Property(name = "visibility")
    private String visibility;

    @Property(name = "isStatic")
    private boolean isStatic;

    @Property(name = "repoName")
    private String repoName;

    @Property(name = "lineNumber")
    private int lineNumber;

    @Property(name = "javadoc")
    private String javadoc;

    @Relationship(type = "CALLS", direction = Relationship.Direction.OUTGOING)
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

    public Long getId() { return id; }
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
        if (!(o instanceof MethodEntity that)) return false;
        return Objects.equals(signature, that.signature) && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(signature, repoName);
    }
}
