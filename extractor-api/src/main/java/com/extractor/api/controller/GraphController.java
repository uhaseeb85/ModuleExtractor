package com.extractor.api.controller;

import com.extractor.api.dto.GraphEdgeResponse;
import com.extractor.api.dto.GraphNodeResponse;
import com.extractor.graph.entity.ClassEntity;
import com.extractor.graph.repository.ClassEntityRepository;
import com.extractor.graph.store.GraphStore;
import org.jgrapht.graph.DefaultEdge;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST controller exposing graph node and edge data for the frontend visualisation.
 *
 * <p>Base path: {@code /api/v1/graph}
 */
@RestController
@RequestMapping("/api/v1/graph")
public class GraphController {

    private final ClassEntityRepository classRepository;
    private final GraphStore store;

    public GraphController(ClassEntityRepository classRepository, GraphStore store) {
        this.classRepository = classRepository;
        this.store = store;
    }

    /**
     * GET /api/v1/graph/nodes
     * Returns all class nodes, optionally filtered by repo name and/or class type.
     *
     * @param repo  Optional repo name filter.
     * @param type  Optional class type filter (CLASS, INTERFACE, ENUM, ANNOTATION, RECORD).
     * @param page  Page number (0-based).
     * @param size  Page size.
     */
    @GetMapping("/nodes")
    public List<GraphNodeResponse> getNodes(
            @RequestParam(required = false) String repo,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "500") int size) {

        List<ClassEntity> entities;
        if (repo != null && !repo.trim().isEmpty()) {
            entities = classRepository.findByRepoName(repo);
        } else {
            // Fetch all with pagination via findAll
            entities = classRepository.findAll(page, size);
        }

        return entities.stream()
                .filter(e -> type == null || type.equalsIgnoreCase(e.getClassType()))
                .map(this::toNodeResponse)
                .collect(Collectors.toList());
    }

    /**
     * GET /api/v1/graph/node/{fqn}
     * Returns detailed information about a single class node, including its methods list.
     */
    @GetMapping("/node/{fqn}")
    public ResponseEntity<GraphNodeResponse> getNode(@PathVariable String fqn) {
        return classRepository.findByFullyQualifiedName(fqn)
                .map(e -> ResponseEntity.ok(toNodeResponse(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/v1/graph/edges
     * Returns IMPORTS edges between class nodes, optionally filtered.
     *
     * @param repo          Optional repo name — returns edges where either end is in this repo.
     * @param crossRepoOnly If {@code true}, returns only cross-repo IMPORTS edges.
     */
    @GetMapping("/edges")
    public List<GraphEdgeResponse> getEdges(
            @RequestParam(required = false) String repo,
            @RequestParam(defaultValue = "false") boolean crossRepoOnly) {

        // Build a lookup of FQN → entity for quick access
        List<ClassEntity> classes = repo != null && !repo.trim().isEmpty()
                ? classRepository.findByRepoName(repo)
                : classRepository.findAll();

        java.util.Map<String, ClassEntity> byFqn = classes.stream()
                .collect(Collectors.toMap(ClassEntity::getFullyQualifiedName, c -> c, (a, b) -> a));

        Set<String> scopedFqns = byFqn.keySet();

        List<GraphEdgeResponse> edges = new ArrayList<>();

        // Iterate over the JGraphT importGraph — the authoritative edge store
        for (DefaultEdge e : store.importGraph().edgeSet()) {
            String sourceFqn = store.importGraph().getEdgeSource(e);
            String targetFqn = store.importGraph().getEdgeTarget(e);

            // At least one end must be in the scoped set
            if (!scopedFqns.contains(sourceFqn) && !scopedFqns.contains(targetFqn)) continue;

            ClassEntity source = byFqn.get(sourceFqn);
            ClassEntity target = byFqn.get(targetFqn);

            boolean isCrossRepo = source != null && target != null
                    && !source.getRepoName().equals(target.getRepoName());
            if (crossRepoOnly && !isCrossRepo) continue;

            edges.add(new GraphEdgeResponse(
                    source != null ? source.getId() : null, sourceFqn,
                    target != null ? target.getId() : null, targetFqn,
                    "IMPORTS", isCrossRepo));
        }
        return edges;
    }

    /**
     * GET /api/v1/graph/callers?class={fqn}&crossRepoOnly={bool}
     * Returns all classes that import the given class.
     */
    @GetMapping("/callers")
    public List<GraphNodeResponse> getCallers(
            @RequestParam("class") String fqn,
            @RequestParam(defaultValue = "false") boolean crossRepoOnly) {
        return classRepository.findCallers(fqn, crossRepoOnly).stream()
                .map(this::toNodeResponse)
                .collect(Collectors.toList());
    }

    /**
     * GET /api/v1/graph/impact?class={fqn}&depth={n}
     * Returns the transitive impact of changing the given class.
     */
    @GetMapping("/impact")
    public List<GraphNodeResponse> getImpact(
            @RequestParam("class") String fqn,
            @RequestParam(defaultValue = "3") int depth) {
        return classRepository.findTransitiveImpact(fqn, depth).stream()
                .map(this::toNodeResponse)
                .collect(Collectors.toList());
    }

    /**
     * GET /api/v1/graph/shared-entities
     * Returns all @Entity classes that are imported cross-repo (the "shared entity" antipattern).
     */
    @GetMapping("/shared-entities")
    public List<GraphNodeResponse> getSharedEntities() {
        return classRepository.findSharedEntityClasses().stream()
                .map(this::toNodeResponse)
                .collect(Collectors.toList());
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private GraphNodeResponse toNodeResponse(ClassEntity e) {
        return new GraphNodeResponse(
                e.getId(),
                e.getFullyQualifiedName(),
                e.getSimpleName(),
                e.getClassType(),
                e.getRepoName(),
                e.getPackageName(),
                e.isAbstract(),
                e.getMethods() != null ? e.getMethods().size() : 0
        );
    }
}
