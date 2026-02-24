package com.extractor.graph.service;

import com.extractor.core.exceptions.GraphException;
import com.extractor.core.interfaces.GraphBuilder;
import com.extractor.core.model.AnnotationEdge;
import com.extractor.core.model.CallEdge;
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
import com.extractor.graph.repository.ArtifactEntityRepository;
import com.extractor.graph.repository.ClassEntityRepository;
import com.extractor.graph.repository.FieldEntityRepository;
import com.extractor.graph.repository.MethodEntityRepository;
import com.extractor.graph.repository.PackageEntityRepository;
import com.extractor.graph.repository.RepositoryEntityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Persists {@link ParseResult} objects to the Neo4j graph in batches.
 *
 * <p>Nodes are upserted (merged) using findBy methods before saving to keep the
 * operation idempotent. For large repos the caller should pass results in batches
 * using {@link #persistBatch} rather than calling {@link #persist} in a loop.
 */
@Service
public class GraphBuilderImpl implements GraphBuilder {

    private static final Logger log = LoggerFactory.getLogger(GraphBuilderImpl.class);

    private final RepositoryEntityRepository repoRepository;
    private final ArtifactEntityRepository artifactRepository;
    private final PackageEntityRepository packageRepository;
    private final ClassEntityRepository classRepository;
    private final MethodEntityRepository methodRepository;
    private final FieldEntityRepository fieldRepository;

    @Value("${extractor.graph.batch-size:100}")
    private int batchSize;

    public GraphBuilderImpl(RepositoryEntityRepository repoRepository,
                            ArtifactEntityRepository artifactRepository,
                            PackageEntityRepository packageRepository,
                            ClassEntityRepository classRepository,
                            MethodEntityRepository methodRepository,
                            FieldEntityRepository fieldRepository) {
        this.repoRepository = repoRepository;
        this.artifactRepository = artifactRepository;
        this.packageRepository = packageRepository;
        this.classRepository = classRepository;
        this.methodRepository = methodRepository;
        this.fieldRepository = fieldRepository;
    }

    @Override
    @Transactional
    public void persist(ParseResult result, RepoConfig repoConfig) throws GraphException {
        persistBatch(List.of(result), repoConfig);
    }

    @Override
    @Transactional
    public void persistBatch(List<ParseResult> results, RepoConfig repoConfig) throws GraphException {
        if (results.isEmpty()) return;
        try {
            // Ensure repository node exists
            RepositoryEntity repoEntity = repoRepository.findByName(repoConfig.name())
                    .orElseGet(() -> repoRepository.save(new RepositoryEntity(
                            repoConfig.name(), repoConfig.url(), repoConfig.branch(),
                            repoConfig.localPath(), repoConfig.buildTool())));

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

            log.debug("Persisted batch of {} parse results for repo '{}'", results.size(), repoConfig.name());
        } catch (Exception e) {
            throw new GraphException("Failed to persist batch for repo '" + repoConfig.name() + "': " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void persistDependencies(List<DependsOnEdge> edges) throws GraphException {
        if (edges.isEmpty()) return;
        try {
            for (DependsOnEdge edge : edges) {
                ArtifactEntity from = findOrCreateArtifact(edge.fromGroupId(), edge.fromArtifactId(),
                        edge.fromVersion(), "JAR", edge.repoName());
                ArtifactEntity to = findOrCreateArtifact(edge.toGroupId(), edge.toArtifactId(),
                        edge.toVersion(), "JAR", null);

                boolean alreadyLinked = from.getDependencies().stream()
                        .anyMatch(dep -> dep.getDependency().equals(to));
                if (!alreadyLinked) {
                    from.getDependencies().add(new ArtifactDependency(to, edge.scope(), edge.isTransitive()));
                    artifactRepository.save(from);
                }
            }
            log.debug("Persisted {} DEPENDS_ON edges", edges.size());
        } catch (Exception e) {
            throw new GraphException("Failed to persist dependency edges: " + e.getMessage(), e);
        }
    }

    // ── Private helpers ──────────────────────────────────────────────────

    private ClassEntity buildClassEntity(ParseResult result) {
        var cn = result.classNode();

        // Upsert the class node
        ClassEntity classEntity = classRepository.findByFullyQualifiedName(cn.fqn())
                .orElseGet(() -> new ClassEntity(
                        cn.fqn(), cn.simpleName(), cn.classType(),
                        cn.isAbstract(), cn.repoName(), cn.packageName(), cn.lineNumber()));

        cn.javadoc().ifPresent(classEntity::setJavadoc);

        // Ensure parent package exists
        PackageEntity pkg = packageRepository
                .findByFullyQualifiedNameAndRepoName(cn.packageName(), cn.repoName())
                .orElseGet(() -> packageRepository.save(
                        new PackageEntity(cn.packageName(), cn.repoName(), null)));

        // Build method entities
        List<MethodEntity> methodEntities = new ArrayList<>();
        for (MethodNode mn : result.methods()) {
            MethodEntity methodEntity = methodRepository
                    .findBySignatureAndRepoName(mn.signature(), mn.repoName())
                    .orElseGet(() -> new MethodEntity(
                            mn.signature(), mn.name(), mn.returnType(),
                            mn.visibility(), mn.isStatic(), mn.repoName(), mn.lineNumber()));
            mn.javadoc().ifPresent(methodEntity::setJavadoc);
            methodEntities.add(methodEntity);
        }
        classEntity.setMethods(methodEntities);

        // Build field entities
        List<FieldEntity> fieldEntities = new ArrayList<>();
        for (FieldNode fn : result.fields()) {
            String annotationsJson = "[\"" + String.join("\",\"", fn.annotations()) + "\"]";
            FieldEntity fieldEntity = new FieldEntity(
                    fn.name(), fn.fieldType(), fn.visibility(), annotationsJson, fn.repoName());
            fieldEntities.add(fieldEntity);
        }
        classEntity.setFields(fieldEntities);

        // Wire IMPORTS edges (lazy — targets may not be persisted yet; resolved in post-processing)
        List<ClassEntity> importedClasses = new ArrayList<>();
        for (ImportEdge edge : result.imports()) {
            if (edge.isWildcard()) continue; // skip wildcard imports
            classRepository.findByFullyQualifiedName(edge.importedFqn())
                    .ifPresent(importedClasses::add);
        }
        classEntity.setImports(importedClasses);

        return classEntity;
    }

    private void flushBatch(List<ClassEntity> batch) {
        classRepository.saveAll(batch);
        log.trace("Flushed {} class entities to Neo4j", batch.size());
        batch.clear();
    }

    private ArtifactEntity findOrCreateArtifact(String groupId, String artifactId,
                                                  String version, String type, String repoName) {
        return artifactRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version)
                .orElseGet(() -> artifactRepository.save(
                        new ArtifactEntity(groupId, artifactId, version, type, repoName)));
    }
}
