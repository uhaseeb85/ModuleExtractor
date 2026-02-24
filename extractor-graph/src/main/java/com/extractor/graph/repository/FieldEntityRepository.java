package com.extractor.graph.repository;

import com.extractor.graph.entity.FieldEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data Neo4j repository for {@link FieldEntity} nodes.
 */
@Repository
public interface FieldEntityRepository extends Neo4jRepository<FieldEntity, Long> {

    List<FieldEntity> findByRepoName(String repoName);

    @Query("""
            MATCH (c:Class {fullyQualifiedName: $classFqn})-[:CONTAINS]->(f:Field)
            RETURN f
            """)
    List<FieldEntity> findByClassFqn(@Param("classFqn") String classFqn);
}
