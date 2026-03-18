package com.extractor.graph.repository;

import com.extractor.graph.entity.ArtifactEntity;
import com.extractor.graph.store.GraphStore;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * In-memory facade replacing the Neo4j/Spring-Data ArtifactEntityRepository.
 */
@Repository
public class ArtifactEntityRepository {

    private final GraphStore store;

    public ArtifactEntityRepository(GraphStore store) {
        this.store = store;
    }

    public ArtifactEntity save(ArtifactEntity entity) {
        store.putArtifact(entity);
        return entity;
    }

    public Optional<ArtifactEntity> findByGroupIdAndArtifactIdAndVersion(
            String groupId, String artifactId, String version) {
        return store.findArtifactByCoords(groupId, artifactId, version);
    }

    public List<ArtifactEntity> findByRepoName(String repoName) {
        return store.artifactsByRepo(repoName);
    }
}
