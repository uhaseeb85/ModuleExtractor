**JAVA MONOLITH**

**MODULE EXTRACTOR**

Technical Design & Specification

Version 1.0 \| February 2026

*Prepared for GitHub Copilot-Assisted Development*

**Table of Contents**

**1. Overview**

**1.1 Purpose**

This document provides a complete technical design and phased implementation specification for the Java Monolith Module Extractor --- an application that scans multiple Java repositories, builds a comprehensive dependency graph, and helps engineers identify and safely extract independent modules.

The specification is written to serve as direct input to GitHub Copilot for phase-by-phase construction. Each section provides precise implementation guidance including class names, interfaces, data models, and technology choices.

**1.2 Problem Statement**

Java monoliths distributed across multiple repositories suffer from invisible coupling: shared common-model JARs, cross-repo Spring bean injection, shared database entities, and undocumented Feign client dependencies. Engineers cannot see the true dependency surface, making extraction risky, slow, and error-prone.

**1.3 Solution Summary**

The application ingests all configured repositories, parses Java source and build files, constructs a multi-layered dependency graph stored in Neo4j, exposes a REST search API, scores extraction candidates automatically, and generates detailed extraction reports. A React frontend provides interactive graph visualisation and natural language search.

**1.4 Guiding Principles**

-   Read-only analysis: the tool never modifies source repositories

-   Language-first: deeply Java-aware (Spring, JPA, Maven, Gradle)

-   Incremental: each phase delivers standalone, usable value

-   Copilot-friendly: every component has clearly named interfaces and data contracts

-   Extensible: designed for future language support and CI/CD integration

**2. System Architecture**

**2.1 High-Level Layer Overview**

  -------------------------------------------------------------------------------------------------------------------------
  **Layer**              **Responsibility**
  ---------------------- --------------------------------------------------------------------------------------------------
  Ingestion Layer        Clone/scan repos, parse Java source via AST, parse Maven/Gradle build files, analyse git history

  Graph Layer            Store and query the multi-level dependency graph in Neo4j using Spring Data Neo4j

  Analysis Engine        Compute coupling scores, detect Spring context, identify and rank extraction candidates

  API Layer              Spring Boot REST API exposing search, analysis, candidate scoring, and report generation

  Frontend               React + TypeScript + Cytoscape.js interactive graph explorer and search UI
  -------------------------------------------------------------------------------------------------------------------------

**2.2 Component Interaction**

+---------------------------------------------------------------------------------+
| **Component Flow**                                                              |
|                                                                                 |
| RepoScanner (JGit) \--\> ASTParser (JavaParser) \--\> GraphBuilder \--\> Neo4j  |
|                                                                                 |
| BuildFileParser (Maven/Gradle API) \--\> ArtifactGraph \--\> Neo4j              |
|                                                                                 |
| GitHistoryAnalyser (JGit) \--\> CO_CHANGES_WITH edges \--\> Neo4j               |
|                                                                                 |
| SpringContextAnalyser \--\> SpringBean nodes \--\> Neo4j                        |
|                                                                                 |
| JPAAnalyser \--\> Table nodes + READS/WRITES edges \--\> Neo4j                  |
|                                                                                 |
| Neo4j \--\> GraphQueryService \--\> REST API (Spring Boot) \--\> React Frontend |
|                                                                                 |
| EmbeddingService (OpenAI) \--\> pgvector (PostgreSQL) \--\> SemanticSearch      |
|                                                                                 |
| CandidateScoringService \--\> ExtractionReportGenerator \--\> JSON / PDF output |
+---------------------------------------------------------------------------------+

**2.3 Technology Stack**

  -----------------------------------------------------------------------------------------------------------------------------
  **Concern**                    **Technology**                          **Rationale**
  ------------------------------ --------------------------------------- ------------------------------------------------------
  Java Source Parsing            JavaParser 3.25.x                       Full Java AST, symbol resolution, annotation support

  Bytecode Analysis              ASM 9.x                                 Analyse dependency JARs without source code

  Maven Dependency Resolution    Maven Invoker API 3.x                   Native resolution of full dependency trees

  Gradle Dependency Resolution   Gradle Tooling API 8.x                  Programmatic Gradle dependency resolution

  Git History Analysis           JGit 6.x                                Cross-repo commit history and co-change analysis

  Graph Database                 Neo4j 5.x Community                     Cypher queries for deep graph traversal

  Graph OGM                      Spring Data Neo4j 7.x                   Entity mapping and repository pattern over Neo4j

  Vector Search                  pgvector extension on PostgreSQL 16     Semantic code similarity search

  Embeddings                     OpenAI text-embedding-3-small           Code and Javadoc vectorisation

  Backend Framework              Spring Boot 3.2 + Java 21               REST API, scheduling, dependency injection

  Frontend                       React 18 + TypeScript + Vite            Component-based SPA

  Graph Visualisation            Cytoscape.js 3.x                        Interactive dependency graph rendering

  UI Components                  shadcn/ui + Tailwind CSS                Accessible, consistent component library

  API State                      TanStack Query 5.x                      Server state management, polling, caching

  Containerisation               Docker + Docker Compose                 Reproducible development and deployment

  Testing                        JUnit 5 + Testcontainers + MockServer   Unit and integration test coverage
  -----------------------------------------------------------------------------------------------------------------------------

**2.4 Maven Multi-Module Repository Structure**

> monolith-extractor/ \<- parent POM
>
> extractor-core/ \<- domain models, interfaces, shared utils
>
> extractor-ingestion/ \<- repo scanning, AST parsing, build parsing
>
> extractor-graph/ \<- Neo4j entities, repositories, GraphBuilder
>
> extractor-analysis/ \<- scoring, Spring/JPA analysis, candidate detection
>
> extractor-search/ \<- vector embeddings, semantic search, NL query
>
> extractor-api/ \<- Spring Boot REST controllers, DTOs
>
> extractor-reports/ \<- extraction report generation (JSON + PDF)
>
> extractor-frontend/ \<- React application (Vite)
>
> extractor-docker/ \<- Docker Compose, Neo4j config, PostgreSQL init

**3. Graph Data Model**

**3.1 Neo4j Node Types**

Every node carries a universal id field (Long, auto-generated) plus the following properties:

  --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **Node Label**   **Key Properties**                                                                                  **Notes**
  ---------------- --------------------------------------------------------------------------------------------------- -----------------------------------------------------
  Repository       name, url, branch, localPath, buildTool, lastSyncSha                                                Top-level unit; one per configured repo

  Artifact         groupId, artifactId, version, type (JAR/POM/WAR), repoName                                          Maven/Gradle artifact coordinates

  Package          fullyQualifiedName, repoName, artifactId                                                            Java package (e.g. com.example.billing)

  Class            fullyQualifiedName, simpleName, classType (CLASS/INTERFACE/ENUM/ANNOTATION), isAbstract, repoName   Every .java source file

  Method           signature, name, returnType, visibility, isStatic, repoName, lineNumber                             All declared methods

  Field            name, fieldType, visibility, annotations (JSON array), repoName                                     Class-level field declarations

  Table            name, schema, repoName, isShared                                                                    Inferred from \@Entity, \@Table, native SQL queries

  SpringBean       beanName, beanType, scope (singleton/prototype), repoName                                           Beans discovered via Spring annotations

  FeignClient      name, url, serviceId, repoName                                                                      Inter-service HTTP clients
  --------------------------------------------------------------------------------------------------------------------------------------------------------------------------

**3.2 Neo4j Relationship Types**

  --------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **Relationship**   **From Node \--\> To Node**                                                                **Key Properties**
  ------------------ ------------------------------------------------------------------------------------------ ------------------------------------------------------
  CONTAINS           Repository\--\>Artifact, Artifact\--\>Package, Package\--\>Class, Class\--\>Method/Field   ---

  DEPENDS_ON         Artifact \--\> Artifact                                                                    scope (compile/test/provided), isTransitive, version

  IMPORTS            Class \--\> Class                                                                          isStatic, isWildcard

  CALLS              Method \--\> Method                                                                        callType (static/virtual/interface), lineNumber

  EXTENDS            Class \--\> Class                                                                          ---

  IMPLEMENTS         Class \--\> Class                                                                          ---

  ANNOTATED_WITH     Class/Method/Field \--\> Class (the annotation)                                            attributes (JSON string)

  INJECTS            Class \--\> SpringBean                                                                     injectionType (constructor/field/setter)

  READS              Method \--\> Table                                                                         queryType=SELECT

  WRITES             Method \--\> Table                                                                         queryType (INSERT/UPDATE/DELETE/MERGE)

  CALLS_REMOTE       Method \--\> FeignClient                                                                   ---

  CO_CHANGES_WITH    Class \--\> Class                                                                          commitCount, lastCommitDate, repos (JSON array)

  OWNED_BY           Table \--\> Repository                                                                     confidence (EXPLICIT/INFERRED)
  --------------------------------------------------------------------------------------------------------------------------------------------------------------------

**3.3 Neo4j Schema Constraints (run on startup)**

> CREATE CONSTRAINT repo_name IF NOT EXISTS FOR (r:Repository) REQUIRE r.name IS UNIQUE;
>
> CREATE CONSTRAINT artifact_coords IF NOT EXISTS FOR (a:Artifact) REQUIRE (a.groupId, a.artifactId, a.version) IS UNIQUE;
>
> CREATE CONSTRAINT class_fqn IF NOT EXISTS FOR (c:Class) REQUIRE c.fullyQualifiedName IS UNIQUE;
>
> CREATE CONSTRAINT table_name IF NOT EXISTS FOR (t:Table) REQUIRE t.name IS UNIQUE;
>
> CREATE INDEX class_repo IF NOT EXISTS FOR (c:Class) ON (c.repoName);
>
> CREATE INDEX method_repo IF NOT EXISTS FOR (m:Method) ON (m.repoName);

**4. Ingestion Layer (extractor-ingestion)**

**4.1 Configuration --- repos.yml**

> extractor:
>
> repos:
>
> \- name: payments-service
>
> url: https://github.com/org/payments-service.git
>
> branch: main
>
> buildTool: MAVEN \# MAVEN or GRADLE
>
> localPath: /data/repos/payments-service
>
> \- name: common-models
>
> url: https://github.com/org/common-models.git
>
> branch: main
>
> buildTool: MAVEN
>
> localPath: /data/repos/common-models
>
> git:
>
> sshKeyPath: /root/.ssh/id_rsa
>
> cloneOnStartup: true
>
> embeddings:
>
> openaiApiKey: \${OPENAI_API_KEY}
>
> batchSize: 50
>
> enabled: true

**4.2 Core Interfaces**

**RepoScanner**

> package com.extractor.ingestion;
>
> public interface RepoScanner {
>
> /\*\* Clone or pull repo to localPath. Returns HEAD commit SHA. \*/
>
> String syncRepo(RepoConfig config) throws IngestionException;
>
> /\*\* Returns all .java file paths changed since lastSha. Pass null for all files. \*/
>
> List\<Path\> getChangedFiles(RepoConfig config, String lastSha);
>
> }

**JavaSourceParser**

> public interface JavaSourceParser {
>
> /\*\* Parse a single .java file. Throws ParseException on unrecoverable failure. \*/
>
> ParseResult parse(Path javaFile, RepoConfig repo) throws ParseException;
>
> }
>
> public record ParseResult(
>
> ClassNode classNode, List\<MethodNode\> methods, List\<FieldNode\> fields,
>
> List\<ImportEdge\> imports, List\<CallEdge\> calls, List\<AnnotationEdge\> annotations
>
> ) {}

**BuildFileParser**

> public interface BuildFileParser {
>
> boolean supports(BuildTool buildTool);
>
> /\*\* Resolves full dependency tree for the module at projectDir. \*/
>
> List\<DependsOnEdge\> resolveDependencies(Path projectDir, RepoConfig repo)
>
> throws BuildParseException;
>
> }

**GitHistoryAnalyser**

> public interface GitHistoryAnalyser {
>
> /\*\* Analyse co-change patterns across all repos. Returns edges where commitCount \>= minCount. \*/
>
> List\<CoChangesEdge\> analyseCoChange(
>
> List\<RepoConfig\> repos, int lookbackDays, int minCoChangeCount),
>
> ) throws GitAnalysisException;
>
> }

**4.3 AST Parsing Strategy**

JavaSourceParserImpl uses JavaParser with CombinedTypeSolver configured with:

-   ReflectionTypeSolver --- for JDK types

-   JavaParserTypeSolver pointing to each repo\'s source root --- enables cross-repo type resolution

-   JarTypeSolver for each dependency JAR resolved from the build tool --- handles third-party types

This configuration allows a class in payments-service referencing a type in common-models to resolve correctly, enabling accurate cross-repo IMPORTS and CALLS edges.

**4.4 Spring Context Detection**

SpringContextAnalyser runs as a post-processing step after all classes are persisted to Neo4j:

-   \@Component, \@Service, \@Repository, \@Controller, \@RestController \--\> create SpringBean node

-   \@Autowired, \@Inject on fields/constructors \--\> create INJECTS relationship

-   \@Bean methods in \@Configuration classes \--\> create SpringBean with factory reference

-   \@FeignClient \--\> create FeignClient node; create CALLS_REMOTE from all interface methods

-   \@Value and \@ConfigurationProperties \--\> record as ExternalConfigDependency (stored as Class property)

**4.5 JPA / Database Analysis**

JPAAnalyser scans all \@Entity-annotated classes after initial graph build:

-   Reads \@Table(name=\...) or derives table name via JPA default naming strategy (class name to snake_case)

-   Creates Table node with OWNED_BY edge pointing to the owning repository

-   Scans \@Query annotations and native SQL strings (via regex) to create READS/WRITES edges from the query method

-   Sets isShared=true on any Table node where OWNED_BY points to more than one repository

**5. Analysis Engine (extractor-analysis)**

**5.1 Coupling Metrics**

  --------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **Metric**                        **Definition**
  --------------------------------- ----------------------------------------------------------------------------------------------------------------------------------------
  Afferent Coupling (Ca)            Number of classes outside the package/module that depend on classes inside it. Higher Ca = more dependents; changes break more things.

  Efferent Coupling (Ce)            Number of classes inside the package/module that depend on classes outside it. Higher Ce = more external dependencies.

  Instability (I)                   I = Ce / (Ca + Ce). Range 0-1. I=0 is maximally stable; I=1 is maximally unstable.

  Abstractness (A)                  Ratio of abstract classes and interfaces to total classes in the module. Range 0-1.

  Distance from Main Sequence (D)   D = \|A + I - 1\|. D \> 0.5 indicates the module is in the pain or useless zone.

  Cross-Repo Dependency Count       Number of DEPENDS_ON and IMPORTS edges that cross repository boundaries. Primary extraction difficulty indicator.
  --------------------------------------------------------------------------------------------------------------------------------------------------------------------------

**5.2 Candidate Detection --- Cypher Queries**

**Packages with zero inbound cross-repo class imports**

> MATCH (p:Package)\<-\[:CONTAINS\]-(c:Class)
>
> WHERE NOT EXISTS {
>
> MATCH (other:Class)-\[:IMPORTS\]-\>(c)
>
> WHERE other.repoName \<\> c.repoName
>
> }
>
> RETURN p, count(c) AS classCount ORDER BY classCount DESC

**Cohesion score per repo/package**

> MATCH (c1:Class)-\[:CALLS\]-\>(m:Method)\<-\[:CONTAINS\]-(c2:Class)
>
> WHERE c1.repoName = c2.repoName AND c1 \<\> c2
>
> WITH c1.repoName AS repo, count(\*) AS internalCalls
>
> MATCH (c:Class {repoName: repo})-\[:CALLS\]-\>(m2:Method)
>
> WHERE NOT (c)-\[:CONTAINS\]-\>(m2)
>
> WITH repo, internalCalls, count(\*) AS externalCalls
>
> RETURN repo, internalCalls, externalCalls,
>
> toFloat(internalCalls)/(internalCalls+externalCalls) AS cohesionScore

**5.3 Candidate Composite Score (0-100, higher = easier to extract)**

  -----------------------------------------------------------------------------------------------------------------------------------------------
  **Dimension**           **Weight**   **Calculation**
  ----------------------- ------------ ----------------------------------------------------------------------------------------------------------
  Data Independence       30%          100 if module owns all its tables exclusively; deduct 20 per shared table. 0 if all tables shared.

  Interface Cleanliness   25%          Percentage of cross-boundary dependencies landing on interfaces/abstract classes vs concrete classes.

  Coupling Ratio          20%          1 - (crossRepoDeps / totalDeps). More self-contained = higher score.

  Git Independence        15%          1 - (crossRepoCoChanges / totalCommits in lookback window). Fewer joint commits = higher score.

  Test Coverage Proxy     10%          Presence and scoping of test classes within the candidate boundary. 100 if dedicated test module exists.
  -----------------------------------------------------------------------------------------------------------------------------------------------

**5.4 Extraction Blocker Types**

  ----------------------------------------------------------------------------------------------------------------------------------
  **Blocker Type**             **Detection Logic**
  ---------------------------- -----------------------------------------------------------------------------------------------------
  Shared Entity                \@Entity class used by classes both inside AND outside the candidate boundary (cross-repo IMPORTS).

  Circular Dependency          Bidirectional CALLS or IMPORTS edges between candidate classes and external classes.

  Static Utility Coupling      Candidate directly references static methods of external concrete classes (not interfaces).

  Shared Spring Bean           A SpringBean with INJECTS relationships pointing to both candidate and non-candidate classes.

  Undeclared DB Ownership      Candidate method has READS/WRITES to a Table whose OWNED_BY points to a different repository.

  Missing Interface Boundary   External classes call methods on concrete candidate classes rather than on interfaces.
  ----------------------------------------------------------------------------------------------------------------------------------

**6. Search Layer (extractor-search)**

**6.1 Search Modes**

  ------------------------------------------------------------------------------------------------------------------------------------
  **Mode**                 **Description**
  ------------------------ -----------------------------------------------------------------------------------------------------------
  Structural Search        Direct Cypher-backed queries via named endpoint patterns (callers, impact, shared entities, etc.)

  Semantic Search          Cosine similarity over class embeddings stored in pgvector. Used for domain concept queries.

  Natural Language Query   LLM (OpenAI GPT-4o mini) translates plain English to Cypher or vector search. Schema-aware system prompt.
  ------------------------------------------------------------------------------------------------------------------------------------

**6.2 Structural Query Endpoints**

> GET /api/v1/search/callers?class=com.example.payment.PaymentService&crossRepoOnly=true
>
> GET /api/v1/search/impact?class=com.example.common.OrderEntity&depth=3
>
> GET /api/v1/search/shared-entities
>
> GET /api/v1/search/zero-inbound-packages
>
> GET /api/v1/search/cochange-pairs?minCount=5&days=90
>
> GET /api/v1/search/table-usage?table=ORDERS

**6.3 Embedding Strategy**

Each Class node is embedded using the following concatenated text input to OpenAI:

> String input = classNode.getFqn() + \"\\n\"
>
> \+ classNode.getJavadoc().orElse(\"\") + \"\\n\"
>
> \+ classNode.getMethods().stream()
>
> .map(m -\> m.getSignature() + \" \" + m.getJavadoc().orElse(\"\"))
>
> .collect(Collectors.joining(\"\\n\"));

PostgreSQL schema for embeddings (pgvector extension required):

> CREATE TABLE class_embeddings (
>
> id BIGSERIAL PRIMARY KEY,
>
> class_fqn TEXT NOT NULL UNIQUE,
>
> repo_name TEXT NOT NULL,
>
> embedding VECTOR(1536),
>
> updated_at TIMESTAMP DEFAULT NOW()
>
> );
>
> CREATE INDEX ON class_embeddings USING ivfflat (embedding vector_cosine_ops);

**6.4 QueryTranslationService Interface**

> public interface QueryTranslationService {
>
> /\*\*
>
> \* Translates NL query to a StructuralQuery, CypherQuery, or VectorQuery.
>
> \* Uses LLM with schema-aware system prompt + 15 few-shot examples.
>
> \* Only read-only Cypher permitted; validated with EXPLAIN before execution.
>
> \*/
>
> TranslatedQuery translate(String naturalLanguageQuery);
>
> }
>
> public sealed interface TranslatedQuery
>
> permits StructuralQuery, CypherQuery, VectorQuery {}

**7. REST API Specification (extractor-api)**

**7.1 Conventions**

-   Base URL: /api/v1

-   All responses: Content-Type: application/json

-   All list responses are paginated: ?page=0&size=20&sort=score,desc

-   Error response format: { \"error\": \"message\", \"code\": \"ERROR_CODE\", \"details\": {} }

**7.2 Ingestion Endpoints**

  ---------------------------------------------------------------------------------------------------------------------------------
  **Method + Path**                 **Description**                              **Response**
  --------------------------------- -------------------------------------------- --------------------------------------------------
  POST /ingestion/sync              Trigger full sync of all repos (async job)   { jobId, status, startedAt }

  POST /ingestion/sync/{repoName}   Sync a single repo (async job)               { jobId, status, startedAt }

  GET /ingestion/jobs/{jobId}       Poll sync job status                         { status, progress%, errors\[\], completedAt }

  GET /ingestion/repos              List repos and sync state                    \[ { name, lastSyncSha, syncedAt, nodeCount } \]
  ---------------------------------------------------------------------------------------------------------------------------------

**7.3 Search Endpoints**

  ------------------------------------------------------------------------------------------------------------------------------------
  **Method + Path**                      **Description**                            **Response**
  -------------------------------------- ------------------------------------------ --------------------------------------------------
  GET /search/query?q=\...               Natural language query                     { results: \[\], queryType, cypherUsed }

  GET /search/callers?class=FQN          Find callers of a class                    { callers: \[ClassSummary\], crossRepoCount }

  GET /search/impact?class=FQN&depth=3   Transitive impact of a class change        { directDeps, transitiveDeps, affectedRepos }

  GET /search/shared-entities            All shared \@Entity classes                \[ { className, ownerRepo, usedByRepos\[\] } \]

  GET /search/cochange?days=90&min=5     Co-change pairs                            \[ { class1, class2, commitCount, repos\[\] } \]

  GET /search/zero-inbound               Packages with no inbound cross-repo deps   \[ { packageFqn, repo, classCount, score } \]
  ------------------------------------------------------------------------------------------------------------------------------------

**7.4 Analysis Endpoints**

  -----------------------------------------------------------------------------------------------------------------------------------
  **Method + Path**                    **Description**                              **Response**
  ------------------------------------ -------------------------------------------- -------------------------------------------------
  GET /analysis/candidates             List top extraction candidates (paginated)   \[ CandidateSummary \]

  GET /analysis/candidates/{id}        Detailed candidate analysis                  CandidateDetail (see 7.5)

  GET /analysis/metrics/{repoName}     Coupling metrics for a repo                  { ca, ce, instability, abstractness, distance }

  GET /analysis/blockers?package=FQN   Extraction blockers for a package            \[ BlockerDetail \]

  POST /analysis/extract-report        Generate extraction report (async)           { reportId }

  GET /analysis/extract-report/{id}    Download report (JSON or ?format=pdf)        ExtractionReport
  -----------------------------------------------------------------------------------------------------------------------------------

**7.5 Key DTO Definitions**

**CandidateDetail**

> public record CandidateDetail(
>
> String id, String packageFqn, String repoName, int classCount,
>
> double compositeScore,
>
> double cohesionScore, double couplingRatio,
>
> double dataIndependenceScore, double interfaceCleanlinessScore,
>
> double gitIndependenceScore,
>
> List\<String\> classesInBoundary,
>
> List\<DependencyEdgeDto\> inboundDependencies,
>
> List\<DependencyEdgeDto\> outboundDependencies,
>
> List\<BlockerDto\> blockers,
>
> List\<TableDto\> ownedTables,
>
> List\<TableDto\> sharedTables
>
> ) {}

**ExtractionReport**

> public record ExtractionReport(
>
> String reportId, String packageFqn, Instant generatedAt,
>
> List\<String\> boundary, // FQNs of all classes inside the module
>
> List\<CutItem\> cutList, // deps that must be severed to extract
>
> List\<EntityConflict\> entityConflicts,
>
> ProposedApi proposedApi, // inferred public API surface
>
> StranglerFigPlan migrationPlan // ordered extraction steps
>
> ) {}

**BlockerDto**

> public record BlockerDto(
>
> BlockerType type, // SHARED_ENTITY, CIRCULAR_DEP, etc.
>
> String description,
>
> String sourceClass,
>
> String targetClass,
>
> String remediationAdvice
>
> ) {}

**8. Frontend Specification (extractor-frontend)**

**8.1 Technology Stack**

-   React 18 + TypeScript, Vite build tool

-   Cytoscape.js 3.x --- interactive dependency graph rendering

-   TanStack Query 5.x --- API state management with polling support

-   Tailwind CSS + shadcn/ui --- accessible component library

-   Recharts --- score breakdown charts and metrics visualisations

**8.2 Application Routes**

  ------------------------------------------------------------------------------------------------------------------------------------------
  **Route**              **Description**
  ---------------------- -------------------------------------------------------------------------------------------------------------------
  /dashboard             Summary cards: repo count, class count, top 5 candidates by score, recent sync status

  /repos                 Repository list with sync status, last sync time, per-repo node counts, trigger sync button

  /graph                 Full interactive dependency graph. Filters: repo, package, node type, relationship type. Click/lasso select.

  /search                Natural language search bar + structured filters. Results shown as graph subview and list.

  /candidates            Ranked list of extraction candidates with score breakdown bars. Click to drill down.

  /candidates/:id        Detailed candidate: boundary class list, cut list, blockers, tables, score breakdown chart.

  /report/:id            Rendered extraction report: boundary, cut list, entity conflicts, proposed API, migration plan. Download buttons.

  /settings              Repo YAML editor, sync schedule, OpenAI API key config, Neo4j connection status
  ------------------------------------------------------------------------------------------------------------------------------------------

**8.3 Graph Visualisation Behaviour**

-   Default layout: CoSE-Bilkent (handles large graphs, avoids overlap)

-   Node colour: one distinct colour per repository; legend displayed

-   Node size: proportional to afferent coupling (Ca) --- more dependents = larger node

-   Edge colour: blue = same-repo, red = cross-repo, orange = FeignClient, green = CO_CHANGES_WITH

-   Click a node: open side panel with FQN, repo, package, method list, and links to search queries

-   Right-click node: context menu --- \'Find callers\', \'Impact analysis\', \'Mark as candidate boundary\'

-   Lasso select: select multiple nodes to define custom extraction candidate boundary

-   Filter panel: checkboxes for repos, node types, relationship types (toggle visibility)

-   Performance: for graphs over 500 nodes use Cytoscape.js virtual rendering (only render visible viewport)

**8.4 Key React Components**

  -----------------------------------------------------------------------------------------------------------------------------------------------------------------
  **Component**             **Props (TypeScript)**                                                  **Responsibility**
  ------------------------- ----------------------------------------------------------------------- ---------------------------------------------------------------
  \<GraphCanvas /\>         nodes: GraphNode\[\], edges: GraphEdge\[\], onNodeClick: (n) =\> void   Cytoscape.js wrapper; handles layout, events, filtering

  \<CandidateScoreBar /\>   candidate: CandidateSummary                                             Recharts stacked bar of 5 score dimensions

  \<BlockerList /\>         blockers: BlockerDto\[\]                                                Colour-coded list with blocker type icon and remediation text

  \<CutListTable /\>        cuts: CutItem\[\]                                                       Sortable table of deps to sever with effort column

  \<SearchBar /\>           onResults: (r: SearchResult\[\]) =\> void                               Debounced 300ms NL search with type-ahead suggestions

  \<SyncStatusBadge /\>     repoName: string                                                        Polls /ingestion/jobs every 2s when sync is in progress

  \<MetricsCard /\>         metrics: CouplingMetrics                                                Displays Ca, Ce, I, A, D values with colour thresholds

  \<ExtractionReport /\>    report: ExtractionReport                                                Full report render with PDF download via browser print
  -----------------------------------------------------------------------------------------------------------------------------------------------------------------

**9. Phased Delivery Plan**

**Phase 1 --- Foundation & Visibility**

+---------------------------------------------------------------------------------------------------+
| **Phase 1 Goal**                                                                                  |
|                                                                                                   |
| After Phase 1: engineers can point the tool at all repos and see a complete, interactive          |
|                                                                                                   |
| inter-repo and class-level dependency graph. No scoring or analysis yet --- just full visibility. |
+---------------------------------------------------------------------------------------------------+

**Phase 1 Deliverables**

-   extractor-core: all enums (BuildTool, NodeType, RelationshipType, SyncStatus, ClassType)

-   extractor-core: all record DTOs (RepoConfig, ClassNode, MethodNode, FieldNode, ParseResult, all Edge types)

-   extractor-core: all interface definitions (RepoScanner, JavaSourceParser, BuildFileParser, GraphBuilder)

-   extractor-core: all exception classes (IngestionException, ParseException, BuildParseException, GraphException)

-   extractor-graph: Neo4j \@Node entity classes for Repository, Artifact, Package, Class, Method, Field

-   extractor-graph: Spring Data Neo4j \@Repository interfaces for each entity

-   extractor-graph: GraphBuilderImpl --- persists ParseResult objects to Neo4j in batches

-   extractor-ingestion: RepoScannerImpl using JGit (clone, pull, changed file detection)

-   extractor-ingestion: JavaSourceParserImpl using JavaParser with CombinedTypeSolver

-   extractor-ingestion: MavenBuildParserImpl and GradleBuildParserImpl

-   extractor-ingestion: IngestionOrchestrator \@Service --- orchestrates sync, parsing, graph build

-   extractor-api: Spring Boot main class + configuration; IngestionController; basic GraphController

-   extractor-docker: docker-compose.yml with Neo4j, PostgreSQL, app, frontend containers

-   extractor-frontend: Vite + React scaffold; GraphCanvas component; /dashboard and /graph routes

**Phase 1 Acceptance Criteria**

1.  POST /api/v1/ingestion/sync successfully clones and processes all configured repos

2.  GET /api/v1/ingestion/jobs/{jobId} reflects real-time progress and completion

3.  The /graph page renders all classes as nodes with IMPORTS, CALLS, EXTENDS edges visible

4.  Cross-repo edges are visually distinct (red) and filterable

5.  Clicking any node shows class name, repo, package, and method count

**Phase 2 --- Search & Query**

+------------------------------------------------------------------------------------------------+
| **Phase 2 Goal**                                                                               |
|                                                                                                |
| After Phase 2: engineers can ask questions about the codebase in plain English and structured  |
|                                                                                                |
| filters, receiving instant graph-backed answers including Spring, JPA, and git co-change data. |
+------------------------------------------------------------------------------------------------+

**Phase 2 Deliverables**

-   extractor-ingestion: SpringContextAnalyser (post-process step adding SpringBean and FeignClient nodes)

-   extractor-ingestion: JPAAnalyser (Table nodes, READS/WRITES edges, OWNED_BY, isShared flag)

-   extractor-ingestion: GitHistoryAnalyser (CO_CHANGES_WITH edges from JGit history)

-   extractor-search: EmbeddingService (OpenAI embeddings with batch processing and retry)

-   extractor-search: VectorSearchRepository (pgvector cosine similarity queries)

-   extractor-search: QueryTranslationService (LLM with schema-aware prompt, Cypher validation)

-   extractor-api: all /search/\* endpoints

-   extractor-frontend: /search page with NL search bar, results in graph subview and list panel

**Phase 2 Acceptance Criteria**

6.  \'What classes depend on PaymentService?\' returns accurate cross-repo callers

7.  \'Show me everything that touches the ORDERS table\' correctly uses JPA analysis

8.  Semantic search for \'billing\' returns relevant classes from all repos

9.  Co-change analysis shows at least one correct cross-repo co-change pair

**Phase 3 --- Candidate Scoring & Analysis**

+------------------------------------------------------------------------------------------+
| **Phase 3 Goal**                                                                         |
|                                                                                          |
| After Phase 3: the app automatically identifies and ranks the best extraction candidates |
|                                                                                          |
| with clear 5-dimension scoring breakdowns and detailed blocker lists.                    |
+------------------------------------------------------------------------------------------+

**Phase 3 Deliverables**

-   extractor-analysis: CouplingMetricsService (Ca, Ce, I, A, D calculation per repo/package)

-   extractor-analysis: CandidateDetectionService (Cypher-based candidate identification)

-   extractor-analysis: CandidateScoringService (composite 5-dimension score, 0-100)

-   extractor-analysis: BlockerDetectionService (all 6 blocker types)

-   extractor-api: /analysis/candidates/\* and /analysis/metrics/\* endpoints

-   extractor-frontend: /candidates list page with score bars; /candidates/:id detail page

**Phase 3 Acceptance Criteria**

10. Top 10 extraction candidates surfaced automatically with composite scores

11. Score breakdown across all 5 dimensions displayed as Recharts bar chart

12. All 6 blocker types detected and listed with remediation advice

13. Shared \@Entity classes correctly identified and cross-referenced with blockers

**Phase 4 --- Extraction Reports**

+------------------------------------------------------------------------------------+
| **Phase 4 Goal**                                                                   |
|                                                                                    |
| After Phase 4: engineers generate a complete, actionable extraction report for any |
|                                                                                    |
| candidate or custom lasso-selected boundary, downloadable as JSON and PDF.         |
+------------------------------------------------------------------------------------+

**Phase 4 Deliverables**

-   extractor-reports: ExtractionReportService (orchestrates report generation)

-   extractor-reports: ProposedApiInferrer (identifies inbound method calls to infer public API)

-   extractor-reports: StranglerFigPlanner (generates ordered migration steps)

-   extractor-reports: PDFReportRenderer (browser print via React component; server PDF optional via iText)

-   extractor-api: POST /analysis/extract-report (async), GET /analysis/extract-report/{id}

-   extractor-frontend: /report/:id full report page with download buttons (JSON and PDF)

-   extractor-frontend: lasso-select on /graph to define custom candidate boundary, send to report generator

**Phase 4 Acceptance Criteria**

14. Report for any scored candidate lists every class in the boundary

15. Cut list enumerates every cross-boundary dependency with description and effort estimate

16. Proposed API shows inferred public method signatures that external callers invoke

17. Strangler-fig migration plan provides a correct, ordered sequence of extraction steps

18. Reports downloadable as both JSON and PDF from the UI

**10. Testing Strategy**

**10.1 Unit Tests (per module)**

-   JavaSourceParserImplTest: assertions on classes with varying annotations, generics, lambdas, inner classes, records

-   MavenBuildParserImplTest: sample pom.xml files including multi-module, dependencyManagement, BOMs

-   CandidateScoringServiceTest: mock graph data; assert score formula for all 5 dimensions independently

-   BlockerDetectionServiceTest: one test method per blocker type with synthetic Neo4j data

-   QueryTranslationServiceTest: mock OpenAI; assert correct Cypher generated for 10 standard NL queries

**10.2 Integration Tests (Testcontainers)**

-   IngestionIntegrationTest: uses a real small Git repository at src/test/resources/sample-repos/

-   GraphBuilderIntegrationTest: verifies correct node and edge creation and counts in real Neo4j container

-   SearchIntegrationTest: verifies Cypher query correctness against pre-populated test graph

-   ApiIntegrationTest: Spring Boot \@SpringBootTest with MockMvc + Testcontainers for all endpoints

**10.3 Sample Test Repository**

Create a synthetic multi-repo Java project at src/test/resources/sample-repos/ with intentional patterns for testing all analysis features:

-   common-models: shared \@Entity classes --- UserEntity, OrderEntity (both used cross-repo)

-   payment-service: uses common-models; \@FeignClient to notification-service; clean billing package (good extraction candidate)

-   notification-service: depends on common-models only; no inbound cross-repo dependencies (best extraction candidate)

-   api-gateway: depends on both payment-service and notification-service (high coupling, poor candidate)

**11. Deployment**

**11.1 Docker Compose**

> services:
>
> neo4j:
>
> image: neo4j:5-community
>
> environment:
>
> NEO4J_AUTH: neo4j/extractor123
>
> NEO4J_PLUGINS: \'\[\"apoc\"\]\'
>
> volumes: \[ neo4j_data:/data \]
>
> ports: \[ \'7474:7474\', \'7687:7687\' \]
>
> postgres:
>
> image: pgvector/pgvector:pg16
>
> environment:
>
> POSTGRES_DB: extractor
>
> POSTGRES_USER: extractor
>
> POSTGRES_PASSWORD: extractor123
>
> volumes: \[ pg_data:/var/lib/postgresql/data \]
>
> ports: \[ \'5432:5432\' \]
>
> extractor-api:
>
> build: ../extractor-api
>
> environment:
>
> NEO4J_URI: bolt://neo4j:7687
>
> SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/extractor
>
> EXTRACTOR_REPOS_CONFIG: /config/repos.yml
>
> OPENAI_API_KEY: \${OPENAI_API_KEY}
>
> volumes: \[ ./config:/config, repo_data:/data/repos \]
>
> ports: \[ \'8080:8080\' \]
>
> depends_on: \[ neo4j, postgres \]
>
> extractor-frontend:
>
> build: ../extractor-frontend
>
> ports: \[ \'3000:80\' \]
>
> depends_on: \[ extractor-api \]
>
> volumes:
>
> neo4j_data: {}
>
> pg_data: {}
>
> repo_data: {}

**11.2 Environment Variables**

  ----------------------------------------------------------------------------------------------------------------------------------------------
  **Variable**                      **Description**
  --------------------------------- ------------------------------------------------------------------------------------------------------------
  OPENAI_API_KEY                    Required for semantic embeddings. Set to \'disabled\' to skip embedding generation and run without OpenAI.

  NEO4J_URI                         Neo4j Bolt URI. Default: bolt://localhost:7687

  NEO4J_USERNAME / NEO4J_PASSWORD   Neo4j credentials. Defaults: neo4j / extractor123

  SPRING_DATASOURCE_URL             PostgreSQL JDBC URL. Default: jdbc:postgresql://localhost:5432/extractor

  EXTRACTOR_REPOS_CONFIG            Absolute path to repos.yml file. Default: /config/repos.yml

  EXTRACTOR_GIT_SSH_KEY_PATH        Path to SSH private key for private repo access. Optional (uses HTTPS if not set).

  EXTRACTOR_INGESTION_CRON          Spring cron for scheduled re-sync. Default: 0 0 2 \* \* \* (2am daily)

  EXTRACTOR_EMBEDDINGS_ENABLED      true/false. Disabling skips OpenAI calls; semantic search unavailable. Default: true
  ----------------------------------------------------------------------------------------------------------------------------------------------

**12. GitHub Copilot Implementation Guidance**

**12.1 Build Order Within Phase 1**

Build files in this exact order to avoid unresolved compilation dependencies:

19. extractor-core: enums --- BuildTool, NodeType, RelationshipType, SyncStatus, ClassType, BlockerType

20. extractor-core: record DTOs --- RepoConfig, ClassNode, MethodNode, FieldNode, ParseResult, all Edge records

21. extractor-core: interface definitions --- RepoScanner, JavaSourceParser, BuildFileParser, GraphBuilder

22. extractor-core: exception classes --- IngestionException, ParseException, BuildParseException, GraphException

23. extractor-graph: Neo4j \@Node entity classes --- RepositoryEntity, ArtifactEntity, PackageEntity, ClassEntity, MethodEntity, FieldEntity

24. extractor-graph: Spring Data Neo4j \@Repository interfaces (one per entity)

25. extractor-graph: GraphBuilderImpl --- implements GraphBuilder, batch-saves ParseResult to Neo4j

26. extractor-ingestion: RepoScannerImpl using JGit

27. extractor-ingestion: JavaSourceParserImpl using JavaParser + CombinedTypeSolver

28. extractor-ingestion: MavenBuildParserImpl and GradleBuildParserImpl

29. extractor-ingestion: IngestionOrchestrator \@Service

30. extractor-api: Main application class, application.yml, IngestionController, basic GraphController

31. extractor-docker: docker-compose.yml

32. extractor-frontend: Vite project scaffold, GraphCanvas.tsx, /dashboard and /graph routes

**12.2 Copilot Prompt Patterns**

+------------------------------------------------------------------------------+
| **Pattern: Implementing a Core Interface**                                   |
|                                                                              |
| Prompt format to use with Copilot:                                           |
|                                                                              |
| \"Implement the following Java interface using \[Technology\]:               |
|                                                                              |
| \[Paste the full interface definition here\]                                 |
|                                                                              |
| Requirements:                                                                |
|                                                                              |
| \- Handle \[specific edge case 1\]                                           |
|                                                                              |
| \- Handle \[specific edge case 2\]                                           |
|                                                                              |
| \- Use \[specific library API details\]                                      |
|                                                                              |
| \- Throw \[ExceptionType\] when \[condition\]                                |
|                                                                              |
| \- Include Javadoc on public methods\"                                       |
|                                                                              |
| Always paste the full interface. Never ask Copilot to infer it from context. |
+------------------------------------------------------------------------------+

+-----------------------------------------------------------------------------------------------+
| **Pattern: Neo4j Entity Classes**                                                             |
|                                                                                               |
| Example prompt for ClassEntity:                                                               |
|                                                                                               |
| \"Create a Spring Data Neo4j 7 \@Node entity class named ClassEntity.                         |
|                                                                                               |
| Fields: id (Long \@Id \@GeneratedValue), fullyQualifiedName (String \@Property unique index), |
|                                                                                               |
| simpleName, classType (ClassType enum), isAbstract (boolean), repoName.                       |
|                                                                                               |
| Relationships (use \@Relationship): IMPORTS to List\<ClassEntity\> OUTGOING,                  |
|                                                                                               |
| EXTENDS to ClassEntity OUTGOING nullable, IMPLEMENTS to List\<ClassEntity\> OUTGOING,         |
|                                                                                               |
| ANNOTATED_WITH to List\<ClassEntity\> OUTGOING.                                               |
|                                                                                               |
| Include equals/hashCode based on fullyQualifiedName.\"                                        |
+-----------------------------------------------------------------------------------------------+

+----------------------------------------------------------------------------------+
| **Pattern: Cypher Query Methods**                                                |
|                                                                                  |
| Example prompt for a repository method:                                          |
|                                                                                  |
| \"Add a \@Query method to ClassEntityRepository that finds all ClassEntity nodes |
|                                                                                  |
| that are imported (via IMPORTS relationship) by classes in a different repoName. |
|                                                                                  |
| Method signature: List\<CrossRepoDependencyProjection\> findCrossRepoImporters(  |
|                                                                                  |
| \@Param(\'repoName\') String repoName);                                          |
|                                                                                  |
| CrossRepoDependencyProjection is an interface projection with: importerFqn(),    |
|                                                                                  |
| importerRepoName(), targetFqn() string methods.\"                                |
+----------------------------------------------------------------------------------+

**12.3 Critical Implementation Notes for Copilot**

-   JavaParser symbol resolution requires CombinedTypeSolver with ALL dependency JARs pre-resolved from Maven/Gradle. Always resolve the full dependency tree BEFORE parsing source files.

-   Spring Data Neo4j OGM lazy-loads relationships by default. For graph traversal queries, use explicit \@Query with Cypher MATCH patterns rather than navigating entity object graphs.

-   Cytoscape.js re-renders entirely on state changes. Wrap GraphCanvas in React.memo and use useMemo for node/edge arrays. For \> 500 nodes, implement viewport culling.

-   pgvector IVFFlat index requires \>= 100 vectors before it is effective. Drop the index before bulk embedding ingestion and recreate it after with CREATE INDEX CONCURRENTLY.

-   Maven Invoker API requires Maven to be installed in the Docker image. Add RUN apt-get install -y maven to the extractor-api Dockerfile.

-   The LLM query translation system prompt must embed the full Neo4j schema (all node labels, relationship types, and key properties). Update the prompt whenever schema changes.

-   All Cypher generated by the NL query translator must be validated with EXPLAIN {cypher} against Neo4j before execution. Reject and return an error for any write operations.

**13. Appendix**

**13.1 Parent POM Skeleton**

> \<project xmlns=\'http://maven.apache.org/POM/4.0.0\'\>
>
> \<modelVersion\>4.0.0\</modelVersion\>
>
> \<groupId\>com.extractor\</groupId\>
>
> \<artifactId\>monolith-extractor\</artifactId\>
>
> \<version\>1.0.0-SNAPSHOT\</version\>
>
> \<packaging\>pom\</packaging\>
>
> \<modules\>
>
> \<module\>extractor-core\</module\>
>
> \<module\>extractor-ingestion\</module\>
>
> \<module\>extractor-graph\</module\>
>
> \<module\>extractor-analysis\</module\>
>
> \<module\>extractor-search\</module\>
>
> \<module\>extractor-api\</module\>
>
> \<module\>extractor-reports\</module\>
>
> \</modules\>
>
> \<properties\>
>
> \<java.version\>21\</java.version\>
>
> \<spring-boot.version\>3.2.0\</spring-boot.version\>
>
> \<javaparser.version\>3.25.8\</javaparser.version\>
>
> \<neo4j-sdn.version\>7.3.0\</neo4j-sdn.version\>
>
> \<jgit.version\>6.8.0.202311291450-r\</jgit.version\>
>
> \<asm.version\>9.6\</asm.version\>
>
> \<testcontainers.version\>1.19.3\</testcontainers.version\>
>
> \</properties\>
>
> \<!\-- BOM imports, plugin management, compiler plugin 21 \--\>
>
> \</project\>

**13.2 Key Cypher Queries Quick Reference**

  ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **Query Purpose**                 **Cypher Pattern**
  --------------------------------- ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
  All cross-repo imports            MATCH (c1:Class)-\[:IMPORTS\]-\>(c2:Class) WHERE c1.repoName \<\> c2.repoName RETURN c1, c2

  Classes with zero inbound deps    MATCH (c:Class) WHERE NOT ()-\[:IMPORTS\]-\>(c) AND NOT ()-\[:CALLS\]-\>()-\[:CONTAINS\]-\>(c) RETURN c

  Shared \@Entity classes           MATCH (c:Class)-\[:ANNOTATED_WITH\]-\>(a:Class {simpleName:\'Entity\'}) WITH c MATCH (x:Class)-\[:IMPORTS\]-\>(c) WHERE x.repoName \<\> c.repoName RETURN c.fullyQualifiedName, collect(DISTINCT x.repoName) AS usedByRepos

  Transitive impact (depth 3)       MATCH path=(c:Class {fullyQualifiedName:\$fqn})\<-\[:IMPORTS\*1..3\]-(:Class) RETURN nodes(path), relationships(path)

  Co-change pairs (\>= N commits)   MATCH (c1:Class)-\[r:CO_CHANGES_WITH\]-\>(c2:Class) WHERE r.commitCount \>= \$minCount RETURN c1.fullyQualifiedName, c2.fullyQualifiedName, r.commitCount ORDER BY r.commitCount DESC

  Coupling metrics per package      MATCH (p:Package)\<-\[:CONTAINS\]-(c:Class)\<-\[:IMPORTS\]-(ext:Class) WHERE ext.repoName \<\> c.repoName WITH p, count(DISTINCT ext) AS ca MATCH (p)\<-\[:CONTAINS\]-(c2:Class)-\[:IMPORTS\]-\>(ext2:Class) WHERE ext2.repoName \<\> c2.repoName WITH p, ca, count(DISTINCT ext2) AS ce RETURN p.fullyQualifiedName, ca, ce, toFloat(ce)/(ca+ce) AS instability
  ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

**13.3 Glossary**

  ------------------------------------------------------------------------------------------------------------------------------------------------------------------
  **Term**                    **Definition**
  --------------------------- --------------------------------------------------------------------------------------------------------------------------------------
  Afferent Coupling (Ca)      Count of external classes that depend on a given module. Higher = more impact if module changes.

  Efferent Coupling (Ce)      Count of external classes a module depends on. Higher = more risk from external changes.

  Extraction Candidate        A cohesive package or class cluster with well-defined boundaries suitable for extraction as a module.

  Extraction Blocker          A dependency pattern (shared entity, circular dep, etc.) that prevents clean extraction without refactoring.

  Strangler Fig Pattern       Gradual extraction strategy where the new module grows to replace the monolith incrementally without a big-bang rewrite.

  Co-change                   Two or more files modified together in the same commit or in temporally proximate commits, indicating hidden coupling.

  Common Models Antipattern   A shared JAR containing domain \@Entity classes used across multiple services, creating tight data coupling and blocking extraction.

  FeignClient                 Spring Cloud declarative REST client. Cross-repo FeignClient usage is a key runtime coupling signal.
  ------------------------------------------------------------------------------------------------------------------------------------------------------------------

*End of Document \| Java Monolith Module Extractor \| v1.0*
