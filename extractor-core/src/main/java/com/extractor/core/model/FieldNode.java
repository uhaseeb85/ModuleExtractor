package com.extractor.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable representation of a field declared on a Java class.
 */
public final class FieldNode {

    private final String name;
    private final String fieldType;
    private final String visibility;
    private final List<String> annotations;
    private final String repoName;

    public FieldNode(String name, String fieldType, String visibility,
                     List<String> annotations, String repoName) {
        this.name = name;
        this.fieldType = fieldType;
        this.visibility = visibility;
        this.annotations = annotations == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(annotations));
        this.repoName = repoName;
    }

    public String getName() { return name; }
    public String getFieldType() { return fieldType; }
    public String getVisibility() { return visibility; }
    public List<String> getAnnotations() { return annotations; }
    public String getRepoName() { return repoName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FieldNode)) return false;
        FieldNode that = (FieldNode) o;
        return Objects.equals(name, that.name) && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() { return Objects.hash(name, repoName); }

    @Override
    public String toString() {
        return "FieldNode{name='" + name + "', repo='" + repoName + "'}";
    }
}
