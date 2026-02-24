package com.extractor.graph.entity;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;

import java.util.Objects;

/**
 * Neo4j node representing a field declared on a Java class.
 */
@Node("Field")
public class FieldEntity {

    @Id
    @GeneratedValue
    private Long id;

    @Property(name = "name")
    private String name;

    @Property(name = "fieldType")
    private String fieldType;

    @Property(name = "visibility")
    private String visibility;

    /** JSON array string of annotation simple names, e.g. {@code ["Autowired","Qualifier"]}. */
    @Property(name = "annotations")
    private String annotations;

    @Property(name = "repoName")
    private String repoName;

    protected FieldEntity() {}

    public FieldEntity(String name, String fieldType, String visibility, String annotations, String repoName) {
        this.name = name;
        this.fieldType = fieldType;
        this.visibility = visibility;
        this.annotations = annotations;
        this.repoName = repoName;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getFieldType() { return fieldType; }
    public String getVisibility() { return visibility; }
    public String getAnnotations() { return annotations; }
    public void setAnnotations(String annotations) { this.annotations = annotations; }
    public String getRepoName() { return repoName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldEntity that)) return false;
        return Objects.equals(name, that.name) && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, repoName);
    }
}
