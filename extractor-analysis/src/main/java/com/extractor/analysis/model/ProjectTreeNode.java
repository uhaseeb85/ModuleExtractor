package com.extractor.analysis.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single node in a scaffolded module's project file tree.
 * Can be a directory or a file; files optionally carry generated content.
 */
public class ProjectTreeNode {

    public enum NodeType { DIRECTORY, FILE }

    private final String name;
    private final String path;
    private final NodeType type;
    private final List<ProjectTreeNode> children;

    /** Non-null only for generated files (pom.xml, context XML, package-info). */
    private String content;

    /** If the file is a MOVE reference from the original repo (e.g. Java source classes). */
    private String sourceRef;

    public ProjectTreeNode(String name, String path, NodeType type) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.children = type == NodeType.DIRECTORY ? new ArrayList<>() : null;
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public NodeType getType() { return type; }
    public List<ProjectTreeNode> getChildren() { return children; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSourceRef() { return sourceRef; }
    public void setSourceRef(String sourceRef) { this.sourceRef = sourceRef; }

    public boolean isDirectory() { return type == NodeType.DIRECTORY; }

    public ProjectTreeNode addChild(ProjectTreeNode child) {
        if (children == null) throw new UnsupportedOperationException("Cannot add children to a file node");
        children.add(child);
        return child;
    }

    /** Finds or creates the nested directory path under this node. */
    public ProjectTreeNode mkdirs(String relativePath) {
        String[] parts = relativePath.split("/");
        ProjectTreeNode current = this;
        StringBuilder pathBuilder = new StringBuilder(this.path);
        for (String part : parts) {
            if (part.isEmpty()) continue;
            pathBuilder.append("/").append(part);
            String childPath = pathBuilder.toString();
            ProjectTreeNode found = null;
            if (current.children != null) {
                for (ProjectTreeNode c : current.children) {
                    if (c.isDirectory() && c.name.equals(part)) {
                        found = c;
                        break;
                    }
                }
            }
            if (found == null) {
                found = current.addChild(new ProjectTreeNode(part, childPath, NodeType.DIRECTORY));
            }
            current = found;
        }
        return current;
    }
}
