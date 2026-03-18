package com.extractor.core.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a CO_CHANGES_WITH relationship derived from git commit history.
 */
public final class CoChangesEdge {

    private final String class1Fqn;
    private final String class2Fqn;
    private final int commitCount;
    private final Instant lastCommitDate;
    private final List<String> repos;

    public CoChangesEdge(String class1Fqn, String class2Fqn, int commitCount,
                         Instant lastCommitDate, List<String> repos) {
        this.class1Fqn = class1Fqn;
        this.class2Fqn = class2Fqn;
        this.commitCount = commitCount;
        this.lastCommitDate = lastCommitDate;
        this.repos = repos == null
                ? Collections.<String>emptyList()
                : Collections.unmodifiableList(new ArrayList<String>(repos));
    }

    public String getClass1Fqn() { return class1Fqn; }
    public String getClass2Fqn() { return class2Fqn; }
    public int getCommitCount() { return commitCount; }
    public Instant getLastCommitDate() { return lastCommitDate; }
    public List<String> getRepos() { return repos; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CoChangesEdge)) return false;
        CoChangesEdge that = (CoChangesEdge) o;
        return Objects.equals(class1Fqn, that.class1Fqn) && Objects.equals(class2Fqn, that.class2Fqn);
    }

    @Override
    public int hashCode() { return Objects.hash(class1Fqn, class2Fqn); }

    @Override
    public String toString() {
        return "CoChangesEdge{class1='" + class1Fqn + "', class2='" + class2Fqn + "', count=" + commitCount + "}";
    }
}
