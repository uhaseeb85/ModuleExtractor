package com.extractor.ingestion.parser;

import com.extractor.core.enums.ClassType;
import com.extractor.core.exceptions.ParseException;
import com.extractor.core.interfaces.JavaSourceParser;
import com.extractor.core.model.AnnotationEdge;
import com.extractor.core.model.CallEdge;
import com.extractor.core.model.ClassNode;
import com.extractor.core.model.FieldNode;
import com.extractor.core.model.ImportEdge;
import com.extractor.core.model.MethodNode;
import com.extractor.core.model.ParseResult;
import com.extractor.core.model.RepoConfig;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JavaParser-backed implementation of {@link JavaSourceParser}.
 *
 * <p>Uses a {@link CombinedTypeSolver} composed of:
 * <ul>
 *   <li>{@link ReflectionTypeSolver} — resolves JDK types</li>
 *   <li>{@link JavaParserTypeSolver} — resolves types from the repo's source root</li>
 *   <li>{@link JarTypeSolver} — resolves third-party types from pre-resolved dependency JARs</li>
 * </ul>
 *
 * <p>Per spec §12.3: JARs <em>must</em> be resolved by the build parser before this class
 * is invoked. The caller (IngestionOrchestrator) is responsible for calling
 * {@link #registerSourceRoot} and {@link #registerJar} before parsing.
 */
@Component
public class JavaSourceParserImpl implements JavaSourceParser {

    private static final Logger log = LoggerFactory.getLogger(JavaSourceParserImpl.class);

    private final CombinedTypeSolver combinedTypeSolver;
    private JavaParser javaParser;

    public JavaSourceParserImpl() {
        this.combinedTypeSolver = new CombinedTypeSolver(new ReflectionTypeSolver());
        rebuildParser();
    }

    /**
     * Register a source root directory so cross-repo types can be resolved.
     *
     * @param sourceRoot Absolute path to the source root (typically {@code src/main/java}).
     */
    public void registerSourceRoot(Path sourceRoot) {
        if (sourceRoot.toFile().isDirectory()) {
            combinedTypeSolver.add(new JavaParserTypeSolver(sourceRoot.toFile()));
            rebuildParser();
            log.debug("Registered source root: {}", sourceRoot);
        }
    }

    /**
     * Register a dependency JAR so third-party and cross-repo compiled types can be resolved.
     *
     * @param jarPath Absolute path to the JAR file.
     */
    public void registerJar(Path jarPath) {
        if (jarPath.toFile().exists()) {
            try {
                combinedTypeSolver.add(new JarTypeSolver(jarPath.toFile()));
            } catch (IOException e) {
                log.warn("Could not add JarTypeSolver for '{}': {}", jarPath, e.getMessage());
            }
        }
    }

    @Override
    public ParseResult parse(Path javaFile, RepoConfig repo) throws ParseException {
        try {
            var parseResult = javaParser.parse(javaFile);

            if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
                String problems = parseResult.getProblems().toString();
                throw new ParseException("Parse failed for " + javaFile + ": " + problems, javaFile);
            }

            CompilationUnit cu = parseResult.getResult().get();
            return extractParseResult(cu, javaFile, repo);

        } catch (ParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ParseException("Unexpected error parsing " + javaFile, javaFile, e);
        }
    }

    // ── Private extraction methods ──────────────────────────────────────

    private ParseResult extractParseResult(CompilationUnit cu, Path javaFile, RepoConfig repo) {
        String packageName = cu.getPackageDeclaration()
                .map(pd -> pd.getNameAsString())
                .orElse("");

        // Find the primary type declaration
        TypeDeclaration<?> primaryType = cu.getPrimaryType().orElse(
                cu.getTypes().isEmpty() ? null : cu.getTypes().get(0));

        if (primaryType == null) {
            throw new ParseException("No type declaration found in " + javaFile, javaFile);
        }

        ClassType classType = resolveClassType(primaryType);
        boolean isAbstract = primaryType instanceof ClassOrInterfaceDeclaration c && c.isAbstract();
        String fqn = packageName.isEmpty()
                ? primaryType.getNameAsString()
                : packageName + "." + primaryType.getNameAsString();
        Optional<String> javadoc = primaryType.getJavadoc().map(j -> j.getDescription().toText());

        List<String> typeAnnotations = new ArrayList<>();
        for (AnnotationExpr ann : primaryType.getAnnotations()) {
            typeAnnotations.add(ann.getNameAsString());
        }

        // Methods
        List<MethodNode> methods = new ArrayList<>();
        for (MethodDeclaration md : primaryType.getMethods()) {
            String sig = buildMethodSignature(fqn, md);
            Optional<String> methodJavadoc = md.getJavadoc().map(j -> j.getDescription().toText());
            methods.add(new MethodNode(
                    sig, md.getNameAsString(), md.getTypeAsString(),
                    md.getAccessSpecifier().asString(), md.isStatic(),
                    repo.name(), md.getBegin().map(p -> p.line).orElse(0), methodJavadoc));
        }

        // Fields
        List<FieldNode> fields = new ArrayList<>();
        for (FieldDeclaration fd : primaryType.getFields()) {
            List<String> fieldAnnotations = fd.getAnnotations().stream()
                    .map(AnnotationExpr::getNameAsString)
                    .toList();
            String visibility = fd.getAccessSpecifier().asString();
            String fieldType = fd.getElementType().asString();
            fd.getVariables().forEach(v ->
                    fields.add(new FieldNode(v.getNameAsString(), fieldType, visibility, fieldAnnotations, repo.name())));
        }

        // Import edges
        List<ImportEdge> imports = new ArrayList<>();
        cu.getImports().forEach(importDecl -> {
            String importedFqn = importDecl.getNameAsString();
            imports.add(new ImportEdge(fqn, importedFqn,
                    importDecl.isStatic(), importDecl.isAsterisk()));
        });

        // Call edges (best-effort — only method calls that can be resolved)
        List<CallEdge> calls = extractCallEdges(cu, fqn, repo.name());

        // Annotation edges
        List<AnnotationEdge> annotations = new ArrayList<>();
        for (AnnotationExpr ann : primaryType.getAnnotations()) {
            String attributesJson = ann.toString().contains("(")
                    ? ann.toString().substring(ann.toString().indexOf('('))
                    : "{}";
            annotations.add(new AnnotationEdge(fqn, ann.getNameAsString(), attributesJson, "CLASS"));
        }

        ClassNode classNode = new ClassNode(fqn, primaryType.getNameAsString(), classType,
                isAbstract, repo.name(), javadoc, methods, fields, typeAnnotations, packageName,
                primaryType.getBegin().map(p -> p.line).orElse(0));

        return new ParseResult(classNode, methods, fields, imports, calls, annotations);
    }

    private List<CallEdge> extractCallEdges(CompilationUnit cu, String callerFqn, String repoName) {
        List<CallEdge> calls = new ArrayList<>();
        cu.findAll(MethodCallExpr.class).forEach(mce -> {
            try {
                var resolved = mce.resolve();
                String calleeFqn = resolved.declaringType().getQualifiedName();
                String calleeMethod = resolved.getName();
                String callerMethod = mce.findAncestor(MethodDeclaration.class)
                        .map(MethodDeclaration::getNameAsString)
                        .orElse("<init>");
                int lineNumber = mce.getBegin().map(p -> p.line).orElse(0);
                calls.add(new CallEdge(callerFqn, callerMethod, calleeFqn, calleeMethod,
                        "VIRTUAL", lineNumber));
            } catch (Exception e) {
                // Symbol resolution failures are expected for external/unresolved types; skip silently
            }
        });
        return calls;
    }

    private ClassType resolveClassType(TypeDeclaration<?> type) {
        if (type instanceof EnumDeclaration) return ClassType.ENUM;
        if (type instanceof ClassOrInterfaceDeclaration c) {
            if (c.isInterface()) return ClassType.INTERFACE;
            if (c.isAnnotationDeclaration()) return ClassType.ANNOTATION;
            return ClassType.CLASS;
        }
        if (type.getNameAsString().startsWith("record")) return ClassType.RECORD;
        return ClassType.CLASS;
    }

    private String buildMethodSignature(String classFqn, MethodDeclaration md) {
        String params = md.getParameters().stream()
                .map(p -> p.getTypeAsString())
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return classFqn + "#" + md.getNameAsString() + "(" + params + "):" + md.getTypeAsString();
    }

    private void rebuildParser() {
        ParserConfiguration config = new ParserConfiguration()
                .setSymbolResolver(new JavaSymbolSolver(combinedTypeSolver))
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE);
        this.javaParser = new JavaParser(config);
    }
}
