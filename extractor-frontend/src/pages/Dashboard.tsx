import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import AddRepoModal from '../components/AddRepoModal'
import {
  GitFork, Box, Share2, AlertTriangle,
  Clock, CheckCircle2, Wifi,
  Plus, FolderSearch, RefreshCw, Trash2, Loader2,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

function StatCard({
  label, value, icon: Icon, sub,
}: {
  label: string; value: string | number; icon: React.ElementType
  color?: string; sub?: string
}) {
  return (
    <div className="rounded-2xl neu-raised p-5">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-medium text-muted-foreground uppercase tracking-wide">{label}</p>
          <p className="mt-1.5 text-3xl font-bold text-foreground">{value}</p>
          {sub && <p className="mt-0.5 text-xs text-muted-foreground">{sub}</p>}
        </div>
        <div className="flex h-10 w-10 items-center justify-center rounded-xl neu-raised-sm">
          <Icon className="h-5 w-5 text-primary" />
        </div>
      </div>
    </div>
  )
}

export default function Dashboard() {
  const qc = useQueryClient()
  const [showAdd, setShowAdd] = useState(false)
  const [scanPath, setScanPath] = useState('')
  const [showScan, setShowScan] = useState(false)
  const [syncingRow, setSyncingRow] = useState<string | null>(null)

  const { data: repos, isLoading } = useQuery({
    queryKey: ['repos'],
    queryFn: api.getRepos,
  })

  const { data: sharedEntities } = useQuery({
    queryKey: ['shared-entities'],
    queryFn: api.getSharedEntities,
  })

  const syncRowMut = useMutation({
    mutationFn: (name: string) => api.triggerRepoSync(name),
    onMutate: (name) => setSyncingRow(name),
    onSettled: () => {
      setSyncingRow(null)
      qc.invalidateQueries({ queryKey: ['repos'] })
    },
  })

  const deleteMut = useMutation({
    mutationFn: (name: string) => api.removeRepo(name),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['repos'] }),
  })

  const syncAllMut = useMutation({
    mutationFn: () => api.triggerFullSync(),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['repos'] }),
  })

  const scanMut = useMutation({
    mutationFn: (path: string) =>
      api.scanDirectory({ directoryPath: path, buildTool: 'MAVEN', branch: 'main' }),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['repos'] })
      setShowScan(false)
      setScanPath('')
    },
  })

  const synced     = repos?.filter((r) => !!r.syncedAt) ?? []
  const totalNodes = repos?.reduce((s, r) => s + r.nodeCount, 0) ?? 0

  return (
    <div className="h-full overflow-y-auto">
      <div className="mx-auto max-w-5xl space-y-8 p-8">
        {/* Page header */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Codebase health overview and repository management
            </p>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" size="sm" className="gap-1.5"
              disabled={syncAllMut.isPending}
              onClick={() => syncAllMut.mutate()}>
              {syncAllMut.isPending
                ? <Loader2 className="h-3.5 w-3.5 animate-spin" />
                : <RefreshCw className="h-3.5 w-3.5" />}
              Sync All
            </Button>
            <Button variant="outline" size="sm" className="gap-1.5" onClick={() => setShowScan(true)}>
              <FolderSearch className="h-3.5 w-3.5" />
              Scan Directory
            </Button>
            <Button size="sm" className="gap-1.5" onClick={() => setShowAdd(true)}>
              <Plus className="h-3.5 w-3.5" />
              Add Repo
            </Button>
          </div>
        </div>

        {/* Stat cards */}
        <div className="grid grid-cols-2 gap-4 lg:grid-cols-4">
          <StatCard
            label="Repositories"
            value={repos?.length ?? '–'}
            icon={GitFork}
            sub={synced.length > 0 ? `${synced.length} synced` : undefined}
          />
          <StatCard
            label="Total Nodes"
            value={totalNodes || '–'}
            icon={Box}
            sub="classes, interfaces, enums"
          />
          <StatCard
            label="Shared Entities"
            value={sharedEntities?.length ?? '–'}
            icon={Share2}
            sub="cross-repo references"
          />
          <StatCard
            label="Cross-Repo Risks"
            value={sharedEntities?.length ?? '–'}
            icon={AlertTriangle}
            sub="require attention"
          />
        </div>

        {/* Scan directory inline form */}
        {showScan && (
          <div className="flex items-center gap-3 rounded-xl neu-inset p-4">
            <FolderSearch className="h-4 w-4 shrink-0 text-muted-foreground" />
            <input
              type="text"
              placeholder="/path/to/directory"
              value={scanPath}
              onChange={(e) => setScanPath(e.target.value)}
              className="flex-1 bg-transparent text-sm outline-none placeholder:text-muted-foreground/50"
            />
            <Button size="sm" disabled={!scanPath || scanMut.isPending}
              onClick={() => scanMut.mutate(scanPath)}>
              {scanMut.isPending ? <Loader2 className="h-3.5 w-3.5 animate-spin" /> : 'Scan'}
            </Button>
            <Button size="sm" variant="ghost" onClick={() => setShowScan(false)}>Cancel</Button>
          </div>
        )}

        {/* Repos table */}
        <section>
          <div className="mb-4">
            <h2 className="text-sm font-semibold uppercase tracking-wide text-muted-foreground">Repositories</h2>
          </div>

          {isLoading ? (
            <div className="space-y-2">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-14 rounded-lg bg-muted animate-pulse" />
              ))}
            </div>
          ) : repos?.length === 0 ? (
            <div className="flex flex-col items-center justify-center rounded-2xl neu-inset py-16 text-center">
              <GitFork className="mb-3 h-10 w-10 text-muted-foreground/30" />
              <p className="text-sm font-medium text-muted-foreground">No repositories added yet</p>
              <p className="mt-1 text-xs text-muted-foreground/60">
                Add a Git repo or scan a local directory to get started
              </p>
              <div className="mt-5 flex gap-2">
                <Button size="sm" onClick={() => setShowAdd(true)}>Add Repo</Button>
                <Button size="sm" variant="outline" onClick={() => setShowScan(true)}>Scan Directory</Button>
              </div>
            </div>
          ) : (
            <div className="rounded-2xl neu-raised overflow-hidden">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b bg-muted/30">
                    <th className="py-2.5 pl-4 pr-3 text-left text-xs font-medium text-muted-foreground uppercase tracking-wide">Repository</th>
                    <th className="px-3 py-2.5 text-left text-xs font-medium text-muted-foreground uppercase tracking-wide">Build</th>
                    <th className="px-3 py-2.5 text-left text-xs font-medium text-muted-foreground uppercase tracking-wide">Branch</th>
                    <th className="px-3 py-2.5 text-right text-xs font-medium text-muted-foreground uppercase tracking-wide">Nodes</th>
                    <th className="pl-3 pr-4 py-2.5 text-left text-xs font-medium text-muted-foreground uppercase tracking-wide">Last Sync</th>
                    <th className="pr-4 py-2.5" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {repos?.map((r) => (
                    <tr key={r.name} className="hover:bg-muted/30 transition-colors group">
                      <td className="py-3.5 pl-4 pr-3">
                        <div className="flex items-center gap-2.5">
                          <div className={cn(
                            'h-2 w-2 rounded-full shrink-0',
                            r.syncedAt ? 'bg-emerald-500' : 'bg-muted-foreground/20',
                          )} />
                          <div>
                            <p className="font-medium text-foreground">{r.name}</p>
                            <p className="text-xs text-muted-foreground/70 font-mono truncate max-w-64">{r.url}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-3 py-3.5">
                        <Badge variant="secondary" className="text-xs">{r.buildTool}</Badge>
                      </td>
                      <td className="px-3 py-3.5 font-mono text-xs text-muted-foreground">{r.branch}</td>
                      <td className="px-3 py-3.5 text-right font-medium text-foreground">{r.nodeCount}</td>
                      <td className="pl-3 pr-4 py-3.5">
                        {syncingRow === r.name ? (
                          <span className="flex items-center gap-1.5 text-xs text-primary">
                            <Loader2 className="h-3.5 w-3.5 animate-spin" /> Syncing…
                          </span>
                        ) : r.syncedAt ? (
                          <span className="flex items-center gap-1 text-xs text-muted-foreground">
                            <Clock className="h-3 w-3" />
                            {new Date(r.syncedAt).toLocaleString()}
                          </span>
                        ) : (
                          <span className="text-xs text-muted-foreground/50">Never synced</span>
                        )}
                      </td>
                      <td className="pr-4 py-3.5">
                        <div className="flex gap-1 justify-end opacity-0 group-hover:opacity-100 transition-opacity">
                          <Button
                            size="icon"
                            variant="ghost"
                            className="h-7 w-7"
                            disabled={syncRowMut.isPending}
                            title="Sync"
                            onClick={() => syncRowMut.mutate(r.name)}
                          >
                            {syncingRow === r.name
                              ? <Loader2 className="h-3.5 w-3.5 animate-spin" />
                              : <RefreshCw className="h-3.5 w-3.5" />}
                          </Button>
                          <Button
                            size="icon"
                            variant="ghost"
                            className="h-7 w-7 text-red-500 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-950/30"
                            title="Remove"
                            onClick={() => {
                              if (confirm(`Remove repository "${r.name}"?`)) {
                                deleteMut.mutate(r.name)
                              }
                            }}
                          >
                            <Trash2 className="h-3.5 w-3.5" />
                          </Button>
                        </div>
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
                  className="flex items-center gap-3 rounded-xl neu-raised-sm px-4 py-2.5"
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
          <section className="rounded-2xl neu-inset p-6 space-y-3">
            <h3 className="font-semibold text-foreground">Getting Started</h3>
            <ol className="space-y-2 text-sm text-muted-foreground list-decimal list-inside">
              <li>Add a Git repository using <strong>Add Repo</strong> or scan a local directory</li>
              <li>Click <strong>Sync</strong> to analyse its code structure</li>
              <li>Explore dependencies in <strong>Dependency Graph</strong></li>
              <li>See module extraction candidates in <strong>Module Recommendations</strong></li>
            </ol>
          </section>
        )}
      </div>

      {showAdd && <AddRepoModal open={showAdd} onClose={() => setShowAdd(false)} />}
    </div>
  )
}
