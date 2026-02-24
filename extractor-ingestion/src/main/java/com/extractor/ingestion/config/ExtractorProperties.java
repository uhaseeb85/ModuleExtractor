package com.extractor.ingestion.config;

import com.extractor.core.enums.BuildTool;
import com.extractor.core.model.RepoConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Binds the {@code extractor} section of {@code application.yml} / {@code repos.yml}.
 *
 * <p>Example YAML:
 * <pre>
 * extractor:
 *   repos:
 *     - name: payments-service
 *       url: https://github.com/org/payments-service.git
 *       branch: main
 *       buildTool: MAVEN
 *       localPath: /data/repos/payments-service
 *   git:
 *     sshKeyPath: /root/.ssh/id_rsa
 *     cloneOnStartup: true
 * </pre>
 */
@ConfigurationProperties(prefix = "extractor")
public class ExtractorProperties {

    private List<RepoProperties> repos = new ArrayList<>();
    private GitProperties git = new GitProperties();

    public List<RepoProperties> getRepos() { return repos; }
    public void setRepos(List<RepoProperties> repos) { this.repos = repos; }
    public GitProperties getGit() { return git; }
    public void setGit(GitProperties git) { this.git = git; }

    /** Converts the YAML-bound list entries to {@link RepoConfig} records. */
    public List<RepoConfig> toRepoConfigs() {
        return repos.stream()
                .map(r -> new RepoConfig(r.getName(), r.getUrl(), r.getBranch(),
                        BuildTool.valueOf(r.getBuildTool().toUpperCase()), r.getLocalPath()))
                .toList();
    }

    // ── Nested property classes ─────────────────────────────────────────

    public static class RepoProperties {
        private String name;
        private String url;
        private String branch = "main";
        private String buildTool = "MAVEN";
        private String localPath;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getBranch() { return branch; }
        public void setBranch(String branch) { this.branch = branch; }
        public String getBuildTool() { return buildTool; }
        public void setBuildTool(String buildTool) { this.buildTool = buildTool; }
        public String getLocalPath() { return localPath; }
        public void setLocalPath(String localPath) { this.localPath = localPath; }
    }

    public static class GitProperties {
        private String sshKeyPath;
        private boolean cloneOnStartup = true;

        public String getSshKeyPath() { return sshKeyPath; }
        public void setSshKeyPath(String sshKeyPath) { this.sshKeyPath = sshKeyPath; }
        public boolean isCloneOnStartup() { return cloneOnStartup; }
        public void setCloneOnStartup(boolean cloneOnStartup) { this.cloneOnStartup = cloneOnStartup; }
    }
}
