package com.extractor.graph.entity;

import java.util.Objects;

/**
 * Plain-POJO representing a field declared on a Java class.
 * Stored in-memory via {@link com.extractor.graph.store.GraphStore}.
 */
public class FieldEntity {

    private String name;
    private String fieldType;
    private String visibility;

    /** JSON array string of annotation simple names, e.g. {@code ["Autowired","Qualifier"]}. */
    private String annotations;

    private String repoName;

    protected FieldEntity() {}

    public FieldEntity(String name, String fieldType, String visibility, String annotations, String repoName) {
        this.name = name;
        this.fieldType = fieldType;
        this.visibility = visibility;
        this.annotations = annotations;
        this.repoName = repoName;
    }

    /** Synthetic ID derived from name+repo hashCode. */
    public Long getId() { return (long) Objects.hash(name, repoName); }
    public String getName() { return name; }
    public String getFieldType() { return fieldType; }
    public String getVisibility() { return visibility; }
    public String getAnnotations() { return annotations; }
    public void setAnnotations(String annotations) { this.annotations = annotations; }
    public String getRepoName() { return repoName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldEntity)) return false;
        FieldEntity that = (FieldEntity) o;
        return Objects.equals(name, that.name) && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, repoName);
    }
}
