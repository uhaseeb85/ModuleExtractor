package com.extractor.graph.repository;

import com.extractor.graph.entity.ArtifactEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data Neo4j repository for {@link ArtifactEntity} nodes.
 */
@Repository
public interface ArtifactEntityRepository extends Neo4jRepository<ArtifactEntity, Long> {

    Optional<ArtifactEntity> findByGroupIdAndArtifactIdAndVersion(
            String groupId, String artifactId, String version);

    @Query("MATCH (a:Artifact {repoName: $repoName}) RETURN a")
    java.util.List<ArtifactEntity> findByRepoName(@Param("repoName") String repoName);
}
