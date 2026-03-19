/** Shared request helper — always hits /api (proxied by Vite dev server or Nginx in prod). */
const BASE = '/api/v1'

async function request<T>(path: string, extraHeaders?: Record<string, string>): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    headers: extraHeaders,
  })
  if (!res.ok) {
    // Try to extract a structured error message from JSON response body
    const body = await res.json().catch(() => null) as Record<string, string> | null
    const detail = body?.detail ?? body?.error ?? `${res.status} ${res.statusText}`
    throw new Error(detail)
  }
  return res.json() as Promise<T>
}

async function send<T>(
  method: string,
  path: string,
  body?: unknown,
  extraHeaders?: Record<string, string>,
): Promise<T> {
  const headers: Record<string, string> = { ...(extraHeaders ?? {}) }
  if (body) headers['Content-Type'] = 'application/json'

  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: Object.keys(headers).length ? headers : undefined,
    body: body ? JSON.stringify(body) : undefined,
  })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw new Error(`${res.status} ${res.statusText}: ${text}`)
  }
  // 204 No Content
  if (res.status === 204) return undefined as unknown as T
  return res.json() as Promise<T>
}

// ── Types ─────────────────────────────────────────────────────────────

export interface GraphNodeResponse {
  id: string | null
  fqn: string
  simpleName: string
  classType: string
  repoName: string
  packageName: string
  isAbstract: boolean
  methodCount: number
}

export interface GraphEdgeResponse {
  sourceId: string | null
  sourceFqn: string
  targetId: string | null
  targetFqn: string
  relationshipType: string
  isCrossRepo: boolean
}

export interface SyncJobResponse {
  jobId: string
  status: string
  progressPercent: number
  currentRepo: string | null
  errors: string[]
  warnings: string[]
  startedAt: string | null
  completedAt: string | null
}

export interface RepoSummaryResponse {
  name: string
  url: string
  branch: string
  buildTool: string
  lastSyncSha: string | null
  syncedAt: string | null
  nodeCount: number
}

export interface CandidateSummaryResponse {
  packageFqn: string
  repoName: string
  classCount: number
  inboundDeps: number
  outboundDeps: number
  isolationScore: number
  stabilityScore: number
  sizeScore: number
  compositeScore: number
  recommendation: string
}

export interface CandidateDetailResponse extends CandidateSummaryResponse {
  blockers: string[]
  classNames: string[]
}

export interface ModuleRecommendationResponse {
  moduleName: string
  modulePackageRoot: string
  repoName: string
  packages: string[]
  classes: string[]
  totalClasses: number
  totalInboundDeps: number
  totalOutboundDeps: number
  avgCompositeScore: number
  minIsolationScore: number
  recommendation: string
  blockers: string[]
}

/** Payload for registering a new repository. */
export interface AddRepoRequest {
  name: string
  url: string
  branch: string
  buildTool: string
  /** Absolute path inside the container. Defaults to /repos/<name> if blank. */
  localPath: string
}

// ── Scaffold types ────────────────────────────────────────────────────

export interface ProjectTreeNodeResponse {
  name: string
  path: string
  type: 'DIRECTORY' | 'FILE'
  children: ProjectTreeNodeResponse[] | null
  hasContent: boolean
  sourceRef: string | null
}

export interface ScaffoldPreviewResponse {
  moduleName: string
  modulePackageRoot: string
  repoName: string
  tree: ProjectTreeNodeResponse
  springContextFiles: string[]
  totalFiles: number
}

export interface FileContentResponse {
  filePath: string
  content: string
}

// ── AI types ──────────────────────────────────────────────────────────

export interface AiModelResponse {
  id: string
  name: string
  description: string | null
  contextLength: number
  promptPrice: string | null
  completionPrice: string | null
}

export interface AiAnalysisRequest {
  model: string
  moduleName?: string
  groupDepth?: number
  minScore?: number
}

export interface AiAnalysisResponse {
  content: string | null
  modelUsed: string | null
  promptTokens: number
  completionTokens: number
  error: string | null
}

export interface AiHealthResponse {
  available: boolean
  latencyMs: number
  error: string | null
}

export interface AiPipelineResponse {
  boundaries: AiAnalysisResponse
  migration: AiAnalysisResponse
  contexts: AiAnalysisResponse
}

/** Payload for scanning a local directory for Git repositories. */
export interface ScanDirectoryRequest {
  /** Absolute path to scan (may be a single repo or a directory of repos). */
  directoryPath: string
  /** Fallback build tool when auto-detection fails. Defaults to MAVEN. */
  buildTool: string
  /** Branch name to record for each discovered repo. Defaults to main. */
  branch: string
}

// ── API calls ─────────────────────────────────────────────────────────

export const api = {
  getNodes: (params?: { repo?: string; type?: string }) =>
    request<GraphNodeResponse[]>(
      `/graph/nodes${toQuery(params)}`
    ),

  getEdges: (params?: { repo?: string; crossRepoOnly?: boolean }) =>
    request<GraphEdgeResponse[]>(
      `/graph/edges${toQuery(params)}`
    ),

  getNode: (fqn: string) =>
    request<GraphNodeResponse>(`/graph/node/${encodeURIComponent(fqn)}`),

  getCallers: (fqn: string, crossRepoOnly = false) =>
    request<GraphNodeResponse[]>(
      `/graph/callers?class=${encodeURIComponent(fqn)}&crossRepoOnly=${crossRepoOnly}`
    ),

  getImpact: (fqn: string, depth = 3) =>
    request<GraphNodeResponse[]>(
      `/graph/impact?class=${encodeURIComponent(fqn)}&depth=${depth}`
    ),

  getSharedEntities: () =>
    request<GraphNodeResponse[]>('/graph/shared-entities'),

  getRepos: () =>
    request<RepoSummaryResponse[]>('/ingestion/repos'),

  addRepo: (data: AddRepoRequest, triggerSync = false) =>
    send<{ repo: string; syncJobId?: string }>(
      'POST',
      `/ingestion/repos${triggerSync ? '?sync=true' : ''}`,
      data
    ),

  scanDirectory: (data: ScanDirectoryRequest, triggerSync = false) =>
    send<{ registered: string[]; syncJobId?: string; message?: string }>(
      'POST',
      `/ingestion/scan-directory${triggerSync ? '?sync=true' : ''}`,
      data
    ),

  removeRepo: (repoName: string) =>
    send<void>('DELETE', `/ingestion/repos/${encodeURIComponent(repoName)}`),

  triggerFullSync: () =>
    send<SyncJobResponse>('POST', '/ingestion/sync'),

  triggerRepoSync: (repoName: string) =>
    send<SyncJobResponse>('POST', `/ingestion/sync/${encodeURIComponent(repoName)}`),

  getJobStatus: (jobId: string) =>
    request<SyncJobResponse>(`/ingestion/jobs/${jobId}`),

  getCandidates: (minClasses = 2, limit = 100) =>
    request<CandidateSummaryResponse[]>(
      `/analysis/candidates${toQuery({ minClasses: String(minClasses), limit: String(limit) })}`
    ),

  getCandidateDetail: (packageFqn: string, repoName?: string) =>
    request<CandidateDetailResponse>(
      `/analysis/candidates/${encodeURIComponent(packageFqn)}${repoName ? toQuery({ repoName }) : ''}`
    ),

  getRecommendations: (groupDepth = 4, minScore = 0.4) =>
    request<ModuleRecommendationResponse[]>(
      `/analysis/recommendations${toQuery({ groupDepth: String(groupDepth), minScore: String(minScore) })}`
    ),

  // ── Scaffold ────────────────────────────────────────────────────────

  getScaffoldPreview: (moduleName: string, modulePackageRoot: string, repoName: string, groupDepth = 4, minScore = 0.4) =>
    request<ScaffoldPreviewResponse>(
      `/scaffold/preview${toQuery({
        moduleName,
        modulePackageRoot,
        repoName,
        groupDepth: String(groupDepth),
        minScore: String(minScore),
      })}`
    ),

  getScaffoldFile: (moduleName: string, repoName: string, filePath: string) =>
    request<FileContentResponse>(
      `/scaffold/file${toQuery({ moduleName, repoName, filePath })}`
    ),

  exportScaffold: async (moduleName: string, modulePackageRoot: string, repoName: string, groupDepth = 4, minScore = 0.4) => {
    const res = await fetch(`${BASE}/scaffold/export${toQuery({
      moduleName,
      modulePackageRoot,
      repoName,
      groupDepth: String(groupDepth),
      minScore: String(minScore),
    })}`, { method: 'POST' })
    if (!res.ok) throw new Error(`${res.status} ${res.statusText}`)
    return res.blob()
  },

  // ── AI ──────────────────────────────────────────────────────────────

  aiHealthCheck: (apiKey: string) =>
    request<AiHealthResponse>('/ai/health', { 'X-OpenRouter-Key': apiKey }),

  getAiModels: (apiKey: string) => {
    const res = fetch(`${BASE}/ai/models`, {
      headers: { 'X-OpenRouter-Key': apiKey },
    })
    return res.then(r => {
      if (!r.ok) throw new Error(`${r.status} ${r.statusText}`)
      return r.json() as Promise<AiModelResponse[]>
    })
  },

  aiRefineBoundaries: (apiKey: string, body: AiAnalysisRequest) =>
    send<AiAnalysisResponse>('POST', '/ai/refine-boundaries', body, { 'X-OpenRouter-Key': apiKey }),

  aiMigrationPlan: (apiKey: string, body: AiAnalysisRequest) =>
    send<AiAnalysisResponse>('POST', '/ai/migration-plan', body, { 'X-OpenRouter-Key': apiKey }),

  aiBoundedContexts: (apiKey: string, body: AiAnalysisRequest) =>
    send<AiAnalysisResponse>('POST', '/ai/bounded-contexts', body, { 'X-OpenRouter-Key': apiKey }),

  aiOptimiseWeights: (apiKey: string, body: AiAnalysisRequest) =>
    send<AiAnalysisResponse>('POST', '/ai/optimise-weights', body, { 'X-OpenRouter-Key': apiKey }),

  aiRunPipeline: (apiKey: string, body: AiAnalysisRequest) =>
    send<AiPipelineResponse>('POST', '/ai/pipeline', body, { 'X-OpenRouter-Key': apiKey }),
}

function toQuery(params?: Record<string, string | boolean | undefined>): string {
  if (!params) return ''
  const q = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== '' && v !== null)
    .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
    .join('&')
  return q ? `?${q}` : ''
}
