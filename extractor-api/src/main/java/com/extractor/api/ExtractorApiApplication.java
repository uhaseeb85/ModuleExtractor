package com.extractor.api;

import com.extractor.ingestion.config.ExtractorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Java Monolith Module Extractor API.
 *
 * <p>Uses {@code scanBasePackages} to pick up components from all {@code extractor-*} modules.
 */
@SpringBootApplication(scanBasePackages = "com.extractor")
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties(ExtractorProperties.class)
public class ExtractorApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExtractorApiApplication.class, args);
    }
}
