import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, ModuleRecommendationResponse } from '../api/client'
import { RefreshCw, ChevronDown, ChevronUp, AlertTriangle } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Progress } from '@/components/ui/progress'
import { cn } from '@/lib/utils'

// ── Helpers ───────────────────────────────────────────────────────────

function ScoreBar({ label, value, className }: { label: string; value: number; className?: string }) {
  const pct = Math.round(value * 100)
  return (
    <div className="flex items-center gap-2 text-xs">
      <span className="w-24 shrink-0 text-right text-muted-foreground">{label}</span>
      <Progress value={pct} className={cn('h-2 flex-1', className)} />
      <span className="w-8 text-right font-medium text-foreground">{pct}%</span>
    </div>
  )
}

function RecBadge({ text }: { text: string }) {
  const variant =
    text === 'Extract now'
      ? 'default'
      : text === 'Extract with refactoring'
      ? 'secondary'
      : 'outline'
  return <Badge variant={variant as 'default' | 'secondary' | 'outline'}>{text}</Badge>
}

// ── Module Card ───────────────────────────────────────────────────────

function ModuleCard({ mod, rank }: { mod: ModuleRecommendationResponse; rank: number }) {
  const [showClasses,  setShowClasses]  = useState(false)
  const [showPackages, setShowPackages] = useState(false)

  const scoreColor =
    mod.avgCompositeScore >= 0.65
      ? 'text-emerald-600 dark:text-emerald-400'
      : mod.avgCompositeScore >= 0.45
      ? 'text-amber-600 dark:text-amber-400'
      : 'text-muted-foreground'

  return (
    <Card>
      <CardContent className="p-5">
        {/* Header */}
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div className="flex items-start gap-3 min-w-0">
            <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-primary text-sm font-bold text-primary-foreground">
              {rank}
            </span>
            <div className="min-w-0">
              <div className="flex flex-wrap items-center gap-2">
                <h2 className="text-lg font-bold capitalize text-foreground">{mod.moduleName}</h2>
                <RecBadge text={mod.recommendation} />
              </div>
              <p className="mt-0.5 font-mono text-xs text-muted-foreground">{mod.modulePackageRoot}</p>
              <p className="text-xs text-muted-foreground">{mod.repoName}</p>
            </div>
          </div>

          <div className="flex flex-col items-center">
            <span className={cn('text-4xl font-extrabold', scoreColor)}>
              {Math.round(mod.avgCompositeScore * 100)}
            </span>
            <span className="text-xs text-muted-foreground">score</span>
          </div>
        </div>

        {/* Stats row */}
        <div className="mt-4 flex flex-wrap gap-6 text-sm">
          {([
            ['Packages',   String(mod.packages.length)],
            ['Classes',    String(mod.totalClasses)],
            ['↑ Inbound',  String(mod.totalInboundDeps)],
            ['↓ Outbound', String(mod.totalOutboundDeps)],
          ] as const).map(([label, val]) => (
            <div key={label} className="text-center">
              <div className="text-base font-bold text-foreground">{val}</div>
              <div className="text-xs text-muted-foreground">{label}</div>
            </div>
          ))}
        </div>

        {/* Score bars */}
        <div className="mt-4 space-y-1.5">
          <ScoreBar label="Avg composite" value={mod.avgCompositeScore} />
          <ScoreBar label="Min isolation" value={mod.minIsolationScore} />
        </div>

        {/* Blockers */}
        {mod.blockers.length > 0 && (
          <div className="mt-4 space-y-1">
            {mod.blockers.map((b) => (
              <div
                key={b}
                className="flex items-start gap-1.5 rounded-md bg-amber-50 px-3 py-1.5 text-xs text-amber-700 dark:bg-amber-900/30 dark:text-amber-300"
              >
                <AlertTriangle className="mt-0.5 h-3 w-3 shrink-0" />
                {b}
              </div>
            ))}
          </div>
        )}

        {/* Packages collapsible */}
        <div className="mt-4 border-t pt-3">
          <button
            onClick={() => setShowPackages((v) => !v)}
            className="flex items-center gap-1.5 text-xs font-medium text-primary hover:underline"
          >
            {showPackages ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
            Packages ({mod.packages.length})
          </button>
          {showPackages && (
            <ul className="mt-2 space-y-1 pl-5">
              {mod.packages.map((p) => (
                <li key={p} className="font-mono text-xs text-muted-foreground">{p}</li>
              ))}
            </ul>
          )}
        </div>

        {/* Classes collapsible */}
        <div className="mt-3">
          <button
            onClick={() => setShowClasses((v) => !v)}
            className="flex items-center gap-1.5 text-xs font-medium text-primary hover:underline"
          >
            {showClasses ? <ChevronUp className="h-3 w-3" /> : <ChevronDown className="h-3 w-3" />}
            Classes ({mod.totalClasses})
          </button>
          {showClasses && (
            <div className="mt-2 flex flex-wrap gap-1.5 pl-5">
              {mod.classes.map((c) => (
                <Badge key={c} variant="secondary" className="font-mono text-xs">
                  {c}
                </Badge>
              ))}
            </div>
          )}
        </div>
      </CardContent>
    </Card>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────

export default function CandidatesPage() {
  const [groupDepth, setGroupDepth] = useState(4)
  const [minScore,   setMinScore]   = useState(0.4)
  const [filter,     setFilter]     = useState('')

  const { data, isLoading, error, refetch } = useQuery<ModuleRecommendationResponse[]>({
    queryKey: ['recommendations', groupDepth, minScore],
    queryFn: () => api.getRecommendations(groupDepth, minScore),
    refetchInterval: 60_000,
  })

  const filtered = (data ?? []).filter(
    (m) =>
      !filter ||
      m.moduleName.toLowerCase().includes(filter.toLowerCase()) ||
      m.modulePackageRoot.toLowerCase().includes(filter.toLowerCase()) ||
      m.repoName.toLowerCase().includes(filter.toLowerCase()),
  )

  const extractNow   = filtered.filter((m) => m.recommendation === 'Extract now').length
  const withRefactor = filtered.filter((m) => m.recommendation === 'Extract with refactoring').length

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Module Recommendations</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Related packages grouped into proposed standalone modules, ranked by extraction readiness
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={() => refetch()} className="gap-1.5">
          <RefreshCw className="h-3.5 w-3.5" />
          Refresh
        </Button>
      </div>

      {/* Summary cards */}
      {data && (
        <div className="grid grid-cols-3 gap-4">
          {([
            ['Proposed modules',         String(data.length)],
            ['Extract now',              String(extractNow)],
            ['Extract with refactoring', String(withRefactor)],
          ] as const).map(([label, value]) => (
            <Card key={label}>
              <CardContent className="p-4">
                <p className="text-2xl font-bold text-foreground">{value}</p>
                <p className="mt-1 text-xs text-muted-foreground">{label}</p>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Filters */}
      <Card>
        <CardContent className="flex flex-wrap items-end gap-4 px-4 py-3">
          <div className="space-y-1">
            <Label className="text-xs">Search</Label>
            <Input
              placeholder="Filter by module, package or repo…"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              className="h-8 w-72"
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Group depth</Label>
            <Input
              type="number"
              min={2}
              max={8}
              value={groupDepth}
              onChange={(e) => setGroupDepth(Number(e.target.value))}
              title="Number of package segments that define the module root"
              className="h-8 w-16"
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Min score</Label>
            <Input
              type="number"
              min={0}
              max={1}
              step={0.05}
              value={minScore}
              onChange={(e) => setMinScore(Number(e.target.value))}
              className="h-8 w-20"
            />
          </div>
        </CardContent>
      </Card>

      {/* Content */}
      {isLoading && (
        <p className="text-sm text-muted-foreground">
          Analysing dependency graph and grouping modules…
        </p>
      )}
      {error && (
        <p className="text-sm text-destructive">
          Failed to load recommendations. Make sure at least one repository has been synced.
        </p>
      )}
      {!isLoading && !error && filtered.length === 0 && (
        <p className="text-sm text-muted-foreground">
          No modules found. Try lowering the min score threshold, or sync a repository first.
        </p>
      )}

      <div className="space-y-4">
        {filtered.map((mod, i) => (
          <ModuleCard
            key={`${mod.repoName}::${mod.modulePackageRoot}`}
            mod={mod}
            rank={i + 1}
          />
        ))}
      </div>
    </div>
  )
}
