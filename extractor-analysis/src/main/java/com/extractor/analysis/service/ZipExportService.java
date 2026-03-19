package com.extractor.analysis.service;

import com.extractor.analysis.model.ProjectTreeNode;
import com.extractor.analysis.model.ScaffoldResult;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Exports a {@link ScaffoldResult} as a ZIP archive with the full Maven project structure.
 */
@Service
public class ZipExportService {

    /**
     * Creates an in-memory ZIP of the scaffold result.
     *
     * @param result the scaffold to export
     * @return ZIP file contents as a byte array
     */
    public byte[] export(ScaffoldResult result) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            writeNode(zos, result.getTree());
        }
        return baos.toByteArray();
    }

    private void writeNode(ZipOutputStream zos, ProjectTreeNode node) throws IOException {
        if (node.isDirectory()) {
            // Add directory entry
            zos.putNextEntry(new ZipEntry(node.getPath() + "/"));
            zos.closeEntry();

            if (node.getChildren() != null) {
                for (ProjectTreeNode child : node.getChildren()) {
                    writeNode(zos, child);
                }
            }
        } else {
            // File entry
            zos.putNextEntry(new ZipEntry(node.getPath()));
            String content;
            if (node.getContent() != null) {
                content = node.getContent();
            } else if (node.getSourceRef() != null) {
                content = "// Source: " + node.getSourceRef() + "\n"
                        + "// This file will be moved from the original repository.\n"
                        + "// Copy the original source file here during extraction.\n";
            } else {
                content = "";
            }
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
    }
}
