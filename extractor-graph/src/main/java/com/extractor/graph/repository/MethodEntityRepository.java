package com.extractor.graph.repository;

import com.extractor.graph.entity.MethodEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data Neo4j repository for {@link MethodEntity} nodes.
 */
@Repository
public interface MethodEntityRepository extends Neo4jRepository<MethodEntity, Long> {

    Optional<MethodEntity> findBySignatureAndRepoName(String signature, String repoName);

    List<MethodEntity> findByRepoName(String repoName);

    @Query("""
            MATCH (c:Class {fullyQualifiedName: $classFqn})-[:CONTAINS]->(m:Method)
            RETURN m
            """)
    List<MethodEntity> findByClassFqn(@Param("classFqn") String classFqn);
}
