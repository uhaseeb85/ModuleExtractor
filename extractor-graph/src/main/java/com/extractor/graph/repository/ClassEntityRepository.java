package com.extractor.graph.repository;

import com.extractor.graph.entity.ClassEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data Neo4j repository for {@link ClassEntity} nodes.
 */
@Repository
public interface ClassEntityRepository extends Neo4jRepository<ClassEntity, Long> {

    Optional<ClassEntity> findByFullyQualifiedName(String fullyQualifiedName);

    List<ClassEntity> findByRepoName(String repoName);

    List<ClassEntity> findByPackageName(String packageName);

    List<ClassEntity> findByRepoNameAndPackageName(String repoName, String packageName);

    /**
     * Finds all classes that are imported by a class from a different repository.
     * Used to identify cross-repo IMPORTS edges for a given repo.
     */
    @Query("""
            MATCH (importer:Class)-[:IMPORTS]->(target:Class)
            WHERE importer.repoName <> target.repoName
              AND target.repoName = $repoName
            RETURN target
            """)
    List<ClassEntity> findClassesImportedCrossRepo(@Param("repoName") String repoName);

    /**
     * Finds all callers of a given class (classes that import or call methods on it),
     * optionally filtered to cross-repo only.
     *
     * @param fqn             Fully-qualified name of the target class.
     * @param crossRepoOnly   If {@code true}, returns only callers from different repositories.
     */
    @Query("""
            MATCH (caller:Class)-[:IMPORTS]->(target:Class {fullyQualifiedName: $fqn})
            WHERE NOT $crossRepoOnly OR caller.repoName <> target.repoName
            RETURN caller
            """)
    List<ClassEntity> findCallers(
            @Param("fqn") String fqn,
            @Param("crossRepoOnly") boolean crossRepoOnly);

    /**
     * Computes transitive impact of a class change up to a configurable depth.
     *
     * @param fqn   Fully-qualified name of the changed class.
     * @param depth Maximum traversal depth (typically 1-5).
     */
    @Query("""
            MATCH path = (c:Class {fullyQualifiedName: $fqn})<-[:IMPORTS*1..$depth]-(dependent:Class)
            RETURN DISTINCT dependent
            """)
    List<ClassEntity> findTransitiveImpact(
            @Param("fqn") String fqn,
            @Param("depth") int depth);

    /**
     * Returns all @Entity-annotated classes that are used (imported) by classes in
     * repositories other than their own — the "shared entity" antipattern.
     */
    @Query("""
            MATCH (c:Class)-[:ANNOTATED_WITH]->(ann:Class)
            WHERE ann.simpleName = 'Entity'
            WITH c
            MATCH (x:Class)-[:IMPORTS]->(c)
            WHERE x.repoName <> c.repoName
            RETURN DISTINCT c
            """)
    List<ClassEntity> findSharedEntityClasses();
}
