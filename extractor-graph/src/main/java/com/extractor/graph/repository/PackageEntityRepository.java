package com.extractor.graph.repository;

import com.extractor.graph.entity.PackageEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data Neo4j repository for {@link PackageEntity} nodes.
 */
@Repository
public interface PackageEntityRepository extends Neo4jRepository<PackageEntity, Long> {

    Optional<PackageEntity> findByFullyQualifiedNameAndRepoName(String fullyQualifiedName, String repoName);

    List<PackageEntity> findByRepoName(String repoName);

    /**
     * Returns packages that have no inbound IMPORTS cross-repo edges.
     * These are strong extraction candidates.
     */
    @Query("""
            MATCH (p:Package)<-[:CONTAINS]-(c:Class)
            WHERE NOT EXISTS {
              MATCH (other:Class)-[:IMPORTS]->(c)
              WHERE other.repoName <> c.repoName
            }
            WITH p, count(c) AS classCount
            RETURN p, classCount
            ORDER BY classCount DESC
            """)
    List<PackageEntity> findZeroInboundCrossRepoDependencyPackages();
}
