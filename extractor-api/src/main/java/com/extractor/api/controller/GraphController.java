package com.extractor.api.controller;

import com.extractor.api.dto.GraphEdgeResponse;
import com.extractor.api.dto.GraphNodeResponse;
import com.extractor.graph.entity.ClassEntity;
import com.extractor.graph.repository.ClassEntityRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

/**
 * REST controller exposing graph node and edge data for the frontend visualisation.
 *
 * <p>Base path: {@code /api/v1/graph}
 */
@RestController
@RequestMapping("/api/v1/graph")
public class GraphController {

    private final ClassEntityRepository classRepository;

    public GraphController(ClassEntityRepository classRepository) {
        this.classRepository = classRepository;
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
        if (repo != null && !repo.isBlank()) {
            entities = classRepository.findByRepoName(repo);
        } else {
            // Fetch all with pagination via findAll
            entities = classRepository.findAll(PageRequest.of(page, size)).getContent();
        }

        return entities.stream()
                .filter(e -> type == null || type.equalsIgnoreCase(e.getClassType()))
                .map(this::toNodeResponse)
                .toList();
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

        List<ClassEntity> classes = repo != null && !repo.isBlank()
                ? classRepository.findByRepoName(repo)
                : classRepository.findAll();

        List<GraphEdgeResponse> edges = new ArrayList<>();

        for (ClassEntity source : classes) {
            for (ClassEntity target : source.getImports()) {
                boolean isCrossRepo = !source.getRepoName().equals(target.getRepoName());
                if (crossRepoOnly && !isCrossRepo) continue;
                edges.add(new GraphEdgeResponse(
                        source.getId(), source.getFullyQualifiedName(),
                        target.getId(), target.getFullyQualifiedName(),
                        "IMPORTS", isCrossRepo));
            }
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
                .toList();
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
                .toList();
    }

    /**
     * GET /api/v1/graph/shared-entities
     * Returns all @Entity classes that are imported cross-repo (the "shared entity" antipattern).
     */
    @GetMapping("/shared-entities")
    public List<GraphNodeResponse> getSharedEntities() {
        return classRepository.findSharedEntityClasses().stream()
                .map(this::toNodeResponse)
                .toList();
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
