package com.extractor.graph.repository;

import com.extractor.graph.entity.MethodEntity;
import com.extractor.graph.store.GraphStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * In-memory facade replacing the Neo4j/Spring-Data MethodEntityRepository.
 */
@Repository
public class MethodEntityRepository {

    private final GraphStore store;

    public MethodEntityRepository(GraphStore store) {
        this.store = store;
    }

    public MethodEntity save(MethodEntity entity) {
        // Methods are stored under their owning class — use putMethod(classFqn, method) for indexed storage.
        // This bare save is used when the class FQN is implicit (e.g. when method is in the entity list).
        return entity;
    }

    public Optional<MethodEntity> findBySignatureAndRepoName(String signature, String repoName) {
        return store.findMethodBySignatureAndRepo(signature, repoName);
    }

    public List<MethodEntity> findByRepoName(String repoName) {
        return store.methodsByRepo(repoName);
    }

    public List<MethodEntity> findByClassFqn(String classFqn) {
        return store.methodsForClass(classFqn);
    }
}
