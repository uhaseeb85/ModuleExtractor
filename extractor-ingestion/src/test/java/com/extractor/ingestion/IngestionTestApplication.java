package com.extractor.ingestion;

import com.extractor.ingestion.config.ExtractorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Minimal Spring Boot context used only in extractor-ingestion integration tests.
 */
@SpringBootApplication(scanBasePackages = {"com.extractor.ingestion", "com.extractor.graph"})
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(ExtractorProperties.class)
public class IngestionTestApplication {
    public static void main(String[] args) {
        SpringApplication.run(IngestionTestApplication.class, args);
    }
}
