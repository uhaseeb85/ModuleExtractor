package com.extractor.api.controller;

import com.extractor.analysis.model.ProjectTreeNode;
import com.extractor.analysis.model.ScaffoldResult;
import com.extractor.analysis.service.CandidateScoringService;
import com.extractor.analysis.service.ModuleScaffoldingService;
import com.extractor.analysis.service.ZipExportService;
import com.extractor.api.dto.ProjectTreeNodeDto;
import com.extractor.api.dto.ScaffoldPreviewDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/scaffold")
public class ScaffoldController {

    private static final Logger log = LoggerFactory.getLogger(ScaffoldController.class);

    private final ModuleScaffoldingService scaffoldingService;
    private final CandidateScoringService scoringService;
    private final ZipExportService zipExportService;

    /** In-memory cache of scaffold results keyed by "moduleName::repoName". */
    private final Map<String, ScaffoldResult> cache = new ConcurrentHashMap<>();

    public ScaffoldController(ModuleScaffoldingService scaffoldingService,
                               CandidateScoringService scoringService,
                               ZipExportService zipExportService) {
        this.scaffoldingService = scaffoldingService;
        this.scoringService = scoringService;
        this.zipExportService = zipExportService;
    }

    /**
     * Preview the scaffolded project structure for a module recommendation.
     *
     * @param moduleName        module name (last segment of package root)
     * @param modulePackageRoot full package root e.g. "com.bank.ivr.auth"
     * @param repoName          source repository name
     * @param groupDepth        grouping depth for recommendations (default 4)
     * @param minScore          minimum composite score (default 0.4)
     */
    @GetMapping("/preview")
    public ResponseEntity<?> preview(
            @RequestParam("moduleName") String moduleName,
            @RequestParam("modulePackageRoot") String modulePackageRoot,
            @RequestParam("repoName") String repoName,
            @RequestParam(name = "groupDepth", defaultValue = "4") int groupDepth,
            @RequestParam(name = "minScore", defaultValue = "0.4") double minScore) {

        // Find the matching recommendation to get the package list
        List<String> packages = findPackagesForModule(modulePackageRoot, repoName, groupDepth, minScore);
        if (packages.isEmpty()) {
            log.warn("No packages found for module '{}' (root={}, repo={}, depth={}, minScore={}). "
                            + "Total recommendations: {}",
                    moduleName, modulePackageRoot, repoName, groupDepth, minScore,
                    scoringService.recommendModules(groupDepth, minScore).size());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No matching module recommendation found",
                            "detail", "Module '" + moduleName + "' in repo '" + repoName
                                    + "' did not match any current recommendations. "
                                    + "Ensure the repository has been synced and analysis is up to date."));
        }

        ScaffoldResult result = scaffoldingService.scaffold(moduleName, modulePackageRoot, repoName, packages);
        String cacheKey = moduleName + "::" + repoName;
        cache.put(cacheKey, result);

        return ResponseEntity.ok(toDto(result));
    }

    /**
     * Get the generated content for a specific file in the scaffold.
     */
    @GetMapping("/file")
    public ResponseEntity<Map<String, String>> getFileContent(
            @RequestParam("moduleName") String moduleName,
            @RequestParam("repoName") String repoName,
            @RequestParam("filePath") String filePath) {

        String cacheKey = moduleName + "::" + repoName;
        ScaffoldResult result = cache.get(cacheKey);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        String content = scaffoldingService.getFileContent(result, filePath);
        if (content == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(Map.of("content", content, "filePath", filePath));
    }

    /**
     * Export the scaffolded module as a ZIP archive.
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam("moduleName") String moduleName,
            @RequestParam("modulePackageRoot") String modulePackageRoot,
            @RequestParam("repoName") String repoName,
            @RequestParam(name = "groupDepth", defaultValue = "4") int groupDepth,
            @RequestParam(name = "minScore", defaultValue = "0.4") double minScore) throws IOException {

        // Build (or retrieve cached) scaffold
        String cacheKey = moduleName + "::" + repoName;
        ScaffoldResult result = cache.get(cacheKey);
        if (result == null) {
            List<String> packages = findPackagesForModule(modulePackageRoot, repoName, groupDepth, minScore);
            if (packages.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            result = scaffoldingService.scaffold(moduleName, modulePackageRoot, repoName, packages);
            cache.put(cacheKey, result);
        }

        byte[] zip = zipExportService.export(result);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + moduleName + ".zip\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(zip);
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private List<String> findPackagesForModule(String modulePackageRoot, String repoName,
                                               int groupDepth, double minScore) {
        return scoringService.recommendModules(groupDepth, minScore).stream()
                .filter(m -> modulePackageRoot.equals(m.getModulePackageRoot())
                        && repoName.equals(m.getRepoName()))
                .findFirst()
                .map(CandidateScoringService.ModuleRecommendation::getPackages)
                .orElse(Collections.emptyList());
    }

    private ScaffoldPreviewDto toDto(ScaffoldResult result) {
        return new ScaffoldPreviewDto(
                result.getModuleName(),
                result.getModulePackageRoot(),
                result.getRepoName(),
                toNodeDto(result.getTree()),
                result.getSpringContextFiles(),
                result.getTotalFiles());
    }

    private ProjectTreeNodeDto toNodeDto(ProjectTreeNode node) {
        List<ProjectTreeNodeDto> childDtos = null;
        if (node.getChildren() != null) {
            childDtos = node.getChildren().stream()
                    .map(this::toNodeDto)
                    .collect(Collectors.toList());
        }
        return new ProjectTreeNodeDto(
                node.getName(),
                node.getPath(),
                node.getType().name(),
                childDtos,
                node.getContent() != null,
                node.getSourceRef());
    }
}
