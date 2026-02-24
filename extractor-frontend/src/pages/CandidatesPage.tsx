import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, ModuleRecommendationResponse } from '../api/client'

// ── Helpers ───────────────────────────────────────────────────────────

function ScoreBar({ label, value, color }: { label: string; value: number; color: string }) {
  const pct = Math.round(value * 100)
  return (
    <div className="flex items-center gap-2 text-xs">
      <span className="w-24 shrink-0 text-right text-gray-400">{label}</span>
      <div className="flex-1 rounded-full bg-gray-700 h-2">
        <div className={`h-2 rounded-full ${color}`} style={{ width: `${pct}%` }} />
      </div>
      <span className="w-8 text-right text-gray-300">{pct}%</span>
    </div>
  )
}

function RecBadge({ text }: { text: string }) {
  const cls =
    text === 'Extract now'
      ? 'bg-green-800 text-green-100 border border-green-600'
      : text === 'Extract with refactoring'
      ? 'bg-yellow-800 text-yellow-100 border border-yellow-600'
      : 'bg-gray-700 text-gray-300 border border-gray-600'
  return (
    <span className={`inline-block rounded-full px-3 py-0.5 text-xs font-semibold ${cls}`}>
      {text}
    </span>
  )
}

// ── Module Card ───────────────────────────────────────────────────────

function ModuleCard({ mod, rank }: { mod: ModuleRecommendationResponse; rank: number }) {
  const [showClasses,  setShowClasses]  = useState(false)
  const [showPackages, setShowPackages] = useState(false)
  const scoreColor =
    mod.avgCompositeScore >= 0.65
      ? 'text-green-400'
      : mod.avgCompositeScore >= 0.45
      ? 'text-yellow-400'
      : 'text-gray-400'

  return (
    <div className="rounded-xl border border-gray-700 bg-gray-800 p-5 shadow">
      {/* ── Header ── */}
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="flex items-start gap-3 min-w-0">
          <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-indigo-700 text-sm font-bold">
            {rank}
          </span>
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2">
              <h2 className="text-lg font-bold capitalize">{mod.moduleName}</h2>
              <RecBadge text={mod.recommendation} />
            </div>
            <p className="mt-0.5 font-mono text-xs text-gray-400">{mod.modulePackageRoot}</p>
            <p className="text-xs text-gray-500">{mod.repoName}</p>
          </div>
        </div>

        {/* Composite score */}
        <div className="flex flex-col items-center">
          <span className={`text-4xl font-extrabold ${scoreColor}`}>
            {Math.round(mod.avgCompositeScore * 100)}
          </span>
          <span className="text-xs text-gray-400">score</span>
        </div>
      </div>

      {/* ── Stats row ── */}
      <div className="mt-4 flex flex-wrap gap-6 text-sm">
        {[
          ['Packages',   String(mod.packages.length)],
          ['Classes',    String(mod.totalClasses)],
          ['↑ Inbound',  String(mod.totalInboundDeps)],
          ['↓ Outbound', String(mod.totalOutboundDeps)],
        ].map(([label, val]) => (
          <div key={label} className="text-center">
            <div className="text-base font-bold">{val}</div>
            <div className="text-xs text-gray-400">{label}</div>
          </div>
        ))}
      </div>

      {/* ── Score bars ── */}
      <div className="mt-4 space-y-1.5">
        <ScoreBar label="Avg composite" value={mod.avgCompositeScore} color="bg-indigo-500" />
        <ScoreBar label="Min isolation"  value={mod.minIsolationScore}  color="bg-blue-500" />
      </div>

      {/* ── Blockers ── */}
      {mod.blockers.length > 0 && (
        <div className="mt-4 space-y-1">
          {mod.blockers.map((b) => (
            <div key={b} className="flex items-start gap-1.5 rounded bg-yellow-900/40 px-3 py-1.5 text-xs text-yellow-300">
              <span className="mt-px shrink-0">⚠</span>
              {b}
            </div>
          ))}
        </div>
      )}

      {/* ── Packages collapsible ── */}
      <div className="mt-4 border-t border-gray-700 pt-3">
        <button
          onClick={() => setShowPackages(v => !v)}
          className="flex items-center gap-1.5 text-xs font-medium text-indigo-400 hover:text-indigo-300"
        >
          <span>{showPackages ? '▲' : '▼'}</span>
          Packages ({mod.packages.length})
        </button>
        {showPackages && (
          <ul className="mt-2 space-y-1 pl-4">
            {mod.packages.map(p => (
              <li key={p} className="font-mono text-xs text-gray-300">{p}</li>
            ))}
          </ul>
        )}
      </div>

      {/* ── Classes collapsible ── */}
      <div className="mt-3">
        <button
          onClick={() => setShowClasses(v => !v)}
          className="flex items-center gap-1.5 text-xs font-medium text-indigo-400 hover:text-indigo-300"
        >
          <span>{showClasses ? '▲' : '▼'}</span>
          Classes ({mod.totalClasses})
        </button>
        {showClasses && (
          <div className="mt-2 flex flex-wrap gap-1.5 pl-4">
            {mod.classes.map(c => (
              <span
                key={c}
                className="rounded bg-gray-700 px-2 py-0.5 font-mono text-xs text-gray-200"
              >
                {c}
              </span>
            ))}
          </div>
        )}
      </div>
    </div>
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
    m =>
      !filter ||
      m.moduleName.toLowerCase().includes(filter.toLowerCase()) ||
      m.modulePackageRoot.toLowerCase().includes(filter.toLowerCase()) ||
      m.repoName.toLowerCase().includes(filter.toLowerCase()),
  )

  const extractNow   = filtered.filter(m => m.recommendation === 'Extract now').length
  const withRefactor = filtered.filter(m => m.recommendation === 'Extract with refactoring').length

  return (
    <div className="p-6">
      {/* ── Header ── */}
      <div className="mb-6 flex items-start justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold">Module Recommendations</h1>
          <p className="mt-1 text-sm text-gray-400">
            Related packages grouped into proposed standalone modules, ranked by extraction readiness
          </p>
        </div>
        <button
          onClick={() => refetch()}
          className="shrink-0 rounded bg-indigo-700 px-3 py-1.5 text-sm hover:bg-indigo-600"
        >
          Refresh
        </button>
      </div>

      {/* ── Summary cards ── */}
      {data && (
        <div className="mb-6 grid grid-cols-3 gap-4">
          {[
            ['Proposed modules',         String(data.length)],
            ['Extract now',              String(extractNow)],
            ['Extract with refactoring', String(withRefactor)],
          ].map(([label, value]) => (
            <div key={label} className="rounded-lg bg-gray-800 p-4">
              <div className="text-3xl font-bold">{value}</div>
              <div className="mt-1 text-sm text-gray-400">{label}</div>
            </div>
          ))}
        </div>
      )}

      {/* ── Filters ── */}
      <div className="mb-5 flex flex-wrap items-center gap-3 text-sm">
        <input
          type="text"
          placeholder="Filter by module, package or repo…"
          value={filter}
          onChange={e => setFilter(e.target.value)}
          className="w-72 rounded bg-gray-700 px-3 py-1.5 placeholder-gray-500 focus:outline-none focus:ring-1 focus:ring-indigo-500"
        />
        <label className="flex items-center gap-2 text-gray-300">
          Group depth:
          <input
            type="number" min={2} max={8} value={groupDepth}
            onChange={e => setGroupDepth(Number(e.target.value))}
            title="Number of package segments that define the module root (e.g. 4 = com.bank.ivr.auth)"
            className="w-14 rounded bg-gray-700 px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </label>
        <label className="flex items-center gap-2 text-gray-300">
          Min score:
          <input
            type="number" min={0} max={1} step={0.05} value={minScore}
            onChange={e => setMinScore(Number(e.target.value))}
            className="w-16 rounded bg-gray-700 px-2 py-1.5 focus:outline-none focus:ring-1 focus:ring-indigo-500"
          />
        </label>
      </div>

      {/* ── Content ── */}
      {isLoading && (
        <p className="text-gray-400">Analysing dependency graph and grouping modules…</p>
      )}
      {error && (
        <p className="text-red-400">
          Failed to load recommendations. Make sure at least one repository has been synced.
        </p>
      )}
      {!isLoading && !error && filtered.length === 0 && (
        <p className="text-gray-400">
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
