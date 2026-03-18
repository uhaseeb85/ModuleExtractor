package com.extractor.graph.repository;

import com.extractor.graph.entity.RepositoryEntity;
import com.extractor.graph.store.GraphStore;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * In-memory facade replacing the Neo4j/Spring-Data RepositoryEntityRepository.
 */
@Repository
public class RepositoryEntityRepository {

    private final GraphStore store;

    public RepositoryEntityRepository(GraphStore store) {
        this.store = store;
    }

    public RepositoryEntity save(RepositoryEntity entity) {
        store.putRepo(entity);
        return entity;
    }

    public Optional<RepositoryEntity> findByName(String name) {
        return store.findRepoByName(name);
    }

    public boolean existsByName(String name) {
        return store.findRepoByName(name).isPresent();
    }
}
