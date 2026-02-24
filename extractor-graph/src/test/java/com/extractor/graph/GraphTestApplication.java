package com.extractor.graph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application used only for integration tests in extractor-graph.
 * Does NOT enable scheduling or async — these are wired at the extractor-api level.
 */
@SpringBootApplication(scanBasePackages = "com.extractor.graph")
public class GraphTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(GraphTestApplication.class, args);
    }
}
