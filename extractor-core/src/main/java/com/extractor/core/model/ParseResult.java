package com.extractor.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The result of parsing a single Java source file.
 * Produced by {@link com.extractor.core.interfaces.JavaSourceParser} and consumed by
 * {@link com.extractor.core.interfaces.GraphBuilder}.
 */
public final class ParseResult {

    private final ClassNode classNode;
    private final List<MethodNode> methods;
    private final List<FieldNode> fields;
    private final List<ImportEdge> imports;
    private final List<CallEdge> calls;
    private final List<AnnotationEdge> annotations;

    public ParseResult(ClassNode classNode, List<MethodNode> methods, List<FieldNode> fields,
                       List<ImportEdge> imports, List<CallEdge> calls, List<AnnotationEdge> annotations) {
        this.classNode   = classNode;
        this.methods     = safeUnmodifiable(methods);
        this.fields      = safeUnmodifiable(fields);
        this.imports     = safeUnmodifiable(imports);
        this.calls       = safeUnmodifiable(calls);
        this.annotations = safeUnmodifiable(annotations);
    }

    private static <T> List<T> safeUnmodifiable(List<T> list) {
        if (list == null) return Collections.emptyList();
        return Collections.unmodifiableList(new ArrayList<T>(list));
    }

    public ClassNode getClassNode() { return classNode; }
    public List<MethodNode> getMethods() { return methods; }
    public List<FieldNode> getFields() { return fields; }
    public List<ImportEdge> getImports() { return imports; }
    public List<CallEdge> getCalls() { return calls; }
    public List<AnnotationEdge> getAnnotations() { return annotations; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParseResult)) return false;
        ParseResult that = (ParseResult) o;
        return Objects.equals(classNode, that.classNode);
    }

    @Override
    public int hashCode() { return Objects.hash(classNode); }

    @Override
    public String toString() {
        return "ParseResult{classNode=" + classNode + "}";
    }
}
