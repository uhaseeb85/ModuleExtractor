package com.extractor.analysis.service;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyses the dependency graph and scores each Java package as an extraction candidate.
 *
 * <p>Scoring dimensions (each 0–1):
 * <ul>
 *   <li><b>isolationScore</b>  – how few outbound dependencies the package has: {@code 1/(1+outbound)}</li>
 *   <li><b>stabilityScore</b>  – % of edges that are inbound (others depend on it, not the reverse)</li>
 *   <li><b>sizeScore</b>       – gaussian curve peaking at ~8 classes (ideal module size)</li>
 *   <li><b>compositeScore</b>  – weighted 45/35/20 combination of the above</li>
 * </ul>
 */
@Service
public class CandidateScoringService {

    private final Driver driver;

    public CandidateScoringService(Driver driver) {
        this.driver = driver;
    }

    // ── Public API ────────────────────────────────────────────────────

    public record PackageScore(
            String packageFqn,
            String repoName,
            int classCount,
            int outboundDeps,
            int inboundDeps,
            double isolationScore,
            double stabilityScore,
            double sizeScore,
            double compositeScore,
            String recommendation,
            List<String> blockers
    ) {}

    /**
     * A proposed extracted module: one or more related packages grouped under a common root,
     * with their full class list and an overall extraction recommendation.
     */
    public record ModuleRecommendation(
            String moduleName,         // e.g. "auth"
            String modulePackageRoot,  // e.g. "com.bank.ivr.auth"
            String repoName,
            List<String> packages,
            List<String> classes,
            int totalClasses,
            int totalInboundDeps,
            int totalOutboundDeps,
            double avgCompositeScore,
            double minIsolationScore,
            String recommendation,     // "Extract now" / "Extract with refactoring" / "Low priority"
            List<String> blockers
    ) {}

    /**
     * Scores all packages in the graph and returns them ranked by composite score (descending).
     * Only packages with at least 2 classes are included.
     *
     * @param minClasses minimum classes in the package (default 2)
     * @param limit      maximum results to return
     */
    public List<PackageScore> rankCandidates(int minClasses, int limit) {
        // Step 1: class counts per package
        Map<String, int[]> stats = fetchPackageClassCounts();

        // Step 2: outbound deps (imports going OUT of the package)
        Map<String, Integer> outbound = fetchOutboundDeps();

        // Step 3: inbound deps (imports coming IN to the package)
        Map<String, Integer> inbound = fetchInboundDeps();

        // Step 4: merge and score
        List<PackageScore> results = new ArrayList<>();
        for (Map.Entry<String, int[]> e : stats.entrySet()) {
            String key = e.getKey();        // "pkg::repo"
            int classCount = e.getValue()[0];
            if (classCount < minClasses) continue;

            String[] parts = key.split("::", 2);
            String pkg  = parts[0];
            String repo = parts.length > 1 ? parts[1] : "";

            int out = outbound.getOrDefault(key, 0);
            int in  = inbound.getOrDefault(key, 0);

            double iso   = 1.0 / (1.0 + out);
            double stab  = (in + out) == 0 ? 0.5 : (double) in / (in + out);
            double size  = gaussianSizeScore(classCount);
            double comp  = 0.45 * iso + 0.35 * stab + 0.20 * size;

            String recommendation = comp >= 0.65 ? "Strong candidate"
                    : comp >= 0.45 ? "Moderate candidate"
                    : "Low priority";

            List<String> blockers = detectBlockers(classCount, out);

            results.add(new PackageScore(pkg, repo, classCount, out, in,
                    round2(iso), round2(stab), round2(size), round2(comp),
                    recommendation, blockers));
        }

        results.sort((a, b) -> Double.compare(b.compositeScore(), a.compositeScore()));
        return results.stream().limit(limit).toList();
    }

    /**
     * Groups scored packages into proposed modules and returns them ranked by score.
     *
     * <p>Grouping strategy: packages sharing the same first {@code groupDepth} segments
     * (e.g. depth 4 → {@code com.bank.ivr.auth}) are merged into one module.
     *
     * @param groupDepth  number of package segments that form the module root (default 4)
     * @param minScore    minimum composite score to include a package (default 0.4)
     */
    public List<ModuleRecommendation> recommendModules(int groupDepth, double minScore) {
        List<PackageScore> all = rankCandidates(1, Integer.MAX_VALUE);

        // Group by (moduleRoot, repoName)
        Map<String, List<PackageScore>> groups = new LinkedHashMap<>();
        for (PackageScore p : all) {
            if (p.compositeScore() < minScore) continue;
            String root = moduleRoot(p.packageFqn(), groupDepth);
            String key  = root + "::" + p.repoName();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

        List<ModuleRecommendation> result = new ArrayList<>();
        for (Map.Entry<String, List<PackageScore>> e : groups.entrySet()) {
            List<PackageScore> pkgs = e.getValue();
            String root = moduleRoot(pkgs.get(0).packageFqn(), groupDepth);
            String repo = pkgs.get(0).repoName();

            List<String> packageNames = pkgs.stream()
                    .map(PackageScore::packageFqn).sorted().toList();
            List<String> classes = fetchClassesForPackages(packageNames, repo);

            double avgScore  = pkgs.stream().mapToDouble(PackageScore::compositeScore).average().orElse(0);
            double minIso    = pkgs.stream().mapToDouble(PackageScore::isolationScore).min().orElse(0);
            int    totalIn   = pkgs.stream().mapToInt(PackageScore::inboundDeps).sum();
            int    totalOut  = pkgs.stream().mapToInt(PackageScore::outboundDeps).sum();
            int    totalCls  = pkgs.stream().mapToInt(PackageScore::classCount).sum();

            // Aggregate blockers (de-duplicate)
            List<String> blockers = pkgs.stream()
                    .flatMap(ps -> ps.blockers().stream())
                    .distinct().toList();

            String rec = avgScore >= 0.65 ? "Extract now"
                    : avgScore >= 0.45    ? "Extract with refactoring"
                    : "Low priority";

            result.add(new ModuleRecommendation(
                    lastSegment(root), root, repo,
                    packageNames, classes,
                    totalCls, totalIn, totalOut,
                    round2(avgScore), round2(minIso),
                    rec, blockers));
        }

        result.sort((a, b) -> Double.compare(b.avgCompositeScore(), a.avgCompositeScore()));
        return result;
    }

    // ── Cypher helpers ────────────────────────────────────────────────

    /** Returns map of "pkg::repo" → [classCount] */
    private Map<String, int[]> fetchPackageClassCounts() {
        Map<String, int[]> map = new HashMap<>();
        try (Session s = driver.session()) {
            s.run("""
                    MATCH (c:Class)
                    WHERE c.packageName IS NOT NULL AND trim(c.packageName) <> ''
                    RETURN c.packageName AS pkg, c.repoName AS repo, count(c) AS n
                    """)
             .list()
             .forEach(r -> {
                 String key = key(r.get("pkg").asString(""), r.get("repo").asString(""));
                 int n = r.get("n").asInt(0);
                 map.put(key, new int[]{n});
             });
        }
        return map;
    }

    /** Returns map of "pkg::repo" → outboundCount (distinct external classes imported) */
    private Map<String, Integer> fetchOutboundDeps() {
        Map<String, Integer> map = new HashMap<>();
        try (Session s = driver.session()) {
            s.run("""
                    MATCH (a:Class)-[:IMPORTS]->(b:Class)
                    WHERE a.packageName IS NOT NULL AND trim(a.packageName) <> ''
                      AND (b.packageName <> a.packageName OR b.repoName <> a.repoName)
                    RETURN a.packageName AS pkg, a.repoName AS repo, count(DISTINCT b) AS n
                    """)
             .list()
             .forEach(r -> {
                 String key = key(r.get("pkg").asString(""), r.get("repo").asString(""));
                 map.put(key, r.get("n").asInt(0));
             });
        }
        return map;
    }

    /** Returns map of "pkg::repo" → inboundCount (distinct external classes that import us) */
    private Map<String, Integer> fetchInboundDeps() {
        Map<String, Integer> map = new HashMap<>();
        try (Session s = driver.session()) {
            s.run("""
                    MATCH (x:Class)-[:IMPORTS]->(y:Class)
                    WHERE y.packageName IS NOT NULL AND trim(y.packageName) <> ''
                      AND (x.packageName <> y.packageName OR x.repoName <> y.repoName)
                    RETURN y.packageName AS pkg, y.repoName AS repo, count(DISTINCT x) AS n
                    """)
             .list()
             .forEach(r -> {
                 String key = key(r.get("pkg").asString(""), r.get("repo").asString(""));
                 map.put(key, r.get("n").asInt(0));
             });
        }
        return map;
    }

    /** Returns sorted simple class names for all given packages in a repo. */
    private List<String> fetchClassesForPackages(List<String> packages, String repo) {
        if (packages.isEmpty()) return List.of();
        try (Session s = driver.session()) {
            return s.run(
                    "MATCH (c:Class) " +
                    "WHERE c.repoName = $repo AND c.packageName IN $pkgs " +
                    "RETURN coalesce(c.simpleName, c.fullyQualifiedName) AS name " +
                    "ORDER BY c.packageName, c.simpleName",
                    Map.of("repo", repo, "pkgs", packages))
             .list(r -> r.get("name").asString(""))
             .stream()
             .filter(n -> !n.isBlank())
             .toList();
        }
    }

    // ── Scoring utilities ─────────────────────────────────────────────

    /**
     * Gaussian curve peaking at idealSize=8.
     * Score = exp(-0.5 * ((n - ideal) / sigma)^2), sigma=5.
     */
    private double gaussianSizeScore(int classCount) {
        double ideal = 8.0;
        double sigma = 5.0;
        double z = (classCount - ideal) / sigma;
        return Math.exp(-0.5 * z * z);
    }

    private List<String> detectBlockers(int classCount, int outbound) {
        List<String> blockers = new ArrayList<>();
        if (outbound > 10)      blockers.add("High outbound coupling (" + outbound + " dependencies to sever)");
        if (outbound > 5)       blockers.add("Moderate outbound coupling — review before extracting");
        if (classCount > 30)    blockers.add("Package too large (" + classCount + " classes) — consider splitting");
        if (classCount < 3)     blockers.add("Package very small (" + classCount + " classes) — may not justify a separate module");
        return blockers;
    }

    private static String key(String pkg, String repo) {
        return pkg + "::" + repo;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Returns the first {@code depth} segments of a dotted package name. */
    private static String moduleRoot(String pkg, int depth) {
        String[] parts = pkg.split("\\.");
        int take = Math.min(depth, Math.max(parts.length - 1, 1));
        return String.join(".", Arrays.copyOf(parts, take));
    }

    /** Returns the last segment of a dotted package name (used as human-readable module name). */
    private static String lastSegment(String pkg) {
        int idx = pkg.lastIndexOf('.');
        return idx >= 0 ? pkg.substring(idx + 1) : pkg;
    }
}
