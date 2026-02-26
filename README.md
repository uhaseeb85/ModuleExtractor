# Java Monolith Module Extractor

A tool for analysing Java monoliths and mapping cross-module dependencies into a Neo4j graph — Phase 1 (Foundation & Visibility).

## Modules

| Module | Purpose |
|---|---|
| `extractor-core` | Enums, model records, interfaces, exceptions — no Spring |
| `extractor-graph` | Spring Data Neo4j entities, repositories, `GraphBuilderImpl` |
| `extractor-ingestion` | JGit scanner, JavaParser, Maven/Gradle build parsers, orchestrator |
| `extractor-api` | Spring Boot REST API (`/api/v1/graph`, `/api/v1/ingestion`) |
| `extractor-analysis` | Phase 2 stub — coupling metrics, blocker detection |
| `extractor-search` | Phase 3 stub — semantic/vector search |
| `extractor-frontend` | React 18 + Vite + Cytoscape.js visualisation |

## Quick Start (Docker Compose)

```bash
# 1. Build the API image
mvn -B clean package -pl extractor-api -am -DskipTests

docker build -t extractor-api:latest -f extractor-api/Dockerfile .

# 2. Build the frontend image
cd extractor-frontend && npm ci && npm run build
docker build -t extractor-frontend:latest .
cd ..

# 3. Start everything
docker compose up -d

# Neo4j browser:  http://localhost:7474  (neo4j / extractor123)
# API:            http://localhost:8080
# Frontend:       http://localhost:5173
```

## Local Development

### Prerequisites
- Java 21
- Maven 3.9+
- Node.js 20+
- Running Neo4j 5 instance (or use `docker compose up neo4j`)

### Backend

```bash
# Start Neo4j only
docker compose up -d neo4j

# Build and run API
mvn -pl extractor-api -am spring-boot:run
```

### Frontend

```bash
cd extractor-frontend
npm install
npm run dev    # http://localhost:5173  (proxied → :8080)
```

## Configuration

Edit `extractor-api/src/main/resources/application.yml`:

```yaml
extractor:
  repos:
    - name: my-monolith
      url: https://github.com/org/my-monolith.git
      branch: main
      buildTool: MAVEN
      localPath: /repos/my-monolith
  git:
    cloneOnStartup: true
    # sshKeyPath: /home/user/.ssh/id_rsa  # for SSH repos
```

## API Reference

### Ingestion

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/v1/ingestion/sync` | Trigger full sync (all repos) |
| `POST` | `/api/v1/ingestion/sync/{repoName}` | Trigger single-repo sync |
| `GET` | `/api/v1/ingestion/jobs/{jobId}` | Poll job status |
| `GET` | `/api/v1/ingestion/repos` | List configured repos |
| `POST` | `/api/v1/ingestion/repos` | Register a single repo at runtime |
| `DELETE` | `/api/v1/ingestion/repos/{name}` | Remove a repo from the list (keeps cloned files) |
| `POST` | `/api/v1/ingestion/scan-directory` | Scan a local directory for git repos and register all of them |

### Scanning a local directory

Use `POST /api/v1/ingestion/scan-directory` to point the extractor at a local filesystem path
that contains one or more git repositories.  Each discovered repo is registered and processed
through the same pipeline as GitHub repos — build tool is auto-detected from `pom.xml` (Maven)
or `build.gradle` (Gradle).

```jsonc
// Request body
{
  "directoryPath": "/local-repos",   // required — absolute path inside the container
  "buildTool": "MAVEN",              // fallback when auto-detection fails
  "branch": "main"                   // branch name recorded for each repo
}
```

```bash
# Optional: trigger ingestion immediately
POST /api/v1/ingestion/scan-directory?sync=true
```

**Docker Compose — exposing your local repos to the container**

The scan directory path must be accessible _inside_ the container.  Add a bind mount to
`docker-compose.yml` for the `extractor-api` service:

```yaml
volumes:
  - repo_clones:/repos
  - /path/to/your/local/projects:/local-repos:ro   # ← add this line
```

Then restart the container and use `/local-repos` as the `directoryPath`.

### Graph

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/v1/graph/nodes` | List nodes (`?repo=&type=&page=&size=`) |
| `GET` | `/api/v1/graph/node/{fqn}` | Get single node |
| `GET` | `/api/v1/graph/edges` | List edges (`?repo=&crossRepoOnly=`) |
| `GET` | `/api/v1/graph/callers` | Callers of a class (`?class=&crossRepoOnly=`) |
| `GET` | `/api/v1/graph/impact` | Transitive impact (`?class=&depth=`) |
| `GET` | `/api/v1/graph/shared-entities` | `@Entity` classes imported cross-repo |

## Running Tests

```bash
# Unit tests (no Docker needed)
mvn test -pl extractor-ingestion -Dtest="JavaSourceParserImplTest,MavenBuildParserImplTest,IngestionOrchestratorScanTest"

# Integration tests (requires Docker for Testcontainers)
mvn verify -pl extractor-graph,extractor-api
```
