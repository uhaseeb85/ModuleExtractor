package com.extractor.api.dto;

import java.util.List;

/**
 * DTO returned by the scaffold preview endpoint.
 */
public final class ScaffoldPreviewDto {

    private final String moduleName;
    private final String modulePackageRoot;
    private final String repoName;
    private final ProjectTreeNodeDto tree;
    private final List<String> springContextFiles;
    private final int totalFiles;

    public ScaffoldPreviewDto(String moduleName, String modulePackageRoot, String repoName,
                               ProjectTreeNodeDto tree, List<String> springContextFiles, int totalFiles) {
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
    public ProjectTreeNodeDto getTree() { return tree; }
    public List<String> getSpringContextFiles() { return springContextFiles; }
    public int getTotalFiles() { return totalFiles; }
}
