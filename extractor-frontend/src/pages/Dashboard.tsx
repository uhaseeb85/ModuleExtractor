import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import {
  GitFork, Box, Share2, AlertTriangle, ChevronRight,
  Clock, CheckCircle2, Wifi
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'

function StatCard({
  label, value, icon: Icon, color, sub,
}: {
  label: string; value: string | number; icon: React.ElementType
  color: string; sub?: string
}) {
  return (
    <div className="rounded-xl border bg-card p-5">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">{label}</p>
          <p className="mt-1.5 text-3xl font-bold text-foreground">{value}</p>
          {sub && <p className="mt-0.5 text-xs text-muted-foreground">{sub}</p>}
        </div>
        <div className={`flex h-10 w-10 items-center justify-center rounded-lg ${color}`}>
          <Icon className="h-5 w-5" />
        </div>
      </div>
    </div>
  )
}

export default function Dashboard() {
  const navigate = useNavigate()

  const { data: repos, isLoading } = useQuery({
    queryKey: ['repos'],
    queryFn: api.getRepos,
  })

  const { data: sharedEntities } = useQuery({
    queryKey: ['shared-entities'],
    queryFn: api.getSharedEntities,
  })

  const synced      = repos?.filter((r) => !!r.syncedAt) ?? []
  const totalNodes  = repos?.reduce((s, r) => s + r.nodeCount, 0) ?? 0

  return (
    <div className="h-full overflow-y-auto">
      <div className="mx-auto max-w-5xl space-y-8 p-8">
        {/* Page header */}
        <div>
          <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Overview of your codebase health and dependency analysis
          </p>
        </div>

        {/* Stat cards */}
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <StatCard
            label="Repositories"
            value={repos?.length ?? '�'}
            icon={GitFork}
            color="bg-indigo-100 text-indigo-600 dark:bg-indigo-950 dark:text-indigo-400"
            sub={synced.length > 0 ? `${synced.length} synced` : undefined}
          />
          <StatCard
            label="Total Nodes"
            value={totalNodes || '�'}
            icon={Box}
            color="bg-violet-100 text-violet-600 dark:bg-violet-950 dark:text-violet-400"
            sub="classes, interfaces, enums"
          />
          <StatCard
            label="Shared Entities"
            value={sharedEntities?.length ?? '�'}
            icon={Share2}
            color="bg-sky-100 text-sky-600 dark:bg-sky-950 dark:text-sky-400"
            sub="cross-repo references"
          />
          <StatCard
            label="Cross-Repo Risks"
            value={sharedEntities?.length ?? '�'}
            icon={AlertTriangle}
            color="bg-amber-100 text-amber-600 dark:bg-amber-950 dark:text-amber-400"
            sub="require attention"
          />
        </div>

        {/* Repos table */}
        <section>
          <div className="mb-4 flex items-center justify-between">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
              Repositories
            </h2>
            <Button variant="ghost" size="sm" className="gap-1 text-xs" onClick={() => navigate('/repos')}>
              Manage <ChevronRight className="h-3 w-3" />
            </Button>
          </div>

          {isLoading ? (
            <div className="space-y-2">
              {[1,2,3].map((i) => (
                <div key={i} className="h-14 rounded-lg bg-muted animate-pulse" />
              ))}
            </div>
          ) : repos?.length === 0 ? (
            <div className="flex flex-col items-center justify-center rounded-xl border border-dashed py-12 text-center">
              <GitFork className="mb-3 h-8 w-8 text-muted-foreground/40" />
              <p className="text-sm font-medium text-muted-foreground">No repositories yet</p>
              <p className="mt-1 text-xs text-muted-foreground/70">Go to Repositories to add your first repo</p>
              <Button size="sm" className="mt-4" onClick={() => navigate('/repos')}>
                Add Repository
              </Button>
            </div>
          ) : (
            <div className="rounded-xl border bg-card overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b bg-muted/40">
                    <th className="py-2.5 pl-4 pr-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wide">Name</th>
                    <th className="px-3 py-2.5 text-left text-xs font-medium text-muted-foreground uppercase tracking-wide">Build</th>
                    <th className="px-3 py-2.5 text-left text-xs font-medium text-muted-foreground uppercase tracking-wide">Branch</th>
                    <th className="px-3 py-2.5 text-right text-xs font-medium text-muted-foreground uppercase tracking-wide">Nodes</th>
                    <th className="pl-3 pr-4 py-2.5 text-left text-xs font-medium text-muted-foreground uppercase tracking-wide">Last Sync</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {repos?.map((r) => (
                    <tr key={r.name} className="hover:bg-muted/30 transition-colors">
                      <td className="py-3 pl-4 pr-3">
                        <div className="flex items-center gap-2">
                          <span
                            className={`h-2 w-2 rounded-full shrink-0 ${r.syncedAt ? 'bg-emerald-500' : 'bg-muted-foreground/30'}`}
                          />
                          <span className="font-medium text-foreground">{r.name}</span>
                        </div>
                      </td>
                      <td className="px-3 py-3">
                        <Badge variant="secondary" className="text-xs">{r.buildTool}</Badge>
                      </td>
                      <td className="px-3 py-3 text-muted-foreground font-mono text-xs">{r.branch}</td>
                      <td className="px-3 py-3 text-right font-medium text-foreground">{r.nodeCount}</td>
                      <td className="pl-3 pr-4 py-3">
                        {r.syncedAt ? (
                          <span className="flex items-center gap-1 text-xs text-muted-foreground">
                            <Clock className="h-3 w-3" />
                            {new Date(r.syncedAt).toLocaleString()}
                          </span>
                        ) : (
                          <span className="text-xs text-muted-foreground/60">Never</span>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>

        {/* Shared entities */}
        {sharedEntities && sharedEntities.length > 0 && (
          <section>
            <div className="mb-4 flex items-center gap-2">
              <Wifi className="h-4 w-4 text-amber-500" />
              <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">
                Shared Entities
              </h2>
              <Badge variant="outline" className="text-xs">{sharedEntities.length}</Badge>
            </div>
            <div className="space-y-1.5">
              {sharedEntities.slice(0, 10).map((e) => (
                <div
                  key={e.fqn}
                  className="flex items-center gap-3 rounded-lg border bg-card px-4 py-2.5"
                >
                  <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-amber-500" />
                  <span className="font-medium text-sm text-foreground">{e.simpleName}</span>
                  <span className="font-mono text-xs text-muted-foreground truncate">{e.fqn}</span>
                </div>
              ))}
              {sharedEntities.length > 10 && (
                <p className="pl-4 text-xs text-muted-foreground">
                  +{sharedEntities.length - 10} more entities
                </p>
              )}
            </div>
          </section>
        )}

        {/* No-data guidance */}
        {!isLoading && repos?.length === 0 && (
          <section className="rounded-xl border border-dashed bg-accent/20 p-6 space-y-3">
            <h3 className="font-semibold text-foreground">Getting Started</h3>
            <ol className="space-y-2 text-sm text-muted-foreground list-decimal list-inside">
              <li>Add a Git repository in <strong>Repositories</strong></li>
              <li>Click <strong>Sync</strong> to analyse its code structure</li>
              <li>Explore dependencies in <strong>Dependency Graph</strong></li>
              <li>See module extraction candidates in <strong>Module Recommendations</strong></li>
            </ol>
          </section>
        )}
      </div>
    </div>
  )
}
