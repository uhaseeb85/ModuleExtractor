package com.extractor.graph.service;

import com.extractor.core.enums.BuildTool;
import com.extractor.core.enums.ClassType;
import com.extractor.core.model.ClassNode;
import com.extractor.core.model.MethodNode;
import com.extractor.core.model.ParseResult;
import com.extractor.core.model.RepoConfig;
import com.extractor.graph.entity.ClassEntity;
import com.extractor.graph.repository.ClassEntityRepository;
import com.extractor.graph.store.GraphStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link GraphBuilderImpl} using the in-memory {@link GraphStore}.
 * No external infrastructure is required.
 */
class GraphBuilderIntegrationTest {

    private GraphStore store;
    private GraphBuilderImpl graphBuilder;
    private ClassEntityRepository classRepository;

    @BeforeEach
    void setUp() {
        store = new GraphStore();
        classRepository = new ClassEntityRepository(store);
        graphBuilder = new GraphBuilderImpl(store);
    }

    private static RepoConfig repo(String name) {
        return new RepoConfig(name, "file://local", "main", BuildTool.MAVEN, "/tmp/" + name);
    }

    @Test
    void persistsClassAndMethods() {
        RepoConfig config = repo("test-repo");
        ClassNode classNode = new ClassNode(
                "com.example.Foo", "Foo", ClassType.CLASS, false,
                "test-repo", Optional.of("Javadoc"), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                "com.example", 10);
        MethodNode method = new MethodNode("bar()", "bar", "void",
                "public", false, "test-repo", 15, Optional.<String>empty());
        ParseResult result = new ParseResult(classNode, Collections.singletonList(method), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        graphBuilder.persist(result, config);

        Optional<ClassEntity> found = classRepository.findByFullyQualifiedName("com.example.Foo");
        assertThat(found).isPresent();
        assertThat(found.get().getSimpleName()).isEqualTo("Foo");
        assertThat(found.get().getMethods()).hasSize(1);
    }

    @Test
    void persistsBatch() {
        RepoConfig config = repo("batch-repo");
        List<ParseResult> batch = Arrays.asList(
                new ParseResult(
                        new ClassNode("com.example.A", "A", ClassType.CLASS, false,
                                "batch-repo", Optional.<String>empty(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                                "com.example", 1),
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList()),
                new ParseResult(
                        new ClassNode("com.example.B", "B", ClassType.INTERFACE, false,
                                "batch-repo", Optional.<String>empty(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(),
                                "com.example", 2),
                        Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList())
        );

        graphBuilder.persistBatch(batch, config);

        List<ClassEntity> classes = classRepository.findByRepoName("batch-repo");
        assertThat(classes).hasSize(2);
        assertThat(classes.stream().map(ClassEntity::getSimpleName).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("A", "B");
    }
}

