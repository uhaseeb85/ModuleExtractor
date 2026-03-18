package com.extractor.graph.repository;

import com.extractor.graph.entity.FieldEntity;
import com.extractor.graph.store.GraphStore;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

/**
 * In-memory facade replacing the Neo4j/Spring-Data FieldEntityRepository.
 */
@Repository
public class FieldEntityRepository {

    private final GraphStore store;

    public FieldEntityRepository(GraphStore store) {
        this.store = store;
    }

    public List<FieldEntity> findByRepoName(String repoName) {
        // TODO: add repoName index to GraphStore if needed at scale
        return Collections.emptyList();
    }

    public List<FieldEntity> findByClassFqn(String classFqn) {
        return store.fieldsForClass(classFqn);
    }
}
