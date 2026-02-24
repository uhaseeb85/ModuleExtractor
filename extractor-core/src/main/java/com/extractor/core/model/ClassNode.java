package com.extractor.core.model;

import com.extractor.core.enums.ClassType;

import java.util.List;
import java.util.Optional;

/**
 * Immutable representation of a parsed Java class (or interface, enum, annotation, record).
 *
 * @param fqn          Fully-qualified class name, e.g. {@code com.example.billing.InvoiceService}.
 * @param simpleName   Simple (unqualified) class name.
 * @param classType    Classification of the type declaration.
 * @param isAbstract   Whether the type is declared abstract.
 * @param repoName     Name of the repository this class belongs to.
 * @param javadoc      Javadoc comment attached to the type declaration, if present.
 * @param methods      All methods declared directly on this type.
 * @param fields       All fields declared directly on this type.
 * @param annotations  Simple names of annotations applied to this type declaration.
 * @param packageName  Fully-qualified package name.
 * @param lineNumber   Line number of the class declaration in the source file.
 */
public record ClassNode(
        String fqn,
        String simpleName,
        ClassType classType,
        boolean isAbstract,
        String repoName,
        Optional<String> javadoc,
        List<MethodNode> methods,
        List<FieldNode> fields,
        List<String> annotations,
        String packageName,
        int lineNumber
) {
    public ClassNode {
        methods = List.copyOf(methods);
        fields = List.copyOf(fields);
        annotations = List.copyOf(annotations);
        javadoc = javadoc == null ? Optional.empty() : javadoc;
    }
}
