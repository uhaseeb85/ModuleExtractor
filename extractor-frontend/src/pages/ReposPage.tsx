import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../api/client'
import AddRepoModal from '../components/AddRepoModal'
import {
  Plus, FolderSearch, RefreshCw, Trash2, GitFork,
  Clock, Loader2
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { cn } from '@/lib/utils'

export default function ReposPage() {
  const qc = useQueryClient()
  const [showAdd, setShowAdd] = useState(false)
  const [scanPath, setScanPath] = useState('')
  const [showScan, setShowScan] = useState(false)
  const [syncingRow, setSyncingRow] = useState<string | null>(null)

  const { data: repos, isLoading } = useQuery({
    queryKey: ['repos'],
    queryFn: api.getRepos,
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

  return (
    <div className="h-full overflow-y-auto">
      <div className="mx-auto max-w-5xl space-y-6 p-8">
        {/* Header */}
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Repositories</h1>
            <p className="mt-1 text-sm text-muted-foreground">
              Manage and sync your Java repositories
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
        {isLoading ? (
          <div className="space-y-2">
            {[1,2,3].map((i) => (
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
                          <Loader2 className="h-3.5 w-3.5 animate-spin" /> Syncing�
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
      </div>

      {showAdd && <AddRepoModal open={showAdd} onClose={() => setShowAdd(false)} />}
    </div>
  )
}
