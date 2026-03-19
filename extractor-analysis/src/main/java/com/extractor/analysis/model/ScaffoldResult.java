package com.extractor.analysis.model;

import java.util.List;

/**
 * The full result of scaffolding a module: a project tree, metadata, and any Spring configs.
 */
public final class ScaffoldResult {

    private final String moduleName;
    private final String modulePackageRoot;
    private final String repoName;
    private final ProjectTreeNode tree;
    private final List<String> springContextFiles;
    private final int totalFiles;

    public ScaffoldResult(String moduleName, String modulePackageRoot, String repoName,
                          ProjectTreeNode tree, List<String> springContextFiles, int totalFiles) {
        this.moduleName = moduleName;
        this.modulePackageRoot = modulePackageRoot;
        this.repoName = repoName;
        this.tree = tree;
        this.springContextFiles = springContextFiles;
        this.totalFiles = totalFiles;
    }

    public String getModuleName() { return moduleName; }
    public String getModulePackageRoot() { return modulePackageRoot; }
    public String getRepoName() { return repoName; }
    public ProjectTreeNode getTree() { return tree; }
    public List<String> getSpringContextFiles() { return springContextFiles; }
    public int getTotalFiles() { return totalFiles; }
}
