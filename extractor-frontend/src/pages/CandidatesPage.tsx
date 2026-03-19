import { useState } from 'react'
import { useQuery, useMutation } from '@tanstack/react-query'
import {
  api,
  type ModuleRecommendationResponse,
  type ProjectTreeNodeResponse,
  type ScaffoldPreviewResponse,
  type AiAnalysisRequest,
  type AiAnalysisResponse,
} from '../api/client'
import { getApiKey, getModelId, hasAiConfig } from '../api/ai-config'
import {
  Search, RefreshCw, AlertTriangle, Package, ChevronDown,
  ChevronRight, Layers, ArrowDownLeft, ArrowUpRight, Box,
  FolderTree, Download, FileText, Shield,
  Sparkles, Brain, Map, Scale, Loader2,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'
import FileTreeNode from '@/components/FileTreeNode'
import FilePreview from '@/components/FilePreview'

// -- Score ring ----------------------------------------------------------------
function Ring({ score, size = 40 }: { score: number; size?: number }) {
  const r    = (size - 6) / 2
  const circ = 2 * Math.PI * r
  const clamp = Math.max(0, Math.min(1, score))
  const color =
    clamp >= 0.65 ? 'hsl(var(--primary))' :
    clamp >= 0.45 ? '#f59e0b' : '#6b7280'

  return (
    <svg width={size} height={size} style={{ transform: 'rotate(-90deg)' }} className="shrink-0">
      <circle
        cx={size / 2} cy={size / 2} r={r}
        fill="none" stroke="currentColor" strokeWidth={3} opacity={0.12}
      />
      <circle
        cx={size / 2} cy={size / 2} r={r}
        fill="none" stroke={color} strokeWidth={3}
        strokeDasharray={`${circ * clamp} ${circ}`}
        strokeLinecap="round"
      />
    </svg>
  )
}

// -- Score bar -----------------------------------------------------------------
function ScoreBar({ label, value }: { label: string; value: number }) {
  const pct = Math.round(Math.max(0, Math.min(1, value)) * 100)
  const color =
    pct >= 65 ? 'bg-emerald-500' :
    pct >= 45 ? 'bg-amber-500'   : 'bg-gray-400'
  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between">
        <span className="text-xs text-muted-foreground">{label}</span>
        <span className="text-xs font-semibold tabular-nums">{pct}%</span>
      </div>
      <div className="h-1.5 rounded-full neu-inset-sm overflow-hidden">
        <div className={cn('h-full rounded-full transition-all duration-500', color)}
          style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}

// -- Recommendation badge ------------------------------------------------------
function RecBadge({ text }: { text: string }) {
  const lower = text?.toLowerCase() ?? ''
  if (lower.includes('extract now') || lower.includes('extract_now'))
    return <Badge className="text-[10px] bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300 border-emerald-200 dark:border-emerald-800 hover:bg-emerald-100">Extract Now</Badge>
  if (lower.includes('refactor'))
    return <Badge className="text-[10px] bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300 border-amber-200 dark:border-amber-800 hover:bg-amber-100">With Refactoring</Badge>
  return <Badge variant="secondary" className="text-[10px]">{text}</Badge>
}

// -- Module list item ----------------------------------------------------------
function ModuleItem({
  mod, selected, onClick,
}: {
  mod: ModuleRecommendationResponse; selected: boolean; onClick: () => void
}) {
  const score = mod.avgCompositeScore ?? 0
  return (
    <button
      onClick={onClick}
      className={cn(
        'w-full flex items-center gap-3 border-b px-4 py-3 text-left',
        'transition-colors hover:bg-accent/60',
        selected
          ? 'bg-accent border-l-2 border-l-primary'
          : 'border-l-2 border-l-transparent',
      )}
    >
      <Ring score={score} size={38} />
      <div className="flex-1 min-w-0">
        <div className="flex flex-wrap items-center gap-1.5 mb-0.5">
          <span className="font-medium text-sm text-foreground truncate max-w-40">
            {mod.moduleName}
          </span>
          <RecBadge text={mod.recommendation} />
        </div>
        <p className="text-xs text-muted-foreground font-mono truncate">
          {mod.modulePackageRoot}
        </p>
        <p className="text-xs text-muted-foreground/70 mt-0.5">
          {mod.totalClasses} classes � {mod.repoName}
        </p>
      </div>
      <span className="shrink-0 text-xs font-semibold tabular-nums text-muted-foreground">
        {Math.round(score * 100)}
      </span>
    </button>
  )
}

// -- Tab types -----------------------------------------------------------------
type DetailTab = 'overview' | 'structure' | 'spring' | 'ai'

// -- AI result panel -----------------------------------------------------------
function AiResultPanel({ result, loading }: { result: AiAnalysisResponse | null; loading: boolean }) {
  if (loading) {
    return (
      <div className="flex items-center justify-center h-full gap-2 text-muted-foreground text-sm">
        <Loader2 className="h-4 w-4 animate-spin" /> Running AI analysis...
      </div>
    )
  }
  if (!result) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-center px-6">
        <div className="rounded-full neu-flat p-4 mb-4">
          <Sparkles className="h-8 w-8 text-muted-foreground/50" />
        </div>
        <p className="text-sm font-medium text-muted-foreground">No AI analysis yet</p>
        <p className="mt-1 text-xs text-muted-foreground/60">
          Use the AI buttons in the toolbar to run an analysis on the selected module
        </p>
      </div>
    )
  }
  if (result.error) {
    return (
      <div className="flex flex-col items-center justify-center h-full gap-3 text-sm px-6 text-center">
        <div className="rounded-full bg-destructive/10 p-3">
          <AlertTriangle className="h-5 w-5 text-destructive" />
        </div>
        <p className="font-medium text-foreground">Analysis failed</p>
        <p className="text-xs text-muted-foreground max-w-md">{result.error}</p>
      </div>
    )
  }
  return (
    <div className="h-full overflow-y-auto p-6 space-y-4">
      {/* Token usage */}
      <div className="flex items-center gap-3 text-xs text-muted-foreground">
        <Badge variant="secondary" className="text-[10px]">{result.modelUsed}</Badge>
        {(result.promptTokens != null || result.completionTokens != null) && (
          <span>
            {result.promptTokens ?? 0} + {result.completionTokens ?? 0} tokens
          </span>
        )}
      </div>
      {/* Content */}
      <div className="rounded-xl neu-raised-sm p-4">
        <pre className="whitespace-pre-wrap text-xs text-foreground font-mono leading-relaxed">
          {result.content}
        </pre>
      </div>
    </div>
  )
}

// -- Detail panel (tabbed) -----------------------------------------------------
function DetailPanel({
  mod,
  params,
  aiResult,
  aiLoading,
}: {
  mod: ModuleRecommendationResponse
  params: { groupDepth: number; minScore: number }
  aiResult: AiAnalysisResponse | null
  aiLoading: boolean
}) {
  const [tab, setTab] = useState<DetailTab>('overview')
  const [selectedFile, setSelectedFile] = useState<ProjectTreeNodeResponse | null>(null)

  const { data: scaffold, isLoading: scaffoldLoading, isError: scaffoldError, error: scaffoldErrorObj } = useQuery({
    queryKey: ['scaffold-preview', mod.moduleName, mod.modulePackageRoot, mod.repoName, params.groupDepth, params.minScore],
    queryFn: () => api.getScaffoldPreview(mod.moduleName, mod.modulePackageRoot, mod.repoName, params.groupDepth, params.minScore),
    retry: 1,
  })

  const exportMutation = useMutation({
    mutationFn: () => api.exportScaffold(mod.moduleName, mod.modulePackageRoot, mod.repoName, params.groupDepth, params.minScore),
    onSuccess: (blob) => {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `${mod.moduleName}.zip`
      a.click()
      URL.revokeObjectURL(url)
    },
  })

  const score = mod.avgCompositeScore ?? 0
  const pct   = Math.round(score * 100)

  const tabs: { key: DetailTab; label: string; icon: typeof Box }[] = [
    { key: 'overview',  label: 'Overview',          icon: Box },
    { key: 'structure', label: 'Project Structure', icon: FolderTree },
    { key: 'spring',    label: 'Spring Context',    icon: Shield },
    { key: 'ai',        label: 'AI Analysis',       icon: Sparkles },
  ]

  return (
    <div className="h-full flex flex-col overflow-hidden">
      {/* Header + CTA */}
      <div className="shrink-0 border-b px-6 py-4 space-y-3">
        <div className="flex items-start gap-4">
          <div className="relative">
            <Ring score={score} size={56} />
            <div className="absolute inset-0 flex items-center justify-center">
              <span className={cn(
                'text-xs font-bold',
                pct >= 65 ? 'text-emerald-600 dark:text-emerald-400' :
                pct >= 45 ? 'text-amber-600 dark:text-amber-400'   : 'text-gray-500',
              )}>
                {pct}
              </span>
            </div>
          </div>
          <div className="flex-1 min-w-0">
            <h2 className="text-lg font-bold text-foreground">{mod.moduleName}</h2>
            <p className="font-mono text-xs text-muted-foreground">{mod.modulePackageRoot}</p>
            <div className="mt-1.5 flex flex-wrap gap-2">
              <RecBadge text={mod.recommendation} />
              <Badge variant="secondary" className="text-xs">{mod.repoName}</Badge>
              <Badge variant="secondary" className="text-xs">{mod.totalClasses} classes</Badge>
            </div>
          </div>
          <Button
            size="sm"
            className="gap-1.5 shrink-0"
            disabled={exportMutation.isPending}
            onClick={() => exportMutation.mutate()}
          >
            <Download className={cn('h-3.5 w-3.5', exportMutation.isPending && 'animate-spin')} />
            {exportMutation.isPending ? 'Exporting...' : 'Export Module'}
          </Button>
        </div>

        {/* Tabs */}
        <div className="flex gap-1">
          {tabs.map(({ key, label, icon: Icon }) => (
            <button
              key={key}
              onClick={() => { setTab(key); setSelectedFile(null) }}
              className={cn(
                'flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-xs font-medium transition-colors',
                tab === key
                  ? 'bg-primary/10 text-primary'
                  : 'text-muted-foreground hover:bg-accent/60',
              )}
            >
              <Icon className="h-3.5 w-3.5" />
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* Tab content */}
      <div className="flex-1 overflow-hidden">
        {tab === 'overview'  && <OverviewTab mod={mod} />}
        {tab === 'structure' && (
          <StructureTab
            scaffold={scaffold ?? null}
            loading={scaffoldLoading}
            error={scaffoldError ? scaffoldErrorObj : null}
            selectedFile={selectedFile}
            onSelectFile={setSelectedFile}
            moduleName={mod.moduleName}
            repoName={mod.repoName}
          />
        )}
        {tab === 'spring'    && <SpringTab scaffold={scaffold ?? null} loading={scaffoldLoading} error={scaffoldError ? scaffoldErrorObj : null} />}
        {tab === 'ai'        && <AiResultPanel result={aiResult} loading={aiLoading} />}
      </div>
    </div>
  )
}

// -- Overview tab --------------------------------------------------------------
function OverviewTab({ mod }: { mod: ModuleRecommendationResponse }) {
  const [showPkg,  setShowPkg]  = useState(false)
  const [showCls,  setShowCls]  = useState(false)

  return (
    <div className="h-full overflow-y-auto">
      <div className="space-y-5 p-6">
        {/* Stats grid */}
        <div className="grid grid-cols-3 gap-3">
          {[
            { label: 'Classes',  value: mod.totalClasses,      icon: Box          },
            { label: 'Inbound',  value: mod.totalInboundDeps,  icon: ArrowDownLeft},
            { label: 'Outbound', value: mod.totalOutboundDeps, icon: ArrowUpRight },
          ].map(({ label, value, icon: Icon }) => (
            <div key={label} className="rounded-xl neu-raised-sm p-3 text-center">
              <Icon className="mx-auto mb-1 h-4 w-4 text-muted-foreground" />
              <p className="text-xl font-bold text-foreground">{value ?? '\u2014'}</p>
              <p className="text-xs text-muted-foreground">{label}</p>
            </div>
          ))}
        </div>

        {/* Score bars */}
        <div className="rounded-xl neu-raised-sm p-4 space-y-4">
          <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
            Scores
          </h3>
          <ScoreBar label="Composite score"    value={mod.avgCompositeScore  ?? 0} />
          {mod.minIsolationScore != null && (
            <ScoreBar label="Isolation score"  value={mod.minIsolationScore} />
          )}
        </div>

        {/* Blockers */}
        {mod.blockers?.length > 0 && (
          <div className="rounded-xl border border-amber-200 dark:border-amber-800 bg-amber-50 dark:bg-amber-950/30 p-4 space-y-2">
            <div className="flex items-center gap-1.5">
              <AlertTriangle className="h-3.5 w-3.5 text-amber-500" />
              <h3 className="text-xs font-semibold uppercase tracking-wide text-amber-700 dark:text-amber-400">
                Blockers ({mod.blockers.length})
              </h3>
            </div>
            <ul className="space-y-1.5">
              {mod.blockers.map((b: string, i: number) => (
                <li key={i} className="flex gap-2 text-xs text-amber-700 dark:text-amber-300">
                  <span className="mt-0.5 h-1.5 w-1.5 shrink-0 rounded-full bg-amber-400" />
                  {b}
                </li>
              ))}
            </ul>
          </div>
        )}

        {/* Packages */}
        {mod.packages?.length > 0 && (
          <div className="rounded-xl neu-raised-sm overflow-hidden">
            <button
              onClick={() => setShowPkg((v) => !v)}
              className="w-full flex items-center gap-2 px-4 py-3 text-left hover:neu-flat transition-all"
            >
              <Package className="h-4 w-4 text-muted-foreground" />
              <span className="text-sm font-medium text-foreground flex-1">Packages</span>
              <Badge variant="secondary" className="text-xs">{mod.packages.length}</Badge>
              {showPkg
                ? <ChevronDown className="h-4 w-4 text-muted-foreground" />
                : <ChevronRight className="h-4 w-4 text-muted-foreground" />}
            </button>
            {showPkg && (
              <div className="border-t px-4 pb-4 pt-3 space-y-1.5">
                {mod.packages.map((pkg: string) => (
                  <p key={pkg} className="font-mono text-xs text-muted-foreground">{pkg}</p>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Classes */}
        {mod.classes?.length > 0 && (
          <div className="rounded-xl neu-raised-sm overflow-hidden">
            <button
              onClick={() => setShowCls((v) => !v)}
              className="w-full flex items-center gap-2 px-4 py-3 text-left hover:neu-flat transition-all"
            >
              <Layers className="h-4 w-4 text-muted-foreground" />
              <span className="text-sm font-medium text-foreground flex-1">Classes</span>
              <Badge variant="secondary" className="text-xs">{mod.classes.length}</Badge>
              {showCls
                ? <ChevronDown className="h-4 w-4 text-muted-foreground" />
                : <ChevronRight className="h-4 w-4 text-muted-foreground" />}
            </button>
            {showCls && (
              <div className="border-t px-4 pb-4 pt-3">
                <div className="flex flex-wrap gap-1.5">
                  {mod.classes.map((cls: string) => {
                    const short = cls.includes('.') ? cls.split('.').pop()! : cls
                    return (
                      <span
                        key={cls}
                        title={cls}
                        className="inline-block rounded-md neu-flat px-2 py-0.5 font-mono text-xs text-muted-foreground"
                      >
                        {short}
                      </span>
                    )
                  })}
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

// -- Structure tab (tree + file preview) ---------------------------------------
function StructureTab({
  scaffold,
  loading,
  error,
  selectedFile,
  onSelectFile,
  moduleName,
  repoName,
}: {
  scaffold: ScaffoldPreviewResponse | null
  loading: boolean
  error: Error | null
  selectedFile: ProjectTreeNodeResponse | null
  onSelectFile: (n: ProjectTreeNodeResponse) => void
  moduleName: string
  repoName: string
}) {
  if (loading) {
    return (
      <div className="flex items-center justify-center h-full gap-2 text-muted-foreground text-sm">
        <RefreshCw className="h-4 w-4 animate-spin" /> Generating project structure...
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center h-full gap-3 text-sm px-6 text-center">
        <div className="rounded-full bg-destructive/10 p-3">
          <AlertTriangle className="h-5 w-5 text-destructive" />
        </div>
        <p className="font-medium text-foreground">Failed to generate project structure</p>
        <p className="text-xs text-muted-foreground max-w-md">{error.message}</p>
      </div>
    )
  }

  if (!scaffold) {
    return (
      <div className="flex items-center justify-center h-full text-muted-foreground text-sm">
        No scaffold data available
      </div>
    )
  }

  return (
    <div className="flex h-full">
      {/* Tree panel */}
      <div className="w-72 shrink-0 border-r overflow-y-auto p-2">
        <div className="flex items-center gap-2 px-2 pb-2 mb-1 border-b">
          <FolderTree className="h-3.5 w-3.5 text-muted-foreground" />
          <span className="text-xs font-medium text-foreground">{scaffold.moduleName}/</span>
          <Badge variant="secondary" className="ml-auto text-[10px]">{scaffold.totalFiles} files</Badge>
        </div>
        <FileTreeNode
          node={scaffold.tree}
          selectedPath={selectedFile?.path ?? null}
          onSelect={onSelectFile}
          defaultOpen
        />
      </div>

      {/* File preview */}
      <div className="flex-1 overflow-hidden bg-background">
        {selectedFile ? (
          <FilePreview
            node={selectedFile}
            moduleName={moduleName}
            repoName={repoName}
          />
        ) : (
          <div className="flex h-full flex-col items-center justify-center text-center px-6">
            <div className="rounded-full neu-flat p-4 mb-4">
              <FileText className="h-8 w-8 text-muted-foreground/50" />
            </div>
            <p className="text-sm font-medium text-muted-foreground">Select a file</p>
            <p className="mt-1 text-xs text-muted-foreground/60">
              Click a file in the tree to preview its generated content
            </p>
            <div className="mt-4 flex items-center gap-4 text-xs text-muted-foreground/60">
              <span className="flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-emerald-500" /> Generated
              </span>
              <span className="flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-blue-400" /> Move from source
              </span>
            </div>
          </div>
        )}
      </div>
    </div>
  )
}

// -- Spring Context tab --------------------------------------------------------
function SpringTab({
  scaffold,
  loading,
  error,
}: {
  scaffold: ScaffoldPreviewResponse | null
  loading: boolean
  error: Error | null
}) {
  if (loading) {
    return (
      <div className="flex items-center justify-center h-full gap-2 text-muted-foreground text-sm">
        <RefreshCw className="h-4 w-4 animate-spin" /> Loading...
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center h-full gap-3 text-sm px-6 text-center">
        <div className="rounded-full bg-destructive/10 p-3">
          <AlertTriangle className="h-5 w-5 text-destructive" />
        </div>
        <p className="font-medium text-foreground">Failed to load Spring context</p>
        <p className="text-xs text-muted-foreground max-w-md">{error.message}</p>
      </div>
    )
  }

  const files = scaffold?.springContextFiles ?? []

  return (
    <div className="h-full overflow-y-auto p-6 space-y-4">
      <div className="rounded-xl neu-raised-sm p-4 space-y-3">
        <div className="flex items-center gap-2">
          <Shield className="h-4 w-4 text-muted-foreground" />
          <h3 className="text-sm font-semibold text-foreground">Spring Context Files</h3>
          <Badge variant="secondary" className="text-xs ml-auto">{files.length}</Badge>
        </div>
        {files.length === 0 ? (
          <p className="text-xs text-muted-foreground">
            No Spring XML context files detected for this module. If this module uses annotation-based
            configuration, context is managed by Spring Boot auto-configuration.
          </p>
        ) : (
          <div className="space-y-2">
            <p className="text-xs text-muted-foreground">
              These Spring XML configuration files have been filtered to include only the beans and
              component-scans relevant to this module's packages.
            </p>
            <ul className="space-y-1.5">
              {files.map((f) => (
                <li key={f} className="flex items-center gap-2">
                  <FileText className="h-3.5 w-3.5 text-muted-foreground shrink-0" />
                  <span className="font-mono text-xs text-muted-foreground">{f}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
      </div>

      <div className="rounded-xl neu-raised-sm p-4 space-y-2">
        <h3 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">
          What gets handled
        </h3>
        <ul className="space-y-2 text-xs text-muted-foreground">
          <li className="flex items-start gap-2">
            <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500" />
            <span><strong className="text-foreground">Bean definitions</strong> — only beans whose class belongs to this module's packages are included</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500" />
            <span><strong className="text-foreground">Component scans</strong> — base-package attributes are narrowed to module scope</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-emerald-500" />
            <span><strong className="text-foreground">Imports</strong> — resource imports between context files are preserved</span>
          </li>
          <li className="flex items-start gap-2">
            <span className="mt-1 h-1.5 w-1.5 shrink-0 rounded-full bg-blue-400" />
            <span><strong className="text-foreground">Annotation config</strong> — @ComponentScan, @Configuration, @Bean annotations are left as-is in source files</span>
          </li>
        </ul>
      </div>
    </div>
  )
}

// -- Page ----------------------------------------------------------------------
export default function CandidatesPage() {
  const [search,     setSearch]     = useState('')
  const [groupDepth, setGroupDepth] = useState(3)
  const [minScore,   setMinScore]   = useState(0.3)
  const [selected,   setSelected]   = useState<ModuleRecommendationResponse | null>(null)
  const [params,     setParams]     = useState({ groupDepth: 3, minScore: 0.3 })
  const [aiResult,   setAiResult]   = useState<AiAnalysisResponse | null>(null)

  const aiConfigured = hasAiConfig()

  const { data: candidates, isLoading, refetch } = useQuery({
    queryKey:  ['recommendations', params.groupDepth, params.minScore],
    queryFn:   () => api.getRecommendations(params.groupDepth, params.minScore),
  })

  const buildAiReq = (): AiAnalysisRequest => ({
    model: getModelId(),
    moduleName: selected?.moduleName ?? '',
    groupDepth: params.groupDepth,
    minScore: params.minScore,
  })

  const aiMutation = useMutation({
    mutationFn: (fn: (key: string, body: AiAnalysisRequest) => Promise<AiAnalysisResponse>) =>
      fn(getApiKey(), buildAiReq()),
    onSuccess: (data) => setAiResult(data),
    onError: (err: Error) => setAiResult({ content: '', modelUsed: '', promptTokens: 0, completionTokens: 0, error: err.message }),
  })

  const handleSelect = (mod: ModuleRecommendationResponse) => {
    setSelected(mod)
    setAiResult(null)
  }

  const applyFilters = () => {
    setParams({ groupDepth, minScore })
    setSelected(null)
  }

  const filtered = (candidates ?? []).filter((c) =>
    !search ||
    c.moduleName.toLowerCase().includes(search.toLowerCase()) ||
    c.modulePackageRoot.toLowerCase().includes(search.toLowerCase()),
  )

  const extractNow  = filtered.filter((c) => c.recommendation?.toLowerCase().includes('extract_now') || c.recommendation?.toLowerCase().includes('extract now')).length
  const withRefactor = filtered.filter((c) => c.recommendation?.toLowerCase().includes('refactor')).length

  return (
    <div className="flex h-full flex-col">
      {/* Toolbar */}
      <div className="flex shrink-0 flex-wrap items-center gap-3 border-b bg-background px-6 py-3">
        <div className="relative flex-1 min-w-40 max-w-64">
          <Search className="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Search modules\u2026"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="h-8 pl-8 text-xs"
          />
        </div>

        <div className="flex items-center gap-1.5">
          <label className="text-xs text-muted-foreground whitespace-nowrap">Group depth</label>
          <Input
            type="number" min={1} max={6}
            value={groupDepth}
            onChange={(e) => setGroupDepth(Number(e.target.value))}
            className="h-8 w-16 text-xs"
          />
        </div>

        <div className="flex items-center gap-1.5">
          <label className="text-xs text-muted-foreground whitespace-nowrap">Min score</label>
          <Input
            type="number" min={0} max={1} step={0.05}
            value={minScore}
            onChange={(e) => setMinScore(Number(e.target.value))}
            className="h-8 w-20 text-xs"
          />
        </div>

        <Button size="sm" variant="outline" className="h-8 gap-1.5 text-xs" onClick={applyFilters}>
          Apply
        </Button>
        <Button size="sm" variant="ghost" className="h-8 gap-1.5 text-xs"
          disabled={isLoading} onClick={() => refetch()}>
          <RefreshCw className={cn('h-3.5 w-3.5', isLoading && 'animate-spin')} />
          Refresh
        </Button>

        {/* AI actions */}
        {aiConfigured && selected && (
          <div className="flex items-center gap-1 border-l pl-3 ml-1">
            <span className="text-[10px] text-muted-foreground mr-1">AI</span>
            {([
              { label: 'Boundaries', icon: Brain,    fn: api.aiRefineBoundaries },
              { label: 'Migration',  icon: Map,      fn: api.aiMigrationPlan    },
              { label: 'Contexts',   icon: Sparkles, fn: api.aiBoundedContexts  },
              { label: 'Weights',    icon: Scale,    fn: api.aiOptimiseWeights  },
            ] as const).map(({ label, icon: Icon, fn }) => (
              <Button
                key={label}
                size="sm"
                variant="ghost"
                className="h-7 gap-1 text-[11px] px-2"
                disabled={aiMutation.isPending}
                onClick={() => aiMutation.mutate(fn)}
              >
                {aiMutation.isPending ? <Loader2 className="h-3 w-3 animate-spin" /> : <Icon className="h-3 w-3" />}
                {label}
              </Button>
            ))}
          </div>
        )}

        {/* Summary badges */}
        <div className="ml-auto flex items-center gap-2">
          <span className="text-xs text-muted-foreground">{filtered.length} modules</span>
          {extractNow > 0 && (
            <Badge className="text-[10px] bg-emerald-100 text-emerald-700 dark:bg-emerald-950 dark:text-emerald-300 border-emerald-200 hover:bg-emerald-100">
              {extractNow} extract now
            </Badge>
          )}
          {withRefactor > 0 && (
            <Badge className="text-[10px] bg-amber-100 text-amber-700 dark:bg-amber-950 dark:text-amber-300 border-amber-200 hover:bg-amber-100">
              {withRefactor} needs refactoring
            </Badge>
          )}
        </div>
      </div>

      {/* Master-detail split */}
      <div className="flex flex-1 min-h-0 overflow-hidden">
        {/* Left: module list */}
        <div className="w-80 shrink-0 border-r border-border/60 flex flex-col overflow-hidden">
          <div className="flex-1 overflow-y-auto">
            {isLoading ? (
              <div className="space-y-px p-2">
                {[1,2,3,4,5,6].map((i) => (
                  <div key={i} className="h-16 rounded-lg bg-muted animate-pulse" />
                ))}
              </div>
            ) : filtered.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-16 text-center px-4">
                <Box className="mb-3 h-8 w-8 text-muted-foreground/30" />
                <p className="text-sm font-medium text-muted-foreground">No candidates found</p>
                <p className="mt-1 text-xs text-muted-foreground/60">
                  Try lowering min score or syncing a repository
                </p>
              </div>
            ) : (
              filtered.map((mod) => (
                <ModuleItem
                  key={`${mod.repoName}-${mod.moduleName}`}
                  mod={mod}
                  selected={selected?.moduleName === mod.moduleName && selected?.repoName === mod.repoName}
                  onClick={() => handleSelect(mod)}
                />
              ))
            )}
          </div>
        </div>

        {/* Right: detail */}
        <div className="flex-1 overflow-hidden bg-background">
          {selected ? (
            <DetailPanel mod={selected} params={params} aiResult={aiResult} aiLoading={aiMutation.isPending} />
          ) : (
            <div className="flex h-full flex-col items-center justify-center text-center px-6">
              <div className="rounded-full neu-flat p-4 mb-4">
                <Layers className="h-8 w-8 text-muted-foreground/50" />
              </div>
              <p className="text-sm font-medium text-muted-foreground">Select a module</p>
              <p className="mt-1 text-xs text-muted-foreground/60">
                Click a module on the left to see its extraction details, project structure, and export options
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
