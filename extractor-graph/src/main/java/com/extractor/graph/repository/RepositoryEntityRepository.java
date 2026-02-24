package com.extractor.graph.repository;

import com.extractor.graph.entity.RepositoryEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data Neo4j repository for {@link RepositoryEntity} nodes.
 */
@Repository
public interface RepositoryEntityRepository extends Neo4jRepository<RepositoryEntity, Long> {

    Optional<RepositoryEntity> findByName(String name);

    boolean existsByName(String name);
}
