package com.extractor.core.model;

import java.util.Optional;

/**
 * Immutable representation of a method declared on a Java class.
 *
 * @param signature    Full method signature, e.g. {@code processPayment(PaymentRequest):PaymentResult}.
 * @param name         Simple method name.
 * @param returnType   Return type as a string, e.g. {@code void}, {@code java.util.List<String>}.
 * @param visibility   Visibility modifier: {@code public}, {@code protected}, {@code private}, or {@code package}.
 * @param isStatic     Whether the method is declared static.
 * @param repoName     Repository this method belongs to.
 * @param lineNumber   Line number of the method declaration.
 * @param javadoc      Javadoc comment, if present.
 */
public record MethodNode(
        String signature,
        String name,
        String returnType,
        String visibility,
        boolean isStatic,
        String repoName,
        int lineNumber,
        Optional<String> javadoc
) {
    public MethodNode {
        javadoc = javadoc == null ? Optional.empty() : javadoc;
    }
}
