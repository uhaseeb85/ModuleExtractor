package com.extractor.core.model;

/**
 * Represents a CALLS relationship from one method to another.
 *
 * @param callerFqn    FQN of the class containing the calling method.
 * @param callerMethod Signature of the calling method.
 * @param calleeFqn    FQN of the class containing the method being called.
 * @param calleeMethod Signature of the method being called.
 * @param callType     Type of call: STATIC, VIRTUAL, or INTERFACE.
 * @param lineNumber   Source line number of the call site.
 */
public record CallEdge(
        String callerFqn,
        String callerMethod,
        String calleeFqn,
        String calleeMethod,
        String callType,
        int lineNumber
) {}
