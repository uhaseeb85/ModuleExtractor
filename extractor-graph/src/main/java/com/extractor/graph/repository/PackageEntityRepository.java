package com.extractor.graph.repository;

import com.extractor.graph.entity.ClassEntity;
import com.extractor.graph.entity.PackageEntity;
import com.extractor.graph.store.GraphStore;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory facade replacing the Neo4j/Spring-Data PackageEntityRepository.
 */
@Repository
public class PackageEntityRepository {

    private final GraphStore store;

    public PackageEntityRepository(GraphStore store) {
        this.store = store;
    }

    public PackageEntity save(PackageEntity entity) {
        store.putPackage(entity);
        return entity;
    }

    public Optional<PackageEntity> findByFullyQualifiedNameAndRepoName(String fqn, String repoName) {
        return store.findPackageByFqnAndRepo(fqn, repoName);
    }

    public List<PackageEntity> findByRepoName(String repoName) {
        return store.packagesByRepo(repoName);
    }

    /**
     * Returns packages that have no inbound cross-repo IMPORTS edges on any of their classes.
     * Implemented by iterating the JGraphT import graph.
     */
    public List<PackageEntity> findZeroInboundCrossRepoDependencyPackages() {
        DefaultDirectedGraph<String, DefaultEdge> graph = store.importGraph();
        List<PackageEntity> result = new ArrayList<>();

        for (PackageEntity pkg : store.allPackages()) {
            List<ClassEntity> classes = store.classesByRepoAndPackage(
                    pkg.getRepoName(), pkg.getFullyQualifiedName());
            boolean hasCrossRepoInbound = classes.stream().anyMatch(c -> {
                String fqn = c.getFullyQualifiedName();
                if (!graph.containsVertex(fqn)) return false;
                return graph.incomingEdgesOf(fqn).stream().anyMatch(edge -> {
                    ClassEntity importer = store.findClassByFqn(graph.getEdgeSource(edge)).orElse(null);
                    return importer != null && !importer.getRepoName().equals(c.getRepoName());
                });
            });
            if (!hasCrossRepoInbound) result.add(pkg);
        }
        return result;
    }
}
