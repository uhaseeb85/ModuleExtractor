import { useState, useEffect, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, SyncJobResponse } from '../api/client'
import { Plus, FolderOpen, RefreshCw, Trash2 } from 'lucide-react'
import AddRepoModal from '../components/AddRepoModal'
import ScanDirectoryModal from '../components/ScanDirectoryModal'
import { Card, CardContent } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'
import { cn } from '@/lib/utils'

export default function ReposPage() {
  const queryClient = useQueryClient()
  const [showAddModal, setShowAddModal] = useState(false)
  const [showScanModal, setShowScanModal] = useState(false)
  const [jobs, setJobs] = useState<Record<string, SyncJobResponse>>({})

  const { data: repos = [], isLoading } = useQuery({
    queryKey: ['repos'],
    queryFn: api.getRepos,
  })

  const removeMutation = useMutation({
    mutationFn: (name: string) => api.removeRepo(name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['repos'] }),
  })

  const pollJobs = useCallback(async () => {
    const running = Object.entries(jobs).filter(
      ([, j]) => j.status === 'RUNNING' || j.status === 'PENDING'
    )
    if (running.length === 0) return
    await Promise.all(
      running.map(async ([repoName, j]) => {
        try {
          const updated = await api.getJobStatus(j.jobId)
          setJobs((prev) => ({ ...prev, [repoName]: updated }))
          if (updated.status === 'COMPLETED') {
            queryClient.invalidateQueries({ queryKey: ['repos'] })
          }
        } catch {
          // ignore transient poll errors
        }
      })
    )
  }, [jobs, queryClient])

  useEffect(() => {
    const id = setInterval(pollJobs, 1500)
    return () => clearInterval(id)
  }, [pollJobs])

  const startSync = async (repoName: string) => {
    try {
      const job = await api.triggerRepoSync(repoName)
      setJobs((prev) => ({ ...prev, [repoName]: job }))
    } catch (err) {
      alert(`Failed to start sync: ${(err as Error).message}`)
    }
  }

  const startSyncAll = async () => {
    try {
      const job = await api.triggerFullSync()
      setJobs((prev) => ({ ...prev, ['__all__']: job }))
    } catch (err) {
      alert(`Failed to start sync: ${(err as Error).message}`)
    }
  }

  const jobFor = (name: string) => jobs[name] ?? jobs['__all__'] ?? null

  return (
    <div className="space-y-6 p-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Repositories</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Manage registered repositories and trigger sync operations
          </p>
        </div>
        <div className="flex gap-2">
          {repos.length > 0 && (
            <Button
              variant="outline"
              size="sm"
              onClick={startSyncAll}
              disabled={jobs['__all__']?.status === 'RUNNING'}
              className="gap-1.5"
            >
              <RefreshCw className={cn('h-3.5 w-3.5', jobs['__all__']?.status === 'RUNNING' && 'animate-spin')} />
              {jobs['__all__']?.status === 'RUNNING'
                ? `Syncing all… ${jobs['__all__'].progressPercent}%`
                : 'Sync All'}
            </Button>
          )}
          <Button variant="outline" size="sm" onClick={() => setShowScanModal(true)} className="gap-1.5">
            <FolderOpen className="h-3.5 w-3.5" />
            Scan Directory
          </Button>
          <Button size="sm" onClick={() => setShowAddModal(true)} className="gap-1.5">
            <Plus className="h-3.5 w-3.5" />
            Add Repository
          </Button>
        </div>
      </div>

      {/* Multi-repo hint */}
      {repos.length > 1 && (
        <Card className="border-primary/20 bg-primary/5">
          <CardContent className="px-4 py-2.5 text-xs text-primary">
            {repos.length} repositories configured — cross-repo dependencies are linked
            automatically during sync.
          </CardContent>
        </Card>
      )}

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Loading…</p>
      ) : repos.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed py-16 text-center">
          <p className="mb-2 text-muted-foreground">No repositories registered yet.</p>
          <Button onClick={() => setShowAddModal(true)} className="mt-2 gap-1.5">
            <Plus className="h-4 w-4" />
            Add Your First Repository
          </Button>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {repos.map((r) => {
            const job = jobFor(r.name)
            const isRunning = job?.status === 'RUNNING' || job?.status === 'PENDING'
            const isFailed = job?.status === 'FAILED'
            const isDone = job?.status === 'COMPLETED'
            return (
              <Card key={r.name} className="overflow-hidden">
                <CardContent className="p-5">
                  <div className="mb-2 flex items-center justify-between">
                    <h2 className="font-semibold text-foreground">{r.name}</h2>
                    <Badge variant="secondary">{r.buildTool}</Badge>
                  </div>
                  <p className="mb-1 break-all text-xs text-muted-foreground">{r.url}</p>
                  <p className="mb-3 text-xs text-muted-foreground">
                    Branch: {r.branch} · {r.nodeCount} nodes
                  </p>
                  <p className="mb-3 text-xs text-muted-foreground">
                    Last sync:{' '}
                    {r.syncedAt ? new Date(r.syncedAt).toLocaleString() : 'Never'}
                  </p>

                  {/* Job status */}
                  {job && (
                    <div className="mb-3">
                      {isRunning && (
                        <div className="space-y-1">
                          <div className="flex items-center justify-between text-xs text-primary">
                            <span>
                              Syncing{job.currentRepo ? ` ${job.currentRepo}` : ''}…
                            </span>
                            <span>{job.progressPercent}%</span>
                          </div>
                          <Progress value={job.progressPercent} />
                        </div>
                      )}
                      {isDone && (
                        <p className="text-xs text-emerald-600 dark:text-emerald-400">
                          ✓ Sync completed
                        </p>
                      )}
                      {isFailed && (
                        <p className="text-xs text-destructive">
                          ✗ Sync failed
                          {job.errors[0] ? `: ${job.errors[0]}` : ''}
                        </p>
                      )}
                    </div>
                  )}

                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      onClick={() => startSync(r.name)}
                      disabled={isRunning}
                      className="gap-1.5"
                    >
                      <RefreshCw className={cn('h-3 w-3', isRunning && 'animate-spin')} />
                      {isRunning ? 'Syncing…' : 'Sync'}
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        if (confirm(`Remove "${r.name}" from analysis? (cloned files are kept)`)) {
                          removeMutation.mutate(r.name)
                        }
                      }}
                      disabled={removeMutation.isPending}
                      className="gap-1.5 text-destructive hover:text-destructive"
                    >
                      <Trash2 className="h-3 w-3" />
                      Remove
                    </Button>
                  </div>
                </CardContent>
              </Card>
            )
          })}
        </div>
      )}

      <AddRepoModal open={showAddModal} onClose={() => setShowAddModal(false)} />
      <ScanDirectoryModal open={showScanModal} onClose={() => setShowScanModal(false)} />
    </div>
  )
}