package com.extractor.analysis.service;

import com.extractor.graph.entity.ClassEntity;
import com.extractor.graph.store.GraphStore;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
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

    private final GraphStore store;

    public CandidateScoringService(GraphStore store) {
        this.store = store;
    }

    // ── Public API ────────────────────────────────────────────────────

    public static final class PackageScore {
        private final String packageFqn;
        private final String repoName;
        private final int classCount;
        private final int outboundDeps;
        private final int inboundDeps;
        private final double isolationScore;
        private final double stabilityScore;
        private final double sizeScore;
        private final double compositeScore;
        private final String recommendation;
        private final List<String> blockers;

        public PackageScore(String packageFqn, String repoName, int classCount,
                            int outboundDeps, int inboundDeps,
                            double isolationScore, double stabilityScore,
                            double sizeScore, double compositeScore,
                            String recommendation, List<String> blockers) {
            this.packageFqn = packageFqn;
            this.repoName = repoName;
            this.classCount = classCount;
            this.outboundDeps = outboundDeps;
            this.inboundDeps = inboundDeps;
            this.isolationScore = isolationScore;
            this.stabilityScore = stabilityScore;
            this.sizeScore = sizeScore;
            this.compositeScore = compositeScore;
            this.recommendation = recommendation;
            this.blockers = blockers;
        }

        public String getPackageFqn() { return packageFqn; }
        public String getRepoName() { return repoName; }
        public int getClassCount() { return classCount; }
        public int getOutboundDeps() { return outboundDeps; }
        public int getInboundDeps() { return inboundDeps; }
        public double getIsolationScore() { return isolationScore; }
        public double getStabilityScore() { return stabilityScore; }
        public double getSizeScore() { return sizeScore; }
        public double getCompositeScore() { return compositeScore; }
        public String getRecommendation() { return recommendation; }
        public List<String> getBlockers() { return blockers; }
    }

    /**
     * A proposed extracted module: one or more related packages grouped under a common root,
     * with their full class list and an overall extraction recommendation.
     */
    public static final class ModuleRecommendation {
        private final String moduleName;
        private final String modulePackageRoot;
        private final String repoName;
        private final List<String> packages;
        private final List<String> classes;
        private final int totalClasses;
        private final int totalInboundDeps;
        private final int totalOutboundDeps;
        private final double avgCompositeScore;
        private final double minIsolationScore;
        private final String recommendation;
        private final List<String> blockers;

        public ModuleRecommendation(String moduleName, String modulePackageRoot, String repoName,
                                    List<String> packages, List<String> classes,
                                    int totalClasses, int totalInboundDeps, int totalOutboundDeps,
                                    double avgCompositeScore, double minIsolationScore,
                                    String recommendation, List<String> blockers) {
            this.moduleName = moduleName;
            this.modulePackageRoot = modulePackageRoot;
            this.repoName = repoName;
            this.packages = packages;
            this.classes = classes;
            this.totalClasses = totalClasses;
            this.totalInboundDeps = totalInboundDeps;
            this.totalOutboundDeps = totalOutboundDeps;
            this.avgCompositeScore = avgCompositeScore;
            this.minIsolationScore = minIsolationScore;
            this.recommendation = recommendation;
            this.blockers = blockers;
        }

        public String getModuleName() { return moduleName; }
        public String getModulePackageRoot() { return modulePackageRoot; }
        public String getRepoName() { return repoName; }
        public List<String> getPackages() { return packages; }
        public List<String> getClasses() { return classes; }
        public int getTotalClasses() { return totalClasses; }
        public int getTotalInboundDeps() { return totalInboundDeps; }
        public int getTotalOutboundDeps() { return totalOutboundDeps; }
        public double getAvgCompositeScore() { return avgCompositeScore; }
        public double getMinIsolationScore() { return minIsolationScore; }
        public String getRecommendation() { return recommendation; }
        public List<String> getBlockers() { return blockers; }
    }

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

        results.sort((a, b) -> Double.compare(b.getCompositeScore(), a.getCompositeScore()));
        return results.stream().limit(limit).collect(Collectors.toList());
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
            if (p.getCompositeScore() < minScore) continue;
            String root = moduleRoot(p.getPackageFqn(), groupDepth);
            String key  = root + "::" + p.getRepoName();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(p);
        }

        List<ModuleRecommendation> result = new ArrayList<>();
        for (Map.Entry<String, List<PackageScore>> e : groups.entrySet()) {
            List<PackageScore> pkgs = e.getValue();
            String root = moduleRoot(pkgs.get(0).getPackageFqn(), groupDepth);
            String repo = pkgs.get(0).getRepoName();

            List<String> packageNames = pkgs.stream()
                    .map(PackageScore::getPackageFqn).sorted().collect(Collectors.toList());
            List<String> classes = fetchClassesForPackages(packageNames, repo);

            double avgScore  = pkgs.stream().mapToDouble(PackageScore::getCompositeScore).average().orElse(0);
            double minIso    = pkgs.stream().mapToDouble(PackageScore::getIsolationScore).min().orElse(0);
            int    totalIn   = pkgs.stream().mapToInt(PackageScore::getInboundDeps).sum();
            int    totalOut  = pkgs.stream().mapToInt(PackageScore::getOutboundDeps).sum();
            int    totalCls  = pkgs.stream().mapToInt(PackageScore::getClassCount).sum();

            // Aggregate blockers (de-duplicate)
            List<String> blockers = pkgs.stream()
                    .flatMap(ps -> ps.getBlockers().stream())
                    .distinct().collect(Collectors.toList());

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

        result.sort((a, b) -> Double.compare(b.getAvgCompositeScore(), a.getAvgCompositeScore()));
        return result;
    }

    // ── JGraphT / in-memory data helpers ──────────────────────────────

    /** Returns map of "pkg::repo" → [classCount] */
    private Map<String, int[]> fetchPackageClassCounts() {
        Map<String, int[]> map = new HashMap<>();
        for (ClassEntity c : store.allClasses()) {
            String pkg = c.getPackageName();
            if (pkg == null || pkg.trim().isEmpty()) continue;
            String key = key(pkg, c.getRepoName());
            map.computeIfAbsent(key, k -> new int[]{0})[0]++;
        }
        return map;
    }

    /** Returns map of "pkg::repo" → outboundCount (distinct external classes imported) */
    private Map<String, Integer> fetchOutboundDeps() {
        Map<String, Integer> map = new HashMap<>();
        DefaultDirectedGraph<String, DefaultEdge> graph = store.importGraph();

        for (ClassEntity a : store.allClasses()) {
            String aPkg = a.getPackageName();
            if (aPkg == null || aPkg.trim().isEmpty()) continue;

            String fqn = a.getFullyQualifiedName();
            if (!graph.containsVertex(fqn)) continue;

            Set<ClassEntity> distinctExternal = new HashSet<>();
            for (DefaultEdge edge : graph.outgoingEdgesOf(fqn)) {
                String targetFqn = graph.getEdgeTarget(edge);
                ClassEntity b = store.findClassByFqn(targetFqn).orElse(null);
                if (b == null) continue;
                String bPkg = b.getPackageName();
                if (!aPkg.equals(bPkg) || !a.getRepoName().equals(b.getRepoName())) {
                    distinctExternal.add(b);
                }
            }
            if (!distinctExternal.isEmpty()) {
                String aKey = key(aPkg, a.getRepoName());
                map.merge(aKey, distinctExternal.size(), Integer::sum);
            }
        }
        return map;
    }

    /** Returns map of "pkg::repo" → inboundCount (distinct external classes that import us) */
    private Map<String, Integer> fetchInboundDeps() {
        Map<String, Integer> map = new HashMap<>();
        DefaultDirectedGraph<String, DefaultEdge> graph = store.importGraph();

        for (ClassEntity y : store.allClasses()) {
            String yPkg = y.getPackageName();
            if (yPkg == null || yPkg.trim().isEmpty()) continue;

            String fqn = y.getFullyQualifiedName();
            if (!graph.containsVertex(fqn)) continue;

            Set<ClassEntity> distinctExternal = new HashSet<>();
            for (DefaultEdge edge : graph.incomingEdgesOf(fqn)) {
                String sourceFqn = graph.getEdgeSource(edge);
                ClassEntity x = store.findClassByFqn(sourceFqn).orElse(null);
                if (x == null) continue;
                String xPkg = x.getPackageName();
                if (!yPkg.equals(xPkg) || !y.getRepoName().equals(x.getRepoName())) {
                    distinctExternal.add(x);
                }
            }
            if (!distinctExternal.isEmpty()) {
                String yKey = key(yPkg, y.getRepoName());
                map.merge(yKey, distinctExternal.size(), Integer::sum);
            }
        }
        return map;
    }

    /** Returns sorted simple class names for all given packages in a repo. */
    private List<String> fetchClassesForPackages(List<String> packages, String repo) {
        if (packages.isEmpty()) return Collections.emptyList();
        Set<String> pkgSet = new HashSet<>(packages);
        return store.allClasses().stream()
                .filter(c -> repo.equals(c.getRepoName()) && pkgSet.contains(c.getPackageName()))
                .map(c -> c.getSimpleName() != null && !c.getSimpleName().trim().isEmpty()
                        ? c.getSimpleName() : c.getFullyQualifiedName())
                .filter(n -> n != null && !n.trim().isEmpty())
                .sorted()
                .collect(Collectors.toList());
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
