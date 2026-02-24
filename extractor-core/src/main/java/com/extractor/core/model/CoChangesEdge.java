package com.extractor.core.model;

import java.time.Instant;
import java.util.List;

/**
 * Represents a CO_CHANGES_WITH relationship derived from git commit history.
 *
 * @param class1Fqn     FQN of the first class in the co-change pair.
 * @param class2Fqn     FQN of the second class in the co-change pair.
 * @param commitCount   Number of commits in which both classes were modified together.
 * @param lastCommitDate Date of the most recent co-change commit.
 * @param repos         Names of repositories that contributed commits to this pair.
 */
public record CoChangesEdge(
        String class1Fqn,
        String class2Fqn,
        int commitCount,
        Instant lastCommitDate,
        List<String> repos
) {
    public CoChangesEdge {
        repos = repos == null ? List.of() : List.copyOf(repos);
    }
}
