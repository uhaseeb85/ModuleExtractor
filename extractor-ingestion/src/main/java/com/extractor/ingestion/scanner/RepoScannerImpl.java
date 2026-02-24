package com.extractor.ingestion.scanner;

import com.extractor.core.exceptions.IngestionException;
import com.extractor.core.interfaces.RepoScanner;
import com.extractor.core.model.RepoConfig;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JGit-backed implementation of {@link RepoScanner}.
 *
 * <p>Clones repositories on first run and performs a pull (fast-forward only)
 * on subsequent runs. SSH authentication is supported when
 * {@code extractor.git.sshKeyPath} is configured.
 */
@Component
public class RepoScannerImpl implements RepoScanner {

    private static final Logger log = LoggerFactory.getLogger(RepoScannerImpl.class);

    @Value("${extractor.git.sshKeyPath:}")
    private String sshKeyPath;

    @Override
    public String syncRepo(RepoConfig config) throws IngestionException {
        Path localPath = Path.of(config.localPath());

        try {
            if (Files.exists(localPath.resolve(".git"))) {
                return pull(config, localPath);
            } else {
                return clone(config, localPath);
            }
        } catch (Exception e) {
            throw new IngestionException(
                    "Failed to sync repo '" + config.name() + "' from " + config.url(), e);
        }
    }

    @Override
    public List<Path> getChangedFiles(RepoConfig config, String lastSha) throws IngestionException {
        Path localPath = Path.of(config.localPath());
        List<Path> changedFiles = new ArrayList<>();

        try (Git git = Git.open(localPath.toFile())) {
            Repository repo = git.getRepository();

            if (lastSha == null || lastSha.isBlank()) {
                // Return all .java files
                return findAllJavaFiles(localPath);
            }

            ObjectId oldHead = repo.resolve(lastSha);
            ObjectId newHead = repo.resolve("HEAD");

            if (oldHead == null || newHead == null) {
                log.warn("Could not resolve SHA '{}' or HEAD for repo '{}' — returning all files",
                        lastSha, config.name());
                return findAllJavaFiles(localPath);
            }

            try (ObjectReader reader = repo.newObjectReader();
                 DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {

                diffFormatter.setRepository(repo);

                AbstractTreeIterator oldTree = prepareTreeParser(repo, oldHead);
                AbstractTreeIterator newTree = prepareTreeParser(repo, newHead);

                List<DiffEntry> diffs = diffFormatter.scan(oldTree, newTree);

                for (DiffEntry diff : diffs) {
                    String path = diff.getNewPath();
                    if (path != null && path.endsWith(".java")) {
                        Path absolutePath = localPath.resolve(path);
                        if (Files.exists(absolutePath)) {
                            changedFiles.add(absolutePath);
                        }
                    }
                }
            }

        } catch (IOException e) {
            throw new IngestionException(
                    "Failed to compute changed files for repo '" + config.name() + "'", e);
        }

        return changedFiles;
    }

    // ── Private helpers ────────────────────────────────────────────────

    private String clone(RepoConfig config, Path localPath) throws GitAPIException, IOException {
        log.info("Cloning '{}' from {} into {}", config.name(), config.url(), localPath);
        Files.createDirectories(localPath);

        var cloneCommand = Git.cloneRepository()
                .setURI(config.url())
                .setDirectory(localPath.toFile())
                .setBranch(config.branch());

        configureSsh(cloneCommand);

        try (Git git = cloneCommand.call()) {
            String sha = git.getRepository().resolve("HEAD").getName();
            log.info("Cloned '{}' @ {}", config.name(), sha);
            return sha;
        }
    }

    private String pull(RepoConfig config, Path localPath) throws IOException, GitAPIException {
        log.info("Pulling latest for '{}' (branch: {})", config.name(), config.branch());

        try (Git git = Git.open(localPath.toFile())) {
            var pullCommand = git.pull()
                    .setRemoteBranchName(config.branch());
            configureSsh(pullCommand);
            pullCommand.call();

            String sha = git.getRepository().resolve("HEAD").getName();
            log.info("Pulled '{}' @ {}", config.name(), sha);
            return sha;
        }
    }

    private void configureSsh(Object command) {
        if (sshKeyPath == null || sshKeyPath.isBlank()) return;

        File keyFile = new File(sshKeyPath);
        if (!keyFile.exists()) {
            log.warn("SSH key not found at '{}' — falling back to default SSH agent", sshKeyPath);
            return;
        }

        SshdSessionFactory sshSessionFactory = new SshdSessionFactoryBuilder()
                .setPreferredAuthentications("publickey")
                .setHomeDirectory(keyFile.getParentFile())
                .setSshDirectory(keyFile.getParentFile())
                .build(null);

        if (command instanceof org.eclipse.jgit.api.CloneCommand c) {
            c.setTransportConfigCallback(transport -> {
                if (transport instanceof SshTransport sshTransport) {
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                }
            });
        } else if (command instanceof org.eclipse.jgit.api.PullCommand p) {
            p.setTransportConfigCallback(transport -> {
                if (transport instanceof SshTransport sshTransport) {
                    sshTransport.setSshSessionFactory(sshSessionFactory);
                }
            });
        }
    }

    private AbstractTreeIterator prepareTreeParser(Repository repo, ObjectId objectId) throws IOException {
        try (RevWalk walk = new RevWalk(repo)) {
            RevCommit commit = walk.parseCommit(objectId);
            try (ObjectReader reader = repo.newObjectReader()) {
                CanonicalTreeParser parser = new CanonicalTreeParser();
                parser.reset(reader, commit.getTree().getId());
                return parser;
            }
        }
    }

    private List<Path> findAllJavaFiles(Path root) throws IngestionException {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(Files::isRegularFile)
                    .toList();
        } catch (IOException e) {
            throw new IngestionException("Failed to walk directory: " + root, e);
        }
    }
}
