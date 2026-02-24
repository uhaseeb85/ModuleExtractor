package com.extractor.graph.service;

import com.extractor.core.enums.BuildTool;
import com.extractor.core.enums.ClassType;
import com.extractor.core.model.ClassNode;
import com.extractor.core.model.FieldNode;
import com.extractor.core.model.MethodNode;
import com.extractor.core.model.ParseResult;
import com.extractor.core.model.RepoConfig;
import com.extractor.graph.entity.ClassEntity;
import com.extractor.graph.repository.ClassEntityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for {@link GraphBuilderImpl} using a real Neo4j instance
 * via Testcontainers.
 */
@SpringBootTest(classes = com.extractor.graph.GraphTestApplication.class)
@Testcontainers
class GraphBuilderIntegrationTest {

    @Container
    static Neo4jContainer<?> neo4j = new Neo4jContainer<>("neo4j:5-community")
            .withoutAuthentication();

    @DynamicPropertySource
    static void neo4jProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4j::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "");
    }

    @Autowired
    GraphBuilderImpl graphBuilder;

    @Autowired
    ClassEntityRepository classRepository;

    private static RepoConfig repo(String name) {
        return new RepoConfig(name, "file://local", "main", BuildTool.MAVEN, Path.of("/tmp/" + name));
    }

    @Test
    void persistsClassAndMethods() {
        RepoConfig config = repo("test-repo");
        ClassNode classNode = new ClassNode(
                "com.example.Foo", "Foo", ClassType.CLASS, false,
                "test-repo", "Javadoc", List.of(), List.of(), List.of(),
                "com.example", 10);
        MethodNode method = new MethodNode("bar()", "bar", "void",
                "public", false, "test-repo", 15, "");
        ParseResult result = new ParseResult(classNode, List.of(method), List.of(),
                List.of(), List.of(), List.of());

        graphBuilder.persist(result, config);

        Optional<ClassEntity> found = classRepository.findByFullyQualifiedName("com.example.Foo");
        assertThat(found).isPresent();
        assertThat(found.get().getSimpleName()).isEqualTo("Foo");
        assertThat(found.get().getMethods()).hasSize(1);
    }

    @Test
    void persistsBatch() {
        RepoConfig config = repo("batch-repo");
        List<ParseResult> batch = List.of(
                new ParseResult(
                        new ClassNode("com.example.A", "A", ClassType.CLASS, false,
                                "batch-repo", "", List.of(), List.of(), List.of(),
                                "com.example", 1),
                        List.of(), List.of(), List.of(), List.of(), List.of()),
                new ParseResult(
                        new ClassNode("com.example.B", "B", ClassType.INTERFACE, false,
                                "batch-repo", "", List.of(), List.of(), List.of(),
                                "com.example", 2),
                        List.of(), List.of(), List.of(), List.of(), List.of())
        );

        graphBuilder.persistBatch(batch, config);

        List<ClassEntity> classes = classRepository.findByRepoName("batch-repo");
        assertThat(classes).hasSize(2);
        assertThat(classes.stream().map(ClassEntity::getSimpleName).toList())
                .containsExactlyInAnyOrder("A", "B");
    }
}
