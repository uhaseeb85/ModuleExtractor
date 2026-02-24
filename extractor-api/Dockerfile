# ── Stage 1: Build ──────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /workspace

# Copy parent POM and all module POMs first (layer-caching for deps)
COPY pom.xml .
COPY extractor-core/pom.xml extractor-core/pom.xml
COPY extractor-graph/pom.xml extractor-graph/pom.xml
COPY extractor-ingestion/pom.xml extractor-ingestion/pom.xml
COPY extractor-analysis/pom.xml extractor-analysis/pom.xml
COPY extractor-search/pom.xml extractor-search/pom.xml
COPY extractor-api/pom.xml extractor-api/pom.xml

# Download dependencies (cached unless a POM changes)
RUN mvn -B dependency:go-offline -pl extractor-api -am

# Copy source
COPY extractor-core/src extractor-core/src
COPY extractor-graph/src extractor-graph/src
COPY extractor-ingestion/src extractor-ingestion/src
COPY extractor-analysis/src extractor-analysis/src
COPY extractor-api/src extractor-api/src

# Build (skip tests in image build; run tests in CI separately)
RUN mvn -B clean package -pl extractor-api -am -Dmaven.test.skip=true

# ── Stage 2: Runtime ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install Maven for the Maven Invoker build parser (needed at runtime for dependency:list)
RUN apt-get update \
 && apt-get install -y --no-install-recommends maven git \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/extractor-api/target/extractor-api-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
