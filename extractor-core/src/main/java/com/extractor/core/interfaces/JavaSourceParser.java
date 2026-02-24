package com.extractor.core.interfaces;

import com.extractor.core.exceptions.ParseException;
import com.extractor.core.model.ParseResult;
import com.extractor.core.model.RepoConfig;

import java.nio.file.Path;

/**
 * Parses a single Java source file into a {@link ParseResult} containing the
 * class node, method/field nodes, and all dependency edges (imports, calls, annotations).
 *
 * <p>Implementations should use JavaParser with a fully configured
 * {@code CombinedTypeSolver} so that cross-repo and third-party type references
 * can be resolved. The dependency JARs <em>must</em> be pre-resolved before calling
 * {@code parse} so that the type solver can locate them.
 */
public interface JavaSourceParser {

    /**
     * Parse a single {@code .java} source file.
     *
     * @param javaFile Absolute path to the Java source file.
     * @param repo     Configuration of the repository the file belongs to.
     * @return Fully populated {@link ParseResult} for the primary type declared in the file.
     * @throws ParseException If the file cannot be parsed or contains unrecoverable syntax errors.
     */
    ParseResult parse(Path javaFile, RepoConfig repo) throws ParseException;
}
