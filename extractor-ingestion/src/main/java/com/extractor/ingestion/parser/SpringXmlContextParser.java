package com.extractor.ingestion.parser;

import com.extractor.graph.entity.SpringXmlConfigEntity;
import com.extractor.graph.entity.SpringXmlConfigEntity.BeanDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Scans a repository for Spring XML context files and extracts bean definitions,
 * component-scan directives, and import declarations.
 */
@Component
public class SpringXmlContextParser {

    private static final Logger log = LoggerFactory.getLogger(SpringXmlContextParser.class);

    private static final String BEANS_NS = "http://www.springframework.org/schema/beans";
    private static final String CONTEXT_NS = "http://www.springframework.org/schema/context";

    /**
     * Walks {@code src/main/resources} (and {@code src/main/webapp}) looking for XML files
     * whose root element is {@code <beans>} in the Spring namespace.
     *
     * @param repoRoot absolute path to the repository root directory
     * @param repoName logical name of the repository
     * @return parsed Spring XML config entities (never null, may be empty)
     */
    public List<SpringXmlConfigEntity> parseRepo(Path repoRoot, String repoName) {
        List<Path> xmlFiles = collectXmlFiles(repoRoot);
        if (xmlFiles.isEmpty()) {
            return Collections.emptyList();
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        // Disable external entities to prevent XXE
        try {
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception e) {
            log.warn("Could not set XXE prevention features: {}", e.getMessage());
        }

        List<SpringXmlConfigEntity> results = new ArrayList<>();
        for (Path xmlFile : xmlFiles) {
            try {
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.parse(xmlFile.toFile());
                Element root = doc.getDocumentElement();

                if (!isSpringBeansRoot(root)) {
                    continue;
                }

                String relativePath = repoRoot.relativize(xmlFile).toString().replace('\\', '/');
                SpringXmlConfigEntity entity = new SpringXmlConfigEntity(relativePath, repoName);
                entity.setBeanDefinitions(extractBeans(root));
                entity.setComponentScanPackages(extractComponentScans(root));
                entity.setImportedResources(extractImports(root));
                results.add(entity);

                log.debug("Parsed Spring XML '{}': {} beans, {} scans, {} imports",
                        relativePath, entity.getBeanDefinitions().size(),
                        entity.getComponentScanPackages().size(),
                        entity.getImportedResources().size());

            } catch (Exception e) {
                log.warn("Failed to parse XML '{}': {}", xmlFile, e.getMessage());
            }
        }

        log.info("Found {} Spring XML config files in repo '{}'", results.size(), repoName);
        return results;
    }

    // ── XML extraction helpers ──────────────────────────────────────────

    private boolean isSpringBeansRoot(Element root) {
        String ns = root.getNamespaceURI();
        String local = root.getLocalName();
        return "beans".equals(local) && (BEANS_NS.equals(ns) || ns == null);
    }

    private List<BeanDefinition> extractBeans(Element root) {
        List<BeanDefinition> beans = new ArrayList<>();
        NodeList beanNodes = root.getElementsByTagNameNS(BEANS_NS, "bean");
        if (beanNodes.getLength() == 0) {
            beanNodes = root.getElementsByTagName("bean");
        }
        for (int i = 0; i < beanNodes.getLength(); i++) {
            if (!(beanNodes.item(i) instanceof Element)) continue;
            Element el = (Element) beanNodes.item(i);
            String id = attr(el, "id", attr(el, "name", ""));
            String classFqn = attr(el, "class", "");
            String scope = attr(el, "scope", "singleton");
            String initMethod = attr(el, "init-method", null);
            String destroyMethod = attr(el, "destroy-method", null);
            String dependsOnRaw = attr(el, "depends-on", "");
            List<String> dependsOn = dependsOnRaw.isEmpty()
                    ? Collections.emptyList()
                    : Arrays.asList(dependsOnRaw.split("[,;\\s]+"));
            beans.add(new BeanDefinition(id, classFqn, scope, initMethod, destroyMethod, dependsOn));
        }
        return beans;
    }

    private List<String> extractComponentScans(Element root) {
        List<String> packages = new ArrayList<>();
        NodeList nodes = root.getElementsByTagNameNS(CONTEXT_NS, "component-scan");
        if (nodes.getLength() == 0) {
            nodes = root.getElementsByTagName("component-scan");
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            if (!(nodes.item(i) instanceof Element)) continue;
            Element el = (Element) nodes.item(i);
            String basePackage = attr(el, "base-package", "");
            if (!basePackage.isEmpty()) {
                for (String pkg : basePackage.split("[,;\\s]+")) {
                    String trimmed = pkg.trim();
                    if (!trimmed.isEmpty()) packages.add(trimmed);
                }
            }
        }
        return packages;
    }

    private List<String> extractImports(Element root) {
        List<String> resources = new ArrayList<>();
        NodeList nodes = root.getElementsByTagNameNS(BEANS_NS, "import");
        if (nodes.getLength() == 0) {
            nodes = root.getElementsByTagName("import");
        }
        for (int i = 0; i < nodes.getLength(); i++) {
            if (!(nodes.item(i) instanceof Element)) continue;
            Element el = (Element) nodes.item(i);
            String resource = attr(el, "resource", "");
            if (!resource.isEmpty()) resources.add(resource);
        }
        return resources;
    }

    // ── File collection ─────────────────────────────────────────────────

    private List<Path> collectXmlFiles(Path repoRoot) {
        List<Path> xmlFiles = new ArrayList<>();
        Path[] searchDirs = {
                repoRoot.resolve("src/main/resources"),
                repoRoot.resolve("src/main/webapp/WEB-INF")
        };
        for (Path dir : searchDirs) {
            if (!Files.isDirectory(dir)) continue;
            try {
                Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        if (file.toString().endsWith(".xml")) {
                            xmlFiles.add(file);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                log.warn("Error walking '{}': {}", dir, e.getMessage());
            }
        }
        return xmlFiles;
    }

    private static String attr(Element el, String name, String defaultValue) {
        String val = el.getAttribute(name);
        return (val != null && !val.isEmpty()) ? val : defaultValue;
    }
}
