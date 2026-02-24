/** Shared request helper — always hits /api (proxied by Vite dev server or Nginx in prod). */
const BASE = '/api/v1'

async function request<T>(path: string): Promise<T> {
  const res = await fetch(`${BASE}${path}`)
  if (!res.ok) throw new Error(`${res.status} ${res.statusText} (${path})`)
  return res.json() as Promise<T>
}

async function send<T>(
  method: string,
  path: string,
  body?: unknown
): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
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
}

function toQuery(params?: Record<string, string | boolean | undefined>): string {
  if (!params) return ''
  const q = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== '' && v !== null)
    .map(([k, v]) => `${k}=${encodeURIComponent(String(v))}`)
    .join('&')
  return q ? `?${q}` : ''
}
