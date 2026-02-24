package com.extractor.api;

import com.extractor.ingestion.config.ExtractorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Java Monolith Module Extractor API.
 *
 * <p>Uses {@code scanBasePackages} to pick up components from all
 * {@code extractor-*} modules, and explicitly enables Neo4j repositories
 * (required because the repositories live in a different module to this class).
 */
@SpringBootApplication(scanBasePackages = "com.extractor")
@EnableNeo4jRepositories(basePackages = "com.extractor.graph.repository")
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(ExtractorProperties.class)
public class ExtractorApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExtractorApiApplication.class, args);
    }
}
