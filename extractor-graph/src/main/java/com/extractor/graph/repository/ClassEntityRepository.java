package com.extractor.graph.repository;

import com.extractor.graph.entity.ClassEntity;
import com.extractor.graph.store.GraphStore;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * In-memory facade replacing the Neo4j/Spring-Data ClassEntityRepository.
 * Delegates all storage and graph traversal operations to {@link GraphStore} / JGraphT.
 */
@Repository
public class ClassEntityRepository {

    private final GraphStore store;

    public ClassEntityRepository(GraphStore store) {
        this.store = store;
    }

    // ── CRUD ──────────────────────────────────────────────────────────

    public ClassEntity save(ClassEntity entity) {
        store.putClass(entity);
        store.ensureVertex(entity.getFullyQualifiedName());
        return entity;
    }

    public List<ClassEntity> saveAll(List<ClassEntity> entities) {
        for (ClassEntity e : entities) {
            store.putClass(e);
            store.ensureVertex(e.getFullyQualifiedName());
        }
        // Wire import edges after all vertices exist in the store
        for (ClassEntity e : entities) {
            for (ClassEntity target : e.getImports()) {
                store.addImportEdge(e.getFullyQualifiedName(), target.getFullyQualifiedName());
            }
        }
        return entities;
    }

    // ── Simple queries ─────────────────────────────────────────────────

    public Optional<ClassEntity> findByFullyQualifiedName(String fqn) {
        return store.findClassByFqn(fqn);
    }

    public List<ClassEntity> findByRepoName(String repoName) {
        return store.classesByRepo(repoName);
    }

    public List<ClassEntity> findByPackageName(String packageName) {
        return store.classesByPackage(packageName);
    }

    public List<ClassEntity> findByRepoNameAndPackageName(String repoName, String packageName) {
        return store.classesByRepoAndPackage(repoName, packageName);
    }

    /** Returns all class entities. */
    public List<ClassEntity> findAll() {
        return new ArrayList<>(store.allClasses());
    }

    /**
     * Returns a page of class entities (0-based page index).
     */
    public List<ClassEntity> findAll(int page, int size) {
        List<ClassEntity> all = findAll();
        int from = page * size;
        if (from >= all.size()) return Collections.emptyList();
        return all.subList(from, Math.min(from + size, all.size()));
    }

    // ── Cross-repo queries ─────────────────────────────────────────────

    /**
     * Returns all classes in {@code repoName} that are imported by classes from other repositories.
     */
    public List<ClassEntity> findClassesImportedCrossRepo(String repoName) {
        DefaultDirectedGraph<String, DefaultEdge> graph = store.importGraph();
        List<ClassEntity> result = new ArrayList<>();
        for (ClassEntity c : store.classesByRepo(repoName)) {
            String fqn = c.getFullyQualifiedName();
            if (!graph.containsVertex(fqn)) continue;
            boolean crossRepoImported = graph.incomingEdgesOf(fqn).stream().anyMatch(edge -> {
                ClassEntity importer = store.findClassByFqn(graph.getEdgeSource(edge)).orElse(null);
                return importer != null && !importer.getRepoName().equals(repoName);
            });
            if (crossRepoImported) result.add(c);
        }
        return result;
    }

    /**
     * Returns all classes that import the given class (direct callers).
     *
     * @param crossRepoOnly if {@code true}, returns only callers from a different repository.
     */
    public List<ClassEntity> findCallers(String fqn, boolean crossRepoOnly) {
        DefaultDirectedGraph<String, DefaultEdge> graph = store.importGraph();
        if (!graph.containsVertex(fqn)) return Collections.emptyList();

        ClassEntity target = store.findClassByFqn(fqn).orElse(null);
        List<ClassEntity> callers = new ArrayList<>();

        for (DefaultEdge edge : graph.incomingEdgesOf(fqn)) {
            String callerFqn = graph.getEdgeSource(edge);
            ClassEntity caller = store.findClassByFqn(callerFqn).orElse(null);
            if (caller == null) continue;
            if (crossRepoOnly && target != null && caller.getRepoName().equals(target.getRepoName())) {
                continue;
            }
            callers.add(caller);
        }
        return callers;
    }

    /**
     * Returns classes that transitively depend on the given class, up to {@code depth} hops.
     * BFS on the reversed IMPORTS graph.
     */
    public List<ClassEntity> findTransitiveImpact(String fqn, int depth) {
        DefaultDirectedGraph<String, DefaultEdge> graph = store.importGraph();
        if (!graph.containsVertex(fqn)) return Collections.emptyList();

        Set<String> visited = new LinkedHashSet<>();
        Queue<String> queue = new ArrayDeque<>();
        Map<String, Integer> depthMap = new HashMap<>();
        queue.add(fqn);
        depthMap.put(fqn, 0);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            int d = depthMap.get(current);
            if (!current.equals(fqn)) visited.add(current);
            if (d >= depth || !graph.containsVertex(current)) continue;
            for (DefaultEdge edge : graph.incomingEdgesOf(current)) {
                String caller = graph.getEdgeSource(edge);
                if (!depthMap.containsKey(caller)) {
                    depthMap.put(caller, d + 1);
                    queue.add(caller);
                }
            }
        }

        return visited.stream()
                .map(store::findClassByFqn)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Returns all @Entity-annotated classes imported cross-repo (the "shared entity" antipattern).
     */
    public List<ClassEntity> findSharedEntityClasses() {
        DefaultDirectedGraph<String, DefaultEdge> graph = store.importGraph();
        List<ClassEntity> result = new ArrayList<>();
        for (ClassEntity c : store.allClasses()) {
            boolean isEntity = c.getAnnotations().stream()
                    .anyMatch(ann -> "Entity".equals(ann.getSimpleName()));
            if (!isEntity) continue;
            String fqn = c.getFullyQualifiedName();
            if (!graph.containsVertex(fqn)) continue;
            boolean hasCrossRepoImporter = graph.incomingEdgesOf(fqn).stream().anyMatch(edge -> {
                ClassEntity imp = store.findClassByFqn(graph.getEdgeSource(edge)).orElse(null);
                return imp != null && !imp.getRepoName().equals(c.getRepoName());
            });
            if (hasCrossRepoImporter) result.add(c);
        }
        return result;
    }
}
