package com.extractor.core.enums;

/**
 * Types of blockers that prevent clean module extraction.
 */
public enum BlockerType {
    /** A @Entity class used by classes both inside and outside the candidate boundary. */
    SHARED_ENTITY,

    /** Bidirectional CALLS or IMPORTS edges between candidate and external classes. */
    CIRCULAR_DEPENDENCY,

    /** Candidate directly references static methods of external concrete classes. */
    STATIC_UTILITY_COUPLING,

    /** A SpringBean has INJECTS relationships to both candidate and non-candidate classes. */
    SHARED_SPRING_BEAN,

    /** Candidate method has READS/WRITES to a Table owned by a different repository. */
    UNDECLARED_DB_OWNERSHIP,

    /** External classes call methods on concrete candidate classes rather than interfaces. */
    MISSING_INTERFACE_BOUNDARY
}
