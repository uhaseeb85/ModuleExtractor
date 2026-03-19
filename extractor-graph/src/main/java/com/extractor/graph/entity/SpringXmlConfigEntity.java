package com.extractor.graph.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Plain-POJO representing a Spring XML application-context file.
 * Captures bean definitions, component-scan directives and imported resources.
 */
public class SpringXmlConfigEntity {

    private String filePath;
    private String repoName;
    private List<BeanDefinition> beanDefinitions = new ArrayList<>();
    private List<String> componentScanPackages = new ArrayList<>();
    private List<String> importedResources = new ArrayList<>();

    protected SpringXmlConfigEntity() {}

    public SpringXmlConfigEntity(String filePath, String repoName) {
        this.filePath = filePath;
        this.repoName = repoName;
    }

    public String getFilePath() { return filePath; }
    public String getRepoName() { return repoName; }
    public List<BeanDefinition> getBeanDefinitions() { return beanDefinitions; }
    public void setBeanDefinitions(List<BeanDefinition> beanDefinitions) { this.beanDefinitions = beanDefinitions; }
    public List<String> getComponentScanPackages() { return componentScanPackages; }
    public void setComponentScanPackages(List<String> componentScanPackages) { this.componentScanPackages = componentScanPackages; }
    public List<String> getImportedResources() { return importedResources; }
    public void setImportedResources(List<String> importedResources) { this.importedResources = importedResources; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SpringXmlConfigEntity)) return false;
        SpringXmlConfigEntity that = (SpringXmlConfigEntity) o;
        return Objects.equals(filePath, that.filePath) && Objects.equals(repoName, that.repoName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filePath, repoName);
    }

    /**
     * A single {@code <bean>} definition from a Spring XML context file.
     */
    public static final class BeanDefinition {
        private final String id;
        private final String classFqn;
        private final String scope;
        private final String initMethod;
        private final String destroyMethod;
        private final List<String> dependsOn;

        public BeanDefinition(String id, String classFqn, String scope,
                              String initMethod, String destroyMethod, List<String> dependsOn) {
            this.id = id;
            this.classFqn = classFqn;
            this.scope = scope;
            this.initMethod = initMethod;
            this.destroyMethod = destroyMethod;
            this.dependsOn = dependsOn;
        }

        public String getId() { return id; }
        public String getClassFqn() { return classFqn; }
        public String getScope() { return scope; }
        public String getInitMethod() { return initMethod; }
        public String getDestroyMethod() { return destroyMethod; }
        public List<String> getDependsOn() { return dependsOn; }
    }
}
