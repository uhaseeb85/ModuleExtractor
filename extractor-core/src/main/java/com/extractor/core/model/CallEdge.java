package com.extractor.core.model;

import java.util.Objects;

/**
 * Represents a CALLS relationship from one method to another.
 */
public final class CallEdge {

    private final String callerFqn;
    private final String callerMethod;
    private final String calleeFqn;
    private final String calleeMethod;
    private final String callType;
    private final int lineNumber;

    public CallEdge(String callerFqn, String callerMethod, String calleeFqn,
                    String calleeMethod, String callType, int lineNumber) {
        this.callerFqn = callerFqn;
        this.callerMethod = callerMethod;
        this.calleeFqn = calleeFqn;
        this.calleeMethod = calleeMethod;
        this.callType = callType;
        this.lineNumber = lineNumber;
    }

    public String getCallerFqn() { return callerFqn; }
    public String getCallerMethod() { return callerMethod; }
    public String getCalleeFqn() { return calleeFqn; }
    public String getCalleeMethod() { return calleeMethod; }
    public String getCallType() { return callType; }
    public int getLineNumber() { return lineNumber; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CallEdge)) return false;
        CallEdge that = (CallEdge) o;
        return lineNumber == that.lineNumber
                && Objects.equals(callerFqn, that.callerFqn)
                && Objects.equals(callerMethod, that.callerMethod)
                && Objects.equals(calleeFqn, that.calleeFqn)
                && Objects.equals(calleeMethod, that.calleeMethod);
    }

    @Override
    public int hashCode() { return Objects.hash(callerFqn, callerMethod, calleeFqn, calleeMethod, lineNumber); }

    @Override
    public String toString() {
        return "CallEdge{caller='" + callerFqn + "#" + callerMethod + "', callee='" + calleeFqn + "#" + calleeMethod + "'}";
    }
}
