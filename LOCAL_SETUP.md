# Running ModuleExtractor Locally (Without Docker)

This application has **no external infrastructure requirements** — no database, no message broker, nothing. Everything runs in-memory using JGraphT. You only need Java, Maven, Node.js, and Git.

---

## Table of Contents

1. [Prerequisites](#1-prerequisites)
2. [Clone the Repository](#2-clone-the-repository)
3. [Build the Backend](#3-build-the-backend)
4. [Configure Repositories to Scan](#4-configure-repositories-to-scan)
5. [Start the Backend](#5-start-the-backend)
6. [Start the Frontend](#6-start-the-frontend)
7. [Verify Everything Is Running](#7-verify-everything-is-running)
8. [Using the Application](#8-using-the-application)
9. [Running Tests](#9-running-tests)
10. [Troubleshooting](#10-troubleshooting)

---

## 1. Prerequisites

Install the following tools before starting. **All three are required.**

### Java 17 or higher

The backend is a Spring Boot 2.7.x application that requires Java 17+.

**Windows — download and install:**
- Go to https://adoptium.net/temurin/releases/
- Download the **JDK 21** (or 17) MSI installer for Windows x64
- Run the installer and follow the prompts (it sets `JAVA_HOME` and `PATH` automatically)

**Verify installation:**
```powershell
java -version
# Expected output: openjdk version "21.x.x" ...
```

> **Note:** If `java -version` still shows an old version after installing, open a **new** PowerShell/terminal window to pick up the updated `PATH`.

---

### Maven 3.9 or higher

Maven builds all six backend modules and is also used at **runtime** by the ingestion engine to resolve Maven project dependencies.

**Windows — download and install:**
1. Download the binary zip from https://maven.apache.org/download.cgi (e.g., `apache-maven-3.9.x-bin.zip`)
2. Extract to a folder, e.g., `C:\tools\apache-maven-3.9.x`
3. Add `C:\tools\apache-maven-3.9.x\bin` to your `PATH` environment variable:
   - Open **Start** → search **"Edit the system environment variables"**
   - Click **Environment Variables** → under **System variables** → select `Path` → **Edit**
   - Click **New** → paste the path → **OK** through all dialogs
4. Open a **new** terminal window

**Verify installation:**
```powershell
mvn -version
# Expected output: Apache Maven 3.9.x ...  Java version: 21.x.x
```

> **Important:** Maven must also be available at runtime (not just build-time), because the ingestion module uses the Maven Invoker API to parse `pom.xml` dependency trees of the repositories you scan.

---

### Node.js 20 or higher

Required only for the React frontend.

**Windows — download and install:**
- Go to https://nodejs.org/en/download
- Download the **LTS** (v20 or v22) Windows Installer (`.msi`)
- Run the installer — it installs both `node` and `npm`

**Verify installation:**
```powershell
node -version    # v20.x.x or higher
npm -version     # 10.x.x or higher
```

---

### Git

Required because the ingestion engine uses JGit to clone and pull repositories that you configure for scanning.

**Windows:** Git is typically already installed. Check with:
```powershell
git --version
# git version 2.x.x
```

If not installed, download from https://git-scm.com/download/win and run the installer.

---

## 2. Clone the Repository

If you haven't already cloned this project:

```powershell
git clone https://github.com/uhaseeb85/ModuleExtractor.git
cd ModuleExtractor
```

If you already have it, make sure you're in the project root:
```powershell
cd C:\Users\uhase\ModuleExtractor
```

---

## 3. Build the Backend

From the **project root** directory, run a full Maven build across all modules:

```powershell
mvn clean package -DskipTests
```

What this does:
- Compiles all 6 modules: `extractor-core`, `extractor-graph`, `extractor-ingestion`, `extractor-analysis`, `extractor-search`, `extractor-api`
- Packages `extractor-api` into a runnable fat JAR at `extractor-api/target/extractor-api-1.0.0-SNAPSHOT.jar`

**Expected output (last few lines):**
```
[INFO] BUILD SUCCESS
[INFO] Total time: ~30-60 seconds
```

> If you see `BUILD FAILURE`, check the [Troubleshooting](#10-troubleshooting) section.

---

## 4. Configure Repositories to Scan

Before starting the backend, tell it which Java repositories to analyse.

Open the file `extractor-api/src/main/resources/application.yml` in a text editor and edit the `extractor.repos` list:

```yaml
extractor:
  repos:
    - name: my-monolith                                      # Display name (any string)
      url: https://github.com/my-org/my-monolith.git        # HTTPS or SSH clone URL
      branch: main                                           # Branch to scan
      buildTool: MAVEN                                       # MAVEN or GRADLE
      localPath: C:/repos/my-monolith                       # Where to clone it on disk

    # Add more repos as needed:
    # - name: payment-service
    #   url: https://github.com/my-org/payment-service.git
    #   branch: develop
    #   buildTool: GRADLE
    #   localPath: C:/repos/payment-service

  git:
    clone-on-startup: true    # Set to false if the repo is already cloned at localPath
    # ssh-key-path: C:/Users/uhase/.ssh/id_rsa  # Only needed for SSH repos
```

**Key settings explained:**

| Setting | Description |
|---|---|
| `name` | Identifier shown in the UI and API responses |
| `url` | HTTPS (no auth needed for public repos) or `git@github.com:org/repo.git` for SSH |
| `branch` | Git branch to scan |
| `buildTool` | `MAVEN` or `GRADLE` — controls how dependency trees are resolved |
| `localPath` | Absolute path on your machine where the repo will be cloned |
| `clone-on-startup` | `true` = clone/pull on start; `false` = skip cloning (repo must already exist at `localPath`) |

> **Windows path tip:** Use forward slashes (`C:/repos/my-monolith`) or escaped backslashes (`C:\\repos\\my-monolith`) inside YAML.

**For private repos with SSH:**
1. Ensure your SSH key is registered with GitHub/GitLab
2. Set `ssh-key-path` to the absolute path of your private key (e.g., `C:/Users/uhase/.ssh/id_rsa`)

**If you just want to test without scanning a real repo**, you can leave `repos` as an empty list (`repos: []`) and manually trigger ingestion later via the API.

---

## 5. Start the Backend

Open a terminal in the **project root** and run:

```powershell
mvn spring-boot:run -pl extractor-api
```

**What to expect:**
- Spring Boot prints startup logs including the port it bound to
- If `clone-on-startup: true`, you will see log output as it clones your configured repositories
- Startup typically takes 10–30 seconds

**Successful startup looks like:**
```
INFO  o.s.b.web.embedded.tomcat.TomcatWebServer  : Tomcat started on port(s): 8081 (http)
INFO  c.e.ExtractorApiApplication                : Started ExtractorApiApplication in 4.5 seconds
```

The API is now available at **http://localhost:8081**

> Leave this terminal open — the backend runs in the foreground.

---

### Alternative: Run the JAR directly

If you prefer to run without Maven:

```powershell
java -jar extractor-api/target/extractor-api-1.0.0-SNAPSHOT.jar
```

This is equivalent but doesn't require Maven to be invoked at startup.

---

### Optional: Pass repo config as environment variables instead of editing YAML

You can override the YAML configuration using environment variables, which is useful if you don't want to modify the source file:

**PowerShell:**
```powershell
$env:EXTRACTOR_REPOS_0_NAME        = "my-monolith"
$env:EXTRACTOR_REPOS_0_URL         = "https://github.com/my-org/my-monolith.git"
$env:EXTRACTOR_REPOS_0_BRANCH      = "main"
$env:EXTRACTOR_REPOS_0_BUILDTOOL   = "MAVEN"
$env:EXTRACTOR_REPOS_0_LOCALPATH   = "C:/repos/my-monolith"
$env:EXTRACTOR_GIT_CLONEONSTARTUP  = "true"

mvn spring-boot:run -pl extractor-api
```

For multiple repos, increment the index: `EXTRACTOR_REPOS_1_NAME`, `EXTRACTOR_REPOS_1_URL`, etc.

---

## 6. Start the Frontend

Open a **second, separate terminal** (keep the backend terminal running) and navigate to the frontend folder:

```powershell
cd C:\Users\uhase\ModuleExtractor\extractor-frontend
```

**First time only — install dependencies:**
```powershell
npm install
```

This downloads all Node packages into `extractor-frontend/node_modules/`. It takes ~30 seconds and only needs to be done once (or when `package.json` changes).

**Start the development server:**
```powershell
npm run dev
```

**Expected output:**
```
  VITE v5.x.x  ready in xxx ms

  ➜  Local:   http://localhost:5173/
  ➜  Network: use --host to expose
```

The frontend is now available at **http://localhost:5173**

> The Vite dev server is pre-configured to proxy all `/api` requests to `http://localhost:8081`, so the frontend talks to your local backend automatically — no extra config needed.

---

## 7. Verify Everything Is Running

### Check the backend health endpoint

Open a new terminal or use your browser:

```powershell
Invoke-WebRequest -Uri http://localhost:8081/actuator/health | Select-Object -ExpandProperty Content
# Expected: {"status":"UP",...}
```

Or just open http://localhost:8081/actuator/health in your browser.

### Check the frontend

Open http://localhost:5173 in your browser. You should see the ModuleExtractor dashboard.

### Check configured repos

```powershell
Invoke-WebRequest -Uri http://localhost:8081/api/v1/ingestion/repos | Select-Object -ExpandProperty Content
```

---

## 8. Using the Application

### Triggering a Scan

After startup, if `clone-on-startup: true` was set, the backend automatically clones and scans all configured repos. You can also trigger scans manually:

**Scan all configured repos:**
```powershell
Invoke-WebRequest -Method POST -Uri http://localhost:8081/api/v1/ingestion/sync
```

**Scan a specific repo by name:**
```powershell
Invoke-WebRequest -Method POST -Uri http://localhost:8081/api/v1/ingestion/sync/my-monolith
```

**Poll the job status** (use the `jobId` returned from the above):
```powershell
Invoke-WebRequest -Uri http://localhost:8081/api/v1/ingestion/jobs/{jobId} | Select-Object -ExpandProperty Content
```

---

### API Quick Reference

#### Ingestion

| Method | URL | Description |
|---|---|---|
| `POST` | `/api/v1/ingestion/sync` | Trigger full sync (all repos) |
| `POST` | `/api/v1/ingestion/sync/{repoName}` | Trigger single-repo sync |
| `GET` | `/api/v1/ingestion/jobs/{jobId}` | Poll job status |
| `GET` | `/api/v1/ingestion/repos` | List configured repos |

#### Graph

| Method | URL | Description |
|---|---|---|
| `GET` | `/api/v1/graph/nodes` | List nodes (`?repo=&type=&page=&size=`) |
| `GET` | `/api/v1/graph/node/{fqn}` | Get single node by fully qualified name |
| `GET` | `/api/v1/graph/edges` | List edges (`?repo=&crossRepoOnly=`) |
| `GET` | `/api/v1/graph/callers` | Get callers of a class (`?class=&crossRepoOnly=`) |
| `GET` | `/api/v1/graph/impact` | Transitive impact analysis (`?class=&depth=`) |
| `GET` | `/api/v1/graph/shared-entities` | `@Entity` classes imported across repos |

> **Important:** The graph is **in-memory only**. It is rebuilt each time the app restarts or a sync is triggered. No data is persisted to disk.

---

## 9. Running Tests

All tests run without Docker or any external dependencies:

```powershell
# Run all unit tests (from project root)
mvn test

# Run tests for a specific module only
mvn test -pl extractor-graph
mvn test -pl extractor-ingestion
mvn test -pl extractor-analysis
```

---

## 10. Troubleshooting

### `BUILD FAILURE` during `mvn clean package`

**Check Java version:**
```powershell
java -version
mvn -version
```
Both must show Java 17 or higher. If `mvn -version` shows an older Java, Maven is picking up the wrong JDK. Set `JAVA_HOME` explicitly:
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.x.x.x-hotspot"
mvn clean package -DskipTests
```

---

### Backend fails to start: `Port 8081 already in use`

Another process is using port 8081. Either:

**Option A — Kill the process using port 8081:**
```powershell
netstat -ano | findstr :8081
# Note the PID from the last column
taskkill /PID <PID> /F
```

**Option B — Change the backend port:**
In `extractor-api/src/main/resources/application.yml`, change:
```yaml
server:
  port: 8082   # or any free port
```
Then also update the Vite proxy target in `extractor-frontend/vite.config.ts`:
```ts
proxy: {
  '/api': {
    target: 'http://localhost:8082',
  },
},
```

---

### Frontend fails to connect to backend (`Network Error` or `ERR_CONNECTION_REFUSED`)

- Confirm the backend is running and bound to port 8081
- Confirm you're running the frontend with `npm run dev` (not `npm run build`)
- The Vite proxy only works with the dev server — it does NOT apply to the production build

---

### Git clone fails during ingestion

**For HTTPS repos** (most common):
- Verify the URL is correct and the repo is publicly accessible
- For private repos, Git may prompt for credentials. Configure a credential helper:
  ```powershell
  git config --global credential.helper manager
  ```
  Then the Windows Credential Manager will store your credentials after the first prompt.

**For SSH repos:**
- Make sure `EXTRACTOR_GIT_SSH_KEY_PATH` or `git.ssh-key-path` in `application.yml` points to a valid private key
- Test SSH access separately: `ssh -T git@github.com`

---

### Gradle projects fail to parse dependencies

The Gradle Tooling API requires the project itself to have a valid Gradle wrapper (`gradlew`). If it doesn't:
1. Install Gradle separately: https://gradle.org/install/
2. Add Gradle to `PATH`

---

### Frontend shows blank page or JS errors in console

Run `npm install` again in the `extractor-frontend` directory — the `node_modules` folder may be missing or incomplete:
```powershell
cd extractor-frontend
Remove-Item -Recurse -Force node_modules
npm install
npm run dev
```

---

### How to stop the application

- **Backend:** Press `Ctrl+C` in the backend terminal
- **Frontend:** Press `Ctrl+C` in the frontend terminal
- Since the graph is **in-memory**, all data is lost when the backend stops. It is rebuilt from source on the next startup.
