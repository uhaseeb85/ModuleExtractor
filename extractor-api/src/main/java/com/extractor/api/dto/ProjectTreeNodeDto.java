package com.extractor.api.dto;

import java.util.List;

/**
 * DTO representing a single node in the scaffolded project tree.
 */
public final class ProjectTreeNodeDto {

    private final String name;
    private final String path;
    private final String type;
    private final List<ProjectTreeNodeDto> children;
    private final boolean hasContent;
    private final String sourceRef;

    public ProjectTreeNodeDto(String name, String path, String type,
                               List<ProjectTreeNodeDto> children,
                               boolean hasContent, String sourceRef) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.children = children;
        this.hasContent = hasContent;
        this.sourceRef = sourceRef;
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public String getType() { return type; }
    public List<ProjectTreeNodeDto> getChildren() { return children; }
    public boolean isHasContent() { return hasContent; }
    public String getSourceRef() { return sourceRef; }
}
