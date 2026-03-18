# Java Monolith Module Extractor

A tool for analysing Java monoliths and mapping cross-module dependencies into an in-memory graph — Phase 1 (Foundation & Visibility).

> **No external infrastructure required.** The dependency graph is stored in-memory using [JGraphT](https://jgrapht.org/). Just start the app and point it at a repo.

## Modules

| Module | Purpose |
|---|---|
| `extractor-core` | Enums, model records, interfaces, exceptions — no Spring |
| `extractor-graph` | JGraphT-backed `GraphStore`, entities, repositories, `GraphBuilderImpl` |
| `extractor-ingestion` | JGit scanner, JavaParser, Maven/Gradle build parsers, orchestrator |
| `extractor-api` | Spring Boot REST API (`/api/v1/graph`, `/api/v1/ingestion`) |
| `extractor-analysis` | Coupling metrics, candidate scoring |
| `extractor-search` | Phase 3 stub — semantic/vector search |
| `extractor-frontend` | React 18 + Vite + Cytoscape.js visualisation |

---

## Running Locally (Recommended)

### Prerequisites
- Java 17+
- Maven 3.9+
- Node.js 20+

### 1 — Build

```bash
cd C:\Users\uhase\ModuleExtractor   # or wherever you cloned it
mvn clean package -DskipTests
```

### 2 — Start the backend

```bash
mvn spring-boot:run -pl extractor-api
```

API is available at **http://localhost:8081**

Verify it started:

```bash
curl http://localhost:8081/actuator/health
# {"status":"UP"}
```

### 3 — Start the frontend

```bash
cd extractor-frontend
npm install        # first time only
npm run dev
```

Frontend is available at **http://localhost:5173** (API calls are proxied to `:8081`)

---

## Running via Docker Compose

```bash
# 1. Build the API jar and image
mvn clean package -DskipTests
docker build -t extractor-api:latest -f extractor-api/Dockerfile .

# 2. Build the frontend image
cd extractor-frontend
docker build -t extractor-frontend:latest .
cd ..

# 3. Start everything
docker compose up -d

# API:       http://localhost:8081
# Frontend:  http://localhost:5173
```

---

## Configuring Repositories to Scan

Edit `extractor-api/src/main/resources/application.yml` and add repos under `extractor.repos`:

```yaml
extractor:
  repos:
    - name: my-monolith
      url: https://github.com/org/my-monolith.git
      branch: main
      buildTool: MAVEN
      localPath: /repos/my-monolith
  git:
    cloneOnStartup: true   # set false if already cloned locally
    # sshKeyPath: ~/.ssh/id_rsa  # for SSH repos
```

Or trigger an ad-hoc scan via the API after startup:

```bash
# Trigger full sync of all configured repos
curl -X POST http://localhost:8081/api/v1/ingestion/sync

# Poll job status
curl http://localhost:8081/api/v1/ingestion/jobs/{jobId}
```

> The graph is **in-memory only** — it is rebuilt each time the app starts or a sync is triggered. There is no database to set up or maintain.

---

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

---

## Running Tests

```bash
# All unit tests (no Docker or database needed)
mvn test -DskipTests=false

# Targeted unit tests
mvn test -pl extractor-ingestion -Dtest="JavaSourceParserImplTest,MavenBuildParserImplTest,IngestionOrchestratorScanTest"

# Integration tests (requires Docker for Testcontainers)
mvn verify -pl extractor-graph,extractor-api

# Specific module
mvn test -pl extractor-graph
mvn test -pl extractor-ingestion
```
