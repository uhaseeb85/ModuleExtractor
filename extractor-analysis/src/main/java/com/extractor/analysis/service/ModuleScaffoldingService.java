package com.extractor.analysis.service;

import com.extractor.analysis.model.ProjectTreeNode;
import com.extractor.analysis.model.ProjectTreeNode.NodeType;
import com.extractor.analysis.model.ScaffoldResult;
import com.extractor.graph.entity.ArtifactDependency;
import com.extractor.graph.entity.ArtifactEntity;
import com.extractor.graph.entity.ClassEntity;
import com.extractor.graph.entity.SpringXmlConfigEntity;
import com.extractor.graph.entity.SpringXmlConfigEntity.BeanDefinition;
import com.extractor.graph.store.GraphStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates a Maven project scaffold for a proposed module extraction.
 *
 * <p>Given a {@link CandidateScoringService.ModuleRecommendation}, this service builds
 * a complete {@link ProjectTreeNode} tree representing the new Maven module, including:
 * <ul>
 *   <li>Standard Maven directory layout ({@code src/main/java}, {@code src/main/resources}, {@code src/test/java})</li>
 *   <li>Generated {@code pom.xml} with dependencies</li>
 *   <li>Java source file references (as MOVE markers from original locations)</li>
 *   <li>Filtered Spring XML context files with only the relevant bean definitions</li>
 * </ul>
 */
@Service
public class ModuleScaffoldingService {

    private static final Logger log = LoggerFactory.getLogger(ModuleScaffoldingService.class);

    private final GraphStore store;
    private final CandidateScoringService scoringService;

    public ModuleScaffoldingService(GraphStore store, CandidateScoringService scoringService) {
        this.store = store;
        this.scoringService = scoringService;
    }

    /**
     * Generates a full scaffold for the given module recommendation.
     *
     * @param moduleName        e.g. "auth"
     * @param modulePackageRoot e.g. "com.bank.ivr.auth"
     * @param repoName          source repository name
     * @param packages          list of packages included in this module
     * @return scaffold result with full project tree
     */
    public ScaffoldResult scaffold(String moduleName, String modulePackageRoot,
                                   String repoName, List<String> packages) {

        ProjectTreeNode root = new ProjectTreeNode(moduleName, moduleName, NodeType.DIRECTORY);

        // Standard Maven layout
        ProjectTreeNode srcMainJava = root.mkdirs("src/main/java");
        ProjectTreeNode srcMainResources = root.mkdirs("src/main/resources");
        ProjectTreeNode srcTestJava = root.mkdirs("src/test/java");

        // Collect classes in this module
        Set<String> pkgSet = new HashSet<>(packages);
        List<ClassEntity> moduleClasses = store.allClasses().stream()
                .filter(c -> repoName.equals(c.getRepoName()) && pkgSet.contains(c.getPackageName()))
                .collect(Collectors.toList());

        // Group classes by package
        Map<String, List<ClassEntity>> classesByPkg = moduleClasses.stream()
                .collect(Collectors.groupingBy(ClassEntity::getPackageName));

        int fileCount = 0;

        // Add Java source files organized by package
        for (Map.Entry<String, List<ClassEntity>> entry : classesByPkg.entrySet()) {
            String pkg = entry.getKey();
            String pkgPath = pkg.replace('.', '/');
            ProjectTreeNode pkgDir = srcMainJava.mkdirs(pkgPath);

            for (ClassEntity cls : entry.getValue()) {
                String fileName = (cls.getSimpleName() != null ? cls.getSimpleName() : cls.getFullyQualifiedName()) + ".java";
                ProjectTreeNode fileNode = new ProjectTreeNode(fileName, pkgDir.getPath() + "/" + fileName, NodeType.FILE);
                fileNode.setSourceRef(cls.getFullyQualifiedName());

                // Read real source file content from disk when available
                if (cls.getSourceFilePath() != null) {
                    try {
                        Path srcPath = Path.of(cls.getSourceFilePath());
                        if (Files.isReadable(srcPath)) {
                            fileNode.setContent(Files.readString(srcPath, StandardCharsets.UTF_8));
                        }
                    } catch (IOException e) {
                        log.warn("Could not read source file '{}': {}", cls.getSourceFilePath(), e.getMessage());
                    }
                }

                pkgDir.addChild(fileNode);
                fileCount++;
            }
        }

        // Generate pom.xml
        String pomContent = generatePom(moduleName, modulePackageRoot, repoName, moduleClasses);
        ProjectTreeNode pomNode = new ProjectTreeNode("pom.xml", root.getPath() + "/pom.xml", NodeType.FILE);
        pomNode.setContent(pomContent);
        root.addChild(pomNode);
        fileCount++;

        // Handle Spring XML context files
        List<String> springContextPaths = new ArrayList<>();
        List<SpringXmlConfigEntity> repoConfigs = store.springXmlConfigsByRepo(repoName);
        for (SpringXmlConfigEntity xmlConfig : repoConfigs) {
            String filteredXml = filterSpringXml(xmlConfig, pkgSet, moduleClasses);
            if (filteredXml != null) {
                String xmlFileName = extractFileName(xmlConfig.getFilePath());
                ProjectTreeNode xmlNode = new ProjectTreeNode(xmlFileName,
                        srcMainResources.getPath() + "/" + xmlFileName, NodeType.FILE);
                xmlNode.setContent(filteredXml);
                srcMainResources.addChild(xmlNode);
                springContextPaths.add(xmlConfig.getFilePath());
                fileCount++;
            }
        }

        return new ScaffoldResult(moduleName, modulePackageRoot, repoName,
                root, springContextPaths, fileCount);
    }

    /**
     * Returns the file content for a specific path in a scaffold preview.
     */
    public String getFileContent(ScaffoldResult result, String filePath) {
        ProjectTreeNode node = findNode(result.getTree(), filePath);
        if (node == null) return null;
        if (node.getContent() != null) return node.getContent();
        if (node.getSourceRef() != null) {
            return "// Source: " + node.getSourceRef() + "\n// This file will be moved from the original repository.";
        }
        return null;
    }

    // ── pom.xml generation ──────────────────────────────────────────────

    private String generatePom(String moduleName, String modulePackageRoot,
                               String repoName, List<ClassEntity> classes) {
        // Try to find the parent artifact for this repo
        List<ArtifactEntity> artifacts = store.artifactsByRepo(repoName);
        String parentGroupId = "com.example";
        String parentArtifactId = repoName;
        String parentVersion = "1.0.0-SNAPSHOT";

        ArtifactEntity repoArtifact = null;
        if (!artifacts.isEmpty()) {
            repoArtifact = artifacts.get(0);
            parentGroupId = repoArtifact.getGroupId() != null ? repoArtifact.getGroupId() : parentGroupId;
            parentVersion = repoArtifact.getVersion() != null ? repoArtifact.getVersion() : parentVersion;
        }

        // Collect all external import FQNs (outside this module)
        Set<String> moduleFqns = classes.stream()
                .map(ClassEntity::getFullyQualifiedName)
                .collect(Collectors.toSet());

        Set<String> externalImportFqns = new TreeSet<>();
        for (ClassEntity cls : classes) {
            for (ClassEntity imp : cls.getImports()) {
                if (!moduleFqns.contains(imp.getFullyQualifiedName())
                        && imp.getPackageName() != null
                        && !imp.getPackageName().startsWith("java.")
                        && !imp.getPackageName().startsWith("javax.")) {
                    externalImportFqns.add(imp.getFullyQualifiedName());
                }
            }
        }

        // Resolve real Maven dependencies from the artifact's dependency list
        // Match by checking if any external import FQN starts with the dependency's groupId
        List<ArtifactDependency> resolvedDeps = new ArrayList<>();
        Set<String> unmatchedPackages = new TreeSet<>();

        if (repoArtifact != null && repoArtifact.getDependencies() != null) {
            // Build a set of all unique dependency GAVs already matched to avoid duplicates
            Set<String> matchedGavs = new LinkedHashSet<>();

            for (String importFqn : externalImportFqns) {
                boolean matched = false;
                for (ArtifactDependency dep : repoArtifact.getDependencies()) {
                    ArtifactEntity target = dep.getDependency();
                    String gav = target.getGroupId() + ":" + target.getArtifactId();
                    // Match: import FQN starts with the groupId (convention for most Java libraries)
                    if (importFqn.startsWith(target.getGroupId() + ".")) {
                        if (matchedGavs.add(gav)) {
                            resolvedDeps.add(dep);
                        }
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    // Extract package from FQN for unmatched imports
                    int lastDot = importFqn.lastIndexOf('.');
                    if (lastDot > 0) {
                        unmatchedPackages.add(importFqn.substring(0, lastDot));
                    }
                }
            }
        } else {
            // No artifact data — fall back to listing external packages
            for (String fqn : externalImportFqns) {
                int lastDot = fqn.lastIndexOf('.');
                if (lastDot > 0) unmatchedPackages.add(fqn.substring(0, lastDot));
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n");
        sb.append("         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        sb.append("         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n");
        sb.append("    <modelVersion>4.0.0</modelVersion>\n\n");

        sb.append("    <parent>\n");
        sb.append("        <groupId>").append(escapeXml(parentGroupId)).append("</groupId>\n");
        sb.append("        <artifactId>").append(escapeXml(parentArtifactId)).append("</artifactId>\n");
        sb.append("        <version>").append(escapeXml(parentVersion)).append("</version>\n");
        sb.append("    </parent>\n\n");

        sb.append("    <artifactId>").append(escapeXml(moduleName)).append("</artifactId>\n");
        sb.append("    <name>").append(escapeXml(moduleName)).append("</name>\n");
        sb.append("    <description>Extracted module for ").append(escapeXml(modulePackageRoot)).append("</description>\n\n");

        sb.append("    <dependencies>\n");

        // Emit real resolved dependencies
        for (ArtifactDependency dep : resolvedDeps) {
            ArtifactEntity target = dep.getDependency();
            sb.append("        <dependency>\n");
            sb.append("            <groupId>").append(escapeXml(target.getGroupId())).append("</groupId>\n");
            sb.append("            <artifactId>").append(escapeXml(target.getArtifactId())).append("</artifactId>\n");
            if (target.getVersion() != null && !target.getVersion().isEmpty()) {
                sb.append("            <version>").append(escapeXml(target.getVersion())).append("</version>\n");
            }
            if (dep.getScope() != null && !"compile".equals(dep.getScope())) {
                sb.append("            <scope>").append(escapeXml(dep.getScope())).append("</scope>\n");
            }
            sb.append("        </dependency>\n");
        }

        // List any unmatched external imports as TODO comments
        if (!unmatchedPackages.isEmpty()) {
            sb.append("        <!-- TODO: Resolve dependencies for the following external imports -->\n");
            for (String pkg : unmatchedPackages) {
                sb.append("        <!-- Unresolved import: ").append(escapeXml(pkg)).append(" -->\n");
            }
        }

        sb.append("    </dependencies>\n");
        sb.append("</project>\n");

        return sb.toString();
    }

    // ── Spring XML filtering ────────────────────────────────────────────

    private String filterSpringXml(SpringXmlConfigEntity xmlConfig, Set<String> packages,
                                   List<ClassEntity> classes) {
        Set<String> classFqns = classes.stream()
                .map(ClassEntity::getFullyQualifiedName)
                .collect(Collectors.toSet());

        // Filter beans: keep only those whose class belongs to this module
        List<BeanDefinition> relevantBeans = xmlConfig.getBeanDefinitions().stream()
                .filter(b -> classFqns.contains(b.getClassFqn())
                        || packages.stream().anyMatch(p -> b.getClassFqn().startsWith(p + ".")))
                .collect(Collectors.toList());

        // Filter component-scan: keep only scans targeting this module's packages
        List<String> relevantScans = xmlConfig.getComponentScanPackages().stream()
                .filter(scan -> packages.stream().anyMatch(p -> p.startsWith(scan) || scan.startsWith(p)))
                .collect(Collectors.toList());

        if (relevantBeans.isEmpty() && relevantScans.isEmpty()) {
            return null; // Nothing relevant in this config
        }

        // Generate filtered XML
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<beans xmlns=\"http://www.springframework.org/schema/beans\"\n");
        sb.append("       xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n");
        if (!relevantScans.isEmpty()) {
            sb.append("       xmlns:context=\"http://www.springframework.org/schema/context\"\n");
        }
        sb.append("       xsi:schemaLocation=\"http://www.springframework.org/schema/beans\n");
        sb.append("       http://www.springframework.org/schema/beans/spring-beans.xsd");
        if (!relevantScans.isEmpty()) {
            sb.append("\n       http://www.springframework.org/schema/context\n");
            sb.append("       http://www.springframework.org/schema/context/spring-context.xsd");
        }
        sb.append("\">\n\n");

        sb.append("    <!-- Filtered from: ").append(escapeXml(xmlConfig.getFilePath())).append(" -->\n\n");

        for (String scan : relevantScans) {
            sb.append("    <context:component-scan base-package=\"").append(escapeXml(scan)).append("\"/>\n");
        }

        if (!relevantScans.isEmpty() && !relevantBeans.isEmpty()) {
            sb.append("\n");
        }

        for (BeanDefinition bean : relevantBeans) {
            sb.append("    <bean");
            if (bean.getId() != null && !bean.getId().isEmpty()) {
                sb.append(" id=\"").append(escapeXml(bean.getId())).append("\"");
            }
            sb.append(" class=\"").append(escapeXml(bean.getClassFqn())).append("\"");
            if (!"singleton".equals(bean.getScope())) {
                sb.append(" scope=\"").append(escapeXml(bean.getScope())).append("\"");
            }
            if (bean.getInitMethod() != null) {
                sb.append(" init-method=\"").append(escapeXml(bean.getInitMethod())).append("\"");
            }
            if (bean.getDestroyMethod() != null) {
                sb.append(" destroy-method=\"").append(escapeXml(bean.getDestroyMethod())).append("\"");
            }
            sb.append("/>\n");
        }

        sb.append("\n</beans>\n");
        return sb.toString();
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private ProjectTreeNode findNode(ProjectTreeNode node, String path) {
        if (path.equals(node.getPath())) return node;
        if (node.getChildren() != null) {
            for (ProjectTreeNode child : node.getChildren()) {
                ProjectTreeNode found = findNode(child, path);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static String extractFileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
