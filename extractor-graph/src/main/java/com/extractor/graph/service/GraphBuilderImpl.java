package com.extractor.graph.service;

import com.extractor.core.exceptions.GraphException;
import com.extractor.core.interfaces.GraphBuilder;
import com.extractor.core.model.AnnotationEdge;
import com.extractor.core.model.CallEdge;
import com.extractor.core.model.ClassNode;
import com.extractor.core.model.DependsOnEdge;
import com.extractor.core.model.FieldNode;
import com.extractor.core.model.ImportEdge;
import com.extractor.core.model.MethodNode;
import com.extractor.core.model.ParseResult;
import com.extractor.core.model.RepoConfig;
import com.extractor.graph.entity.ArtifactDependency;
import com.extractor.graph.entity.ArtifactEntity;
import com.extractor.graph.entity.ClassEntity;
import com.extractor.graph.entity.FieldEntity;
import com.extractor.graph.entity.MethodEntity;
import com.extractor.graph.entity.PackageEntity;
import com.extractor.graph.entity.RepositoryEntity;
import com.extractor.graph.store.GraphStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persists {@link ParseResult} objects into the in-memory {@link GraphStore}.
 *
 * <p>All data lives in JVM memory. Call {@link GraphStore#clearRepo(String)} before re-scanning
 * a repository to avoid stale entries.
 *
 * <p>Processing is two-pass per batch:
 * <ol>
 *   <li>First pass — add all {@link ClassEntity} vertices to the store.</li>
 *   <li>Second pass — resolve import edges now that all vertices are present.</li>
 * </ol>
 */
@Service
public class GraphBuilderImpl implements GraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(GraphBuilderImpl.class);

    private final GraphStore store;

    @Value("${extractor.graph.batch-size:100}")
    private int batchSize;

    public GraphBuilderImpl(GraphStore store) {
        this.store = store;
    }

    @Override
    public void persist(ParseResult result, RepoConfig repoConfig) throws GraphException {
        persistBatch(Collections.singletonList(result), repoConfig);
    }

    @Override
    public void persistBatch(List<ParseResult> results, RepoConfig repoConfig) throws GraphException {
        if (results.isEmpty()) return;
        try {
            // Ensure repository node exists
            store.findRepoByName(repoConfig.getName()).orElseGet(() -> {
                RepositoryEntity repo = new RepositoryEntity(
                        repoConfig.getName(), repoConfig.getUrl(), repoConfig.getBranch(),
                        repoConfig.getLocalPath(), repoConfig.getBuildTool());
                store.putRepo(repo);
                return repo;
            });

            // First pass: create ClassEntity objects and add to store
            List<ClassEntity> batch = new ArrayList<>(batchSize);
            for (ParseResult result : results) {
                ClassEntity classEntity = buildClassEntity(result);
                batch.add(classEntity);
                if (batch.size() >= batchSize) {
                    flushBatch(batch);
                }
            }
            if (!batch.isEmpty()) {
                flushBatch(batch);
            }

            // Second pass: resolve IMPORTS edges now all vertices exist
            for (ParseResult result : results) {
                String fromFqn = result.getClassNode().getFqn();
                for (ImportEdge edge : result.getImports()) {
                    if (edge.isWildcard()) continue;
                    String toFqn = edge.getImportedFqn();
                    if (store.findClassByFqn(toFqn).isPresent()) {
                        store.addImportEdge(fromFqn, toFqn);
                    }
                }
            }

            log.debug("Persisted batch of {} parse results for repo '{}'", results.size(), repoConfig.getName());
        } catch (Exception e) {
            throw new GraphException(
                    "Failed to persist batch for repo '" + repoConfig.getName() + "': " + e.getMessage(), e);
        }
    }

    @Override
    public void persistDependencies(List<DependsOnEdge> edges) throws GraphException {
        if (edges.isEmpty()) return;
        try {
            for (DependsOnEdge edge : edges) {
                ArtifactEntity from = findOrCreateArtifact(
                        edge.getFromGroupId(), edge.getFromArtifactId(), edge.getFromVersion(), "JAR", edge.getRepoName());
                ArtifactEntity to = findOrCreateArtifact(
                        edge.getToGroupId(), edge.getToArtifactId(), edge.getToVersion(), "JAR", null);

                boolean alreadyLinked = from.getDependencies().stream()
                        .anyMatch(dep -> dep.getDependency().equals(to));
                if (!alreadyLinked) {
                    from.getDependencies().add(new ArtifactDependency(to, edge.getScope(), edge.isTransitive()));
                    store.putArtifact(from);
                }
            }
            log.debug("Persisted {} DEPENDS_ON edges", edges.size());
        } catch (Exception e) {
            throw new GraphException("Failed to persist dependency edges: " + e.getMessage(), e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private ClassEntity buildClassEntity(ParseResult result) {
        ClassNode cn = result.getClassNode();

        // Upsert (or reuse existing) class entity
        ClassEntity classEntity = store.findClassByFqn(cn.getFqn())
                .orElseGet(() -> new ClassEntity(
                        cn.getFqn(), cn.getSimpleName(), cn.getClassType(),
                        cn.isAbstract(), cn.getRepoName(), cn.getPackageName(), cn.getLineNumber()));
        cn.getJavadoc().ifPresent(classEntity::setJavadoc);

        // Ensure parent package exists
        store.findPackageByFqnAndRepo(cn.getPackageName(), cn.getRepoName())
                .orElseGet(() -> {
                    PackageEntity pkg = new PackageEntity(cn.getPackageName(), cn.getRepoName(), null);
                    store.putPackage(pkg);
                    return pkg;
                });

        // Build and store method entities
        List<MethodEntity> methodEntities = new ArrayList<>();
        for (MethodNode mn : result.getMethods()) {
            MethodEntity methodEntity = store.findMethodBySignatureAndRepo(mn.getSignature(), mn.getRepoName())
                    .orElseGet(() -> new MethodEntity(
                            mn.getSignature(), mn.getName(), mn.getReturnType(),
                            mn.getVisibility(), mn.isStatic(), mn.getRepoName(), mn.getLineNumber()));
            mn.getJavadoc().ifPresent(methodEntity::setJavadoc);
            store.putMethod(cn.getFqn(), methodEntity);
            methodEntities.add(methodEntity);
        }
        classEntity.setMethods(methodEntities);

        // Build and store field entities
        List<FieldEntity> fieldEntities = new ArrayList<>();
        for (FieldNode fn : result.getFields()) {
            String annotationsJson = "[\"" + String.join("\",\"", fn.getAnnotations()) + "\"]";
            FieldEntity fieldEntity = new FieldEntity(
                    fn.getName(), fn.getFieldType(), fn.getVisibility(), annotationsJson, fn.getRepoName());
            store.putField(cn.getFqn(), fieldEntity);
            fieldEntities.add(fieldEntity);
        }
        classEntity.setFields(fieldEntities);

        // Note: IMPORTS edges are not wired here; they are resolved in the second pass in persistBatch.
        return classEntity;
    }

    private void flushBatch(List<ClassEntity> batch) {
        for (ClassEntity e : batch) {
            store.putClass(e);
            store.ensureVertex(e.getFullyQualifiedName());
        }
        log.trace("Flushed {} class entities to in-memory store", batch.size());
        batch.clear();
    }

    private ArtifactEntity findOrCreateArtifact(
            String groupId, String artifactId, String version, String type, String repoName) {
        return store.findArtifactByCoords(groupId, artifactId, version)
                .orElseGet(() -> {
                    ArtifactEntity a = new ArtifactEntity(groupId, artifactId, version, type, repoName);
                    store.putArtifact(a);
                    return a;
                });
    }
}


