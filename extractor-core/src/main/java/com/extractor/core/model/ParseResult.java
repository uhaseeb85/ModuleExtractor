package com.extractor.core.model;

import java.util.List;

/**
 * The result of parsing a single Java source file.
 * Produced by {@link com.extractor.core.interfaces.JavaSourceParser} and consumed by
 * {@link com.extractor.core.interfaces.GraphBuilder}.
 *
 * @param classNode   The primary class/interface/enum/record declared in the file.
 * @param methods     All methods extracted from the file (same as classNode.methods(), included for convenience).
 * @param fields      All fields extracted from the file.
 * @param imports     All static and non-static import edges from this file.
 * @param calls       All method call edges detected in this file.
 * @param annotations All annotation edges detected on types, methods, and fields.
 */
public record ParseResult(
        ClassNode classNode,
        List<MethodNode> methods,
        List<FieldNode> fields,
        List<ImportEdge> imports,
        List<CallEdge> calls,
        List<AnnotationEdge> annotations
) {
    public ParseResult {
        methods = methods == null ? List.of() : List.copyOf(methods);
        fields = fields == null ? List.of() : List.copyOf(fields);
        imports = imports == null ? List.of() : List.copyOf(imports);
        calls = calls == null ? List.of() : List.copyOf(calls);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }
}
