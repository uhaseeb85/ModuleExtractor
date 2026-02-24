package com.extractor.api.controller;

import com.extractor.analysis.service.CandidateScoringService;
import com.extractor.api.dto.CandidateDetailDto;
import com.extractor.api.dto.CandidateSummaryDto;
import com.extractor.api.dto.ModuleRecommendationDto;
import com.extractor.graph.repository.ClassEntityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
public class AnalysisController {

    private final CandidateScoringService scoringService;
    private final ClassEntityRepository classRepo;

    public AnalysisController(CandidateScoringService scoringService,
                               ClassEntityRepository classRepo) {
        this.scoringService = scoringService;
        this.classRepo = classRepo;
    }

    /**
     * Returns all packages ranked by extraction-candidate composite score.
     *
     * @param minClasses minimum class-count threshold (default 2)
     * @param limit      max results returned (default 100)
     */
    @GetMapping("/candidates")
    public List<CandidateSummaryDto> listCandidates(
            @RequestParam(name = "minClasses", defaultValue = "2") int minClasses,
            @RequestParam(name = "limit",      defaultValue = "100") int limit) {

        return scoringService.rankCandidates(minClasses, limit)
                .stream()
                .map(s -> new CandidateSummaryDto(
                        s.packageFqn(), s.repoName(),
                        s.classCount(), s.inboundDeps(), s.outboundDeps(),
                        s.isolationScore(), s.stabilityScore(), s.sizeScore(),
                        s.compositeScore(), s.recommendation()))
                .toList();
    }

    /**
     * Returns detailed info for one package, including class list and blockers.
     *
     * @param packageFqn URL-encoded fully-qualified package name
     * @param repoName   optional repo filter (required when same package exists in multiple repos)
     */
    @GetMapping("/candidates/{packageFqn}")
    public ResponseEntity<CandidateDetailDto> getCandidateDetail(
            @PathVariable("packageFqn") String packageFqn,
            @RequestParam(name = "repoName", required = false) String repoName) {

        List<CandidateScoringService.PackageScore> all = scoringService.rankCandidates(0, Integer.MAX_VALUE);

        CandidateScoringService.PackageScore match = all.stream()
                .filter(s -> s.packageFqn().equals(packageFqn)
                        && (repoName == null || repoName.isBlank() || s.repoName().equals(repoName)))
                .findFirst()
                .orElse(null);

        if (match == null) {
            return ResponseEntity.notFound().build();
        }

        // Fetch actual class names from the graph
        List<String> classNames = (repoName != null && !repoName.isBlank())
                ? classRepo.findByRepoNameAndPackageName(repoName, packageFqn)
                           .stream()
                           .map(c -> c.getSimpleName() != null ? c.getSimpleName() : c.getFullyQualifiedName())
                           .sorted()
                           .toList()
                : classRepo.findByPackageName(packageFqn)
                           .stream()
                           .map(c -> c.getSimpleName() != null ? c.getSimpleName() : c.getFullyQualifiedName())
                           .sorted()
                           .toList();

        CandidateDetailDto dto = new CandidateDetailDto(
                match.packageFqn(), match.repoName(),
                match.classCount(), match.inboundDeps(), match.outboundDeps(),
                match.isolationScore(), match.stabilityScore(), match.sizeScore(),
                match.compositeScore(), match.recommendation(),
                match.blockers(), classNames);

        return ResponseEntity.ok(dto);
    }

    /**
     * Returns a list of proposed modules, each grouping related packages under a common root,
     * with the full class list for each module.
     *
     * @param groupDepth segments used to determine module root (default 4, e.g. "com.acme.svc.auth")
     * @param minScore   minimum composite score for a package to be included (default 0.4)
     */
    @GetMapping("/recommendations")
    public List<ModuleRecommendationDto> listRecommendations(
            @RequestParam(name = "groupDepth", defaultValue = "4")  int groupDepth,
            @RequestParam(name = "minScore",   defaultValue = "0.4") double minScore) {

        return scoringService.recommendModules(groupDepth, minScore)
                .stream()
                .map(m -> new ModuleRecommendationDto(
                        m.moduleName(), m.modulePackageRoot(), m.repoName(),
                        m.packages(), m.classes(),
                        m.totalClasses(), m.totalInboundDeps(), m.totalOutboundDeps(),
                        m.avgCompositeScore(), m.minIsolationScore(),
                        m.recommendation(), m.blockers()))
                .toList();
    }
}
