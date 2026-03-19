package com.extractor.graph.store;

import com.extractor.graph.entity.ArtifactEntity;
import com.extractor.graph.entity.ClassEntity;
import com.extractor.graph.entity.FieldEntity;
import com.extractor.graph.entity.MethodEntity;
import com.extractor.graph.entity.PackageEntity;
import com.extractor.graph.entity.RepositoryEntity;
import com.extractor.graph.entity.SpringXmlConfigEntity;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory graph store backed by JGraphT.
 *
 * <p>Replaces Neo4j/Spring Data Neo4j as the persistence layer. All data lives in JVM
 * memory and is rebuilt from source on each application start / repository re-scan.
 *
 * <p>Thread-safety: Map operations use {@link ConcurrentHashMap}; JGraphT graph
 * mutations are synchronized on {@code this}.
 */
@Component
public class GraphStore {

    private static final Logger log = LoggerFactory.getLogger(GraphStore.class);

    // ── Entity indexes ──────────────────────────────────────────────────

    /** Canonical class index. Key = fullyQualifiedName. */
    private final Map<String, ClassEntity> classByFqn = new ConcurrentHashMap<>();

    /** Package index. Key = fullyQualifiedName + "::" + repoName. */
    private final Map<String, PackageEntity> packageByKey = new ConcurrentHashMap<>();

    /** Artifact index. Key = groupId + ":" + artifactId + ":" + version. */
    private final Map<String, ArtifactEntity> artifactByCoords = new ConcurrentHashMap<>();

    /** Repository index. Key = name. */
    private final Map<String, RepositoryEntity> repoByName = new ConcurrentHashMap<>();

    /** Method index. Key = signature + "::" + repoName. */
    private final Map<String, MethodEntity> methodByKey = new ConcurrentHashMap<>();

    /** Methods grouped by owning class FQN. */
    private final Map<String, List<MethodEntity>> methodsByClassFqn = new ConcurrentHashMap<>();

    /** Fields grouped by owning class FQN. */
    private final Map<String, List<FieldEntity>> fieldsByClassFqn = new ConcurrentHashMap<>();

    /** Spring XML config index. Key = filePath + "::" + repoName. */
    private final Map<String, SpringXmlConfigEntity> springXmlByKey = new ConcurrentHashMap<>();

    // ── JGraphT IMPORTS graph ───────────────────────────────────────────

    /**
     * Directed graph of IMPORTS edges between Java classes.
     * Vertices are fully-qualified class names (String).
     * An edge A → B means class A imports (depends on) class B.
     */
    private DefaultDirectedGraph<String, DefaultEdge> importGraph =
            new DefaultDirectedGraph<>(DefaultEdge.class);

    // ── Write operations ────────────────────────────────────────────────

    public void putClass(ClassEntity entity) {
        classByFqn.put(entity.getFullyQualifiedName(), entity);
    }

    public void putAllClasses(Collection<ClassEntity> entities) {
        for (ClassEntity e : entities) {
            putClass(e);
        }
    }

    public void putPackage(PackageEntity entity) {
        packageByKey.put(packageKey(entity.getFullyQualifiedName(), entity.getRepoName()), entity);
    }

    public void putArtifact(ArtifactEntity entity) {
        artifactByCoords.put(
                entity.getGroupId() + ":" + entity.getArtifactId() + ":" + entity.getVersion(),
                entity);
    }

    public void putRepo(RepositoryEntity entity) {
        repoByName.put(entity.getName(), entity);
    }

    public void putMethod(String classFqn, MethodEntity method) {
        methodByKey.put(method.getSignature() + "::" + method.getRepoName(), method);
        methodsByClassFqn.computeIfAbsent(classFqn, k -> new ArrayList<>()).add(method);
    }

    public void putField(String classFqn, FieldEntity field) {
        fieldsByClassFqn.computeIfAbsent(classFqn, k -> new ArrayList<>()).add(field);
    }

    public void putSpringXmlConfig(SpringXmlConfigEntity entity) {
        springXmlByKey.put(entity.getFilePath() + "::" + entity.getRepoName(), entity);
    }

    public List<SpringXmlConfigEntity> springXmlConfigsByRepo(String repoName) {
        return springXmlByKey.values().stream()
                .filter(e -> repoName.equals(e.getRepoName()))
                .collect(Collectors.toList());
    }

    /**
     * Adds a directed IMPORTS edge A → B to the JGraphT graph.
     * Automatically adds vertices if not already present.
     */
    public synchronized void addImportEdge(String fromFqn, String toFqn) {
        importGraph.addVertex(fromFqn);
        importGraph.addVertex(toFqn);
        if (!importGraph.containsEdge(fromFqn, toFqn)) {
            importGraph.addEdge(fromFqn, toFqn);
        }
    }

    /** Ensures a vertex exists in the import graph (even with no edges). */
    public synchronized void ensureVertex(String fqn) {
        importGraph.addVertex(fqn);
    }

    // ── Read operations ─────────────────────────────────────────────────

    public Optional<ClassEntity> findClassByFqn(String fqn) {
        return Optional.ofNullable(classByFqn.get(fqn));
    }

    public Collection<ClassEntity> allClasses() {
        return classByFqn.values();
    }

    public List<ClassEntity> classesByRepo(String repoName) {
        return classByFqn.values().stream()
                .filter(c -> repoName.equals(c.getRepoName()))
                .collect(Collectors.toList());
    }

    public List<ClassEntity> classesByPackage(String packageName) {
        return classByFqn.values().stream()
                .filter(c -> packageName.equals(c.getPackageName()))
                .collect(Collectors.toList());
    }

    public List<ClassEntity> classesByRepoAndPackage(String repoName, String packageName) {
        return classByFqn.values().stream()
                .filter(c -> repoName.equals(c.getRepoName()) && packageName.equals(c.getPackageName()))
                .collect(Collectors.toList());
    }

    public Optional<PackageEntity> findPackageByFqnAndRepo(String fqn, String repoName) {
        return Optional.ofNullable(packageByKey.get(packageKey(fqn, repoName)));
    }

    public List<PackageEntity> packagesByRepo(String repoName) {
        return packageByKey.values().stream()
                .filter(p -> repoName.equals(p.getRepoName()))
                .collect(Collectors.toList());
    }

    public Collection<PackageEntity> allPackages() {
        return packageByKey.values();
    }

    public Optional<ArtifactEntity> findArtifactByCoords(String groupId, String artifactId, String version) {
        return Optional.ofNullable(artifactByCoords.get(groupId + ":" + artifactId + ":" + version));
    }

    public List<ArtifactEntity> artifactsByRepo(String repoName) {
        return artifactByCoords.values().stream()
                .filter(a -> repoName.equals(a.getRepoName()))
                .collect(Collectors.toList());
    }

    public Optional<RepositoryEntity> findRepoByName(String name) {
        return Optional.ofNullable(repoByName.get(name));
    }

    public Collection<RepositoryEntity> allRepos() {
        return repoByName.values();
    }

    public Optional<MethodEntity> findMethodBySignatureAndRepo(String signature, String repoName) {
        return Optional.ofNullable(methodByKey.get(signature + "::" + repoName));
    }

    public List<MethodEntity> methodsByRepo(String repoName) {
        return methodByKey.values().stream()
                .filter(m -> repoName.equals(m.getRepoName()))
                .collect(Collectors.toList());
    }

    public List<MethodEntity> methodsForClass(String classFqn) {
        List<MethodEntity> result = methodsByClassFqn.get(classFqn);
        return result != null ? result : new ArrayList<>();
    }

    public List<FieldEntity> fieldsForClass(String classFqn) {
        List<FieldEntity> result = fieldsByClassFqn.get(classFqn);
        return result != null ? result : new ArrayList<>();
    }

    /** Returns the JGraphT IMPORTS graph. Structural mutations should use {@link #addImportEdge}. */
    public DefaultDirectedGraph<String, DefaultEdge> importGraph() {
        return importGraph;
    }

    // ── Clear / evict ──────────────────────────────────────────────────

    /**
     * Removes all data for a given repository and rebuilds the import graph.
     * Call before re-scanning a repository to avoid stale data.
     */
    public synchronized void clearRepo(String repoName) {
        log.info("Clearing graph store for repo '{}'", repoName);

        List<String> fqnsToRemove = classByFqn.values().stream()
                .filter(c -> repoName.equals(c.getRepoName()))
                .map(ClassEntity::getFullyQualifiedName)
                .collect(Collectors.toList());

        for (String fqn : fqnsToRemove) {
            classByFqn.remove(fqn);
            methodsByClassFqn.remove(fqn);
            fieldsByClassFqn.remove(fqn);
        }

        packageByKey.entrySet().removeIf(e -> repoName.equals(e.getValue().getRepoName()));
        artifactByCoords.entrySet().removeIf(e -> repoName.equals(e.getValue().getRepoName()));
        methodByKey.entrySet().removeIf(e -> repoName.equals(e.getValue().getRepoName()));
        springXmlByKey.entrySet().removeIf(e -> repoName.equals(e.getValue().getRepoName()));
        repoByName.remove(repoName);

        rebuildImportGraph();
    }

    /** Clears all data from the store. */
    public synchronized void clearAll() {
        classByFqn.clear();
        packageByKey.clear();
        artifactByCoords.clear();
        repoByName.clear();
        methodByKey.clear();
        methodsByClassFqn.clear();
        fieldsByClassFqn.clear();
        springXmlByKey.clear();
        importGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
    }

    /** Rebuilds the JGraphT import graph from the current state of {@link #classByFqn}. */
    private void rebuildImportGraph() {
        DefaultDirectedGraph<String, DefaultEdge> newGraph = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (ClassEntity c : classByFqn.values()) {
            newGraph.addVertex(c.getFullyQualifiedName());
            for (ClassEntity imported : c.getImports()) {
                if (classByFqn.containsKey(imported.getFullyQualifiedName())) {
                    newGraph.addVertex(imported.getFullyQualifiedName());
                    newGraph.addEdge(c.getFullyQualifiedName(), imported.getFullyQualifiedName());
                }
            }
        }
        importGraph = newGraph;
    }

    // ── Utilities ──────────────────────────────────────────────────────

    private static String packageKey(String fqn, String repoName) {
        return fqn + "::" + repoName;
    }
}
