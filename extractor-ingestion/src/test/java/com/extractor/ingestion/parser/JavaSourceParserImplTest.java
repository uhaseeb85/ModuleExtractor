package com.extractor.ingestion.parser;

import com.extractor.core.enums.BuildTool;
import com.extractor.core.enums.ClassType;
import com.extractor.core.model.ClassNode;
import com.extractor.core.model.ParseResult;
import com.extractor.core.model.RepoConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link JavaSourceParserImpl}.
 * Uses temporary source files — no Spring context required.
 */
class JavaSourceParserImplTest {

    @TempDir
    Path tempDir;

    JavaSourceParserImpl parser;
    RepoConfig repoConfig;

    @BeforeEach
    void setUp() {
        parser = new JavaSourceParserImpl();
        parser.registerSourceRoot(tempDir);
        repoConfig = new RepoConfig("test-repo", "file://local", "main", BuildTool.MAVEN, tempDir.toString());
    }

    private Path writeSource(String relativePath, String content) throws IOException {
        Path file = tempDir.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    // ── Tests ──────────────────────────────────────────────────────

    @Test
    void parsesSimpleClass() throws Exception {
        Path source = writeSource("com/example/Foo.java",
                "package com.example;\npublic class Foo {\n  public void bar() {}\n}\n");

        ParseResult result = parser.parse(source, repoConfig);

        ClassNode clazz = result.getClassNode();
        assertThat(clazz.getFqn()).isEqualTo("com.example.Foo");
        assertThat(clazz.getSimpleName()).isEqualTo("Foo");
        assertThat(clazz.getClassType()).isEqualTo(ClassType.CLASS);
        assertThat(clazz.isAbstract()).isFalse();
        assertThat(result.getMethods()).hasSize(1);
        assertThat(result.getMethods().get(0).getName()).isEqualTo("bar");
    }

    @Test
    void parsesInterface() throws Exception {
        Path source = writeSource("com/example/MyInterface.java",
                "package com.example;\npublic interface MyInterface {\n  void doSomething();\n}\n");

        ParseResult result = parser.parse(source, repoConfig);

        assertThat(result.getClassNode().getClassType()).isEqualTo(ClassType.INTERFACE);
        assertThat(result.getMethods()).hasSize(1);
    }

    @Test
    void parsesEnum() throws Exception {
        Path source = writeSource("com/example/Status.java",
                "package com.example;\npublic enum Status { ACTIVE, INACTIVE; }\n");

        ParseResult result = parser.parse(source, repoConfig);

        assertThat(result.getClassNode().getClassType()).isEqualTo(ClassType.ENUM);
    }

    @Test
    void parsesRecord() throws Exception {
        Path source = writeSource("com/example/Point.java",
                "package com.example;\npublic record Point(int x, int y) {}\n");

        ParseResult result = parser.parse(source, repoConfig);

        assertThat(result.getClassNode().getClassType()).isEqualTo(ClassType.RECORD);
    }

    @Test
    void parsesAnnotationType() throws Exception {
        Path source = writeSource("com/example/MyAnnotation.java",
                "package com.example;\nimport java.lang.annotation.*;\n@Retention(RetentionPolicy.RUNTIME)\npublic @interface MyAnnotation { String value() default \"\"; }\n");

        ParseResult result = parser.parse(source, repoConfig);

        assertThat(result.getClassNode().getClassType()).isEqualTo(ClassType.ANNOTATION);
    }

    @Test
    void extractsImportEdges() throws Exception {
        Path source = writeSource("com/example/Service.java",
                "package com.example;\nimport com.example.Foo;\nimport java.util.List;\npublic class Service {\n  Foo foo;\n  List<String> items;\n}\n");

        ParseResult result = parser.parse(source, repoConfig);

        List<String> imports = result.getImports().stream()
                .map(e -> e.getImportedFqn())
                .collect(Collectors.toList());
        assertThat(imports).contains("com.example.Foo");
    }

    @Test
    void extractsFields() throws Exception {
        Path source = writeSource("com/example/Widget.java",
                "package com.example;\npublic class Widget {\n  private String name;\n  private int count;\n}\n");

        ParseResult result = parser.parse(source, repoConfig);

        assertThat(result.getFields()).hasSize(2);
        assertThat(result.getFields().stream().map(f -> f.getName()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("name", "count");
    }

    @Test
    void extractsAnnotationEdges() throws Exception {
        Path source = writeSource("com/example/Controller.java",
                "package com.example;\nimport org.springframework.web.bind.annotation.RestController;\n@RestController\npublic class Controller {}\n");

        ParseResult result = parser.parse(source, repoConfig);

        assertThat(result.getAnnotations()).isNotEmpty();
        boolean hasRestController = result.getAnnotations().stream()
                .anyMatch(a -> a.getAnnotationFqn().contains("RestController"));
        assertThat(hasRestController).isTrue();
    }

    @Test
    void handlesInnerClass() throws Exception {
        Path source = writeSource("com/example/Outer.java",
                "package com.example;\npublic class Outer {\n  public static class Inner {\n    public void doWork() {}\n  }\n}\n");

        // Should not throw
        ParseResult result = parser.parse(source, repoConfig);
        assertThat(result.getClassNode().getSimpleName()).isEqualTo("Outer");
    }

    @Test
    void handlesGenerics() throws Exception {
        Path source = writeSource("com/example/Box.java",
                "package com.example;\npublic class Box<T> {\n  private T value;\n  public T get() { return value; }\n}\n");

        ParseResult result = parser.parse(source, repoConfig);
        assertThat(result.getClassNode().getFqn()).isEqualTo("com.example.Box");
        assertThat(result.getMethods()).hasSize(1);
    }
}
