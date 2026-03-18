package com.extractor.core.model;

import java.util.Objects;

/**
 * Represents an IMPORTS relationship between two classes.
 */
public final class ImportEdge {

    private final String importerFqn;
    private final String importedFqn;
    private final boolean isStatic;
    private final boolean isWildcard;

    public ImportEdge(String importerFqn, String importedFqn, boolean isStatic, boolean isWildcard) {
        this.importerFqn = importerFqn;
        this.importedFqn = importedFqn;
        this.isStatic = isStatic;
        this.isWildcard = isWildcard;
    }

    public String getImporterFqn() { return importerFqn; }
    public String getImportedFqn() { return importedFqn; }
    public boolean isStatic() { return isStatic; }
    public boolean isWildcard() { return isWildcard; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImportEdge)) return false;
        ImportEdge that = (ImportEdge) o;
        return isStatic == that.isStatic && isWildcard == that.isWildcard
                && Objects.equals(importerFqn, that.importerFqn)
                && Objects.equals(importedFqn, that.importedFqn);
    }

    @Override
    public int hashCode() { return Objects.hash(importerFqn, importedFqn, isStatic, isWildcard); }

    @Override
    public String toString() {
        return "ImportEdge{importer='" + importerFqn + "', imported='" + importedFqn + "'}";
    }
}
