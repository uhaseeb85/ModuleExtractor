import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, type ModuleRecommendationResponse } from '../api/client'
import {
  Search, RefreshCw, AlertTriangle, Package, ChevronDown,
  ChevronRight, Layers, ArrowDownLeft, ArrowUpRight, Box
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

// -- Score ring ----------------------------------------------------------------
function Ring({ score, size = 40 }: { score: number; size?: number }) {
  const r    = (size - 6) / 2
  const circ = 2 * Math.PI * r
  const clamp = Math.max(0, Math.min(1, score))
  const color =
    clamp >= 0.65 ? '#10b981' :
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
      <div className="h-1.5 rounded-full bg-muted overflow-hidden">
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

// -- Detail panel --------------------------------------------------------------
function DetailPanel({ mod }: { mod: ModuleRecommendationResponse }) {
  const [showPkg,  setShowPkg]  = useState(false)
  const [showCls,  setShowCls]  = useState(false)
  const score = mod.avgCompositeScore ?? 0
  const pct   = Math.round(score * 100)

  return (
    <div className="h-full overflow-y-auto">
      <div className="space-y-6 p-6">
        {/* Module title + score */}
        <div className="flex items-start gap-5">
          <div className="relative">
            <Ring score={score} size={72} />
            <div className="absolute inset-0 flex items-center justify-center">
              <span className={cn(
                'text-sm font-bold',
                pct >= 65 ? 'text-emerald-600 dark:text-emerald-400' :
                pct >= 45 ? 'text-amber-600 dark:text-amber-400'   : 'text-gray-500',
              )}>
                {pct}
              </span>
            </div>
          </div>
          <div className="flex-1 min-w-0 mt-1">
            <h2 className="text-xl font-bold text-foreground">{mod.moduleName}</h2>
            <p className="mt-0.5 font-mono text-xs text-muted-foreground">{mod.modulePackageRoot}</p>
            <div className="mt-2 flex flex-wrap gap-2">
              <RecBadge text={mod.recommendation} />
              <Badge variant="secondary" className="text-xs">{mod.repoName}</Badge>
            </div>
          </div>
        </div>

        {/* Stats grid */}
        <div className="grid grid-cols-3 gap-3">
          {[
            { label: 'Classes',  value: mod.totalClasses,      icon: Box          },
            { label: 'Inbound',  value: mod.totalInboundDeps,  icon: ArrowDownLeft},
            { label: 'Outbound', value: mod.totalOutboundDeps, icon: ArrowUpRight },
          ].map(({ label, value, icon: Icon }) => (
            <div key={label} className="rounded-xl border bg-card p-3 text-center">
              <Icon className="mx-auto mb-1 h-4 w-4 text-muted-foreground" />
              <p className="text-xl font-bold text-foreground">{value ?? '�'}</p>
              <p className="text-xs text-muted-foreground">{label}</p>
            </div>
          ))}
        </div>

        {/* Score bars */}
        <div className="rounded-xl border bg-card p-4 space-y-4">
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
          <div className="rounded-xl border bg-card overflow-hidden">
            <button
              onClick={() => setShowPkg((v) => !v)}
              className="w-full flex items-center gap-2 px-4 py-3 text-left hover:bg-muted/30 transition-colors"
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
          <div className="rounded-xl border bg-card overflow-hidden">
            <button
              onClick={() => setShowCls((v) => !v)}
              className="w-full flex items-center gap-2 px-4 py-3 text-left hover:bg-muted/30 transition-colors"
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
                        className="inline-block rounded-md bg-muted px-2 py-0.5 font-mono text-xs text-muted-foreground"
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

// -- Page ----------------------------------------------------------------------
export default function CandidatesPage() {
  const [search,     setSearch]     = useState('')
  const [groupDepth, setGroupDepth] = useState(3)
  const [minScore,   setMinScore]   = useState(0.3)
  const [selected,   setSelected]   = useState<ModuleRecommendationResponse | null>(null)
  const [params,     setParams]     = useState({ groupDepth: 3, minScore: 0.3 })

  const { data: candidates, isLoading, refetch } = useQuery({
    queryKey:  ['recommendations', params.groupDepth, params.minScore],
    queryFn:   () => api.getRecommendations(params.groupDepth, params.minScore),
  })

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
      <div className="flex shrink-0 flex-wrap items-center gap-3 border-b bg-card/50 px-6 py-3">
        <div className="relative flex-1 min-w-40 max-w-64">
          <Search className="absolute left-2.5 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="Search modules�"
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
        <div className="w-80 shrink-0 border-r flex flex-col overflow-hidden">
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
                  onClick={() => setSelected(mod)}
                />
              ))
            )}
          </div>
        </div>

        {/* Right: detail */}
        <div className="flex-1 overflow-hidden bg-background">
          {selected ? (
            <DetailPanel mod={selected} />
          ) : (
            <div className="flex h-full flex-col items-center justify-center text-center px-6">
              <div className="rounded-full bg-muted p-4 mb-4">
                <Layers className="h-8 w-8 text-muted-foreground/50" />
              </div>
              <p className="text-sm font-medium text-muted-foreground">Select a module</p>
              <p className="mt-1 text-xs text-muted-foreground/60">
                Click a module on the left to see its extraction details
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}
