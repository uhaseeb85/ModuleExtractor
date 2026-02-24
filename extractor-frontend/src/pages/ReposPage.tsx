import { useState, useEffect, useCallback } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { api, SyncJobResponse } from '../api/client'
import AddRepoModal from '../components/AddRepoModal'

export default function ReposPage() {
  const queryClient = useQueryClient()
  const [showAddModal, setShowAddModal] = useState(false)
  // repoName → latest job response
  const [jobs, setJobs] = useState<Record<string, SyncJobResponse>>({})

  const { data: repos = [], isLoading } = useQuery({
    queryKey: ['repos'],
    queryFn: api.getRepos,
  })

  const removeMutation = useMutation({
    mutationFn: (name: string) => api.removeRepo(name),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['repos'] }),
  })

  // Poll active jobs every 1.5 s
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
      // pin the full-sync job under a special key and also under each repo
      const allKey = '__all__'
      setJobs((prev) => ({ ...prev, [allKey]: job }))
    } catch (err) {
      alert(`Failed to start sync: ${(err as Error).message}`)
    }
  }

  const jobFor = (name: string) => jobs[name] ?? jobs['__all__'] ?? null

  return (
    <div className="p-6">
      {/* Header */}
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold">Repositories</h1>
        <div className="flex gap-2">
          {repos.length > 0 && (
            <button
              onClick={startSyncAll}
              disabled={jobs['__all__']?.status === 'RUNNING'}
              className="rounded bg-gray-700 px-3 py-1.5 text-xs font-semibold hover:bg-gray-600 disabled:opacity-60"
            >
              {jobs['__all__']?.status === 'RUNNING'
                ? `Syncing all… ${jobs['__all__'].progressPercent}%`
                : 'Sync All'}
            </button>
          )}
          <button
            onClick={() => setShowAddModal(true)}
            className="rounded bg-indigo-600 px-3 py-1.5 text-xs font-semibold hover:bg-indigo-500"
          >
            + Add Repository
          </button>
        </div>
      </div>

      {/* Multi-repo hint */}
      {repos.length > 1 && (
        <p className="mb-4 rounded bg-indigo-900/30 px-4 py-2 text-xs text-indigo-300">
          {repos.length} repositories configured — cross-repo dependencies are linked automatically during sync.
        </p>
      )}

      {isLoading ? (
        <p className="text-gray-400">Loading…</p>
      ) : repos.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-gray-600 py-16 text-center">
          <p className="mb-2 text-gray-400">No repositories registered yet.</p>
          <button
            onClick={() => setShowAddModal(true)}
            className="mt-2 rounded bg-indigo-600 px-4 py-2 text-sm font-semibold hover:bg-indigo-500"
          >
            Add Your First Repository
          </button>
        </div>
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {repos.map((r) => {
            const job = jobFor(r.name)
            const isRunning = job?.status === 'RUNNING' || job?.status === 'PENDING'
            const isFailed = job?.status === 'FAILED'
            const isDone = job?.status === 'COMPLETED'
            return (
              <div key={r.name} className="rounded-lg bg-gray-800 p-5">
                <div className="mb-2 flex items-center justify-between">
                  <h2 className="font-semibold">{r.name}</h2>
                  <span className="rounded bg-gray-700 px-2 py-0.5 text-xs">
                    {r.buildTool}
                  </span>
                </div>
                <p className="mb-1 break-all text-xs text-gray-400">{r.url}</p>
                <p className="mb-3 text-xs text-gray-500">
                  Branch: {r.branch} · {r.nodeCount} nodes
                </p>
                <p className="mb-3 text-xs text-gray-500">
                  Last sync:{' '}
                  {r.syncedAt ? new Date(r.syncedAt).toLocaleString() : 'Never'}
                </p>

                {/* Job status strip */}
                {job && (
                  <div className="mb-3">
                    {isRunning && (
                      <>
                        <div className="mb-1 flex items-center justify-between text-xs text-indigo-300">
                          <span>
                            Syncing{job.currentRepo ? ` ${job.currentRepo}` : ''}…
                          </span>
                          <span>{job.progressPercent}%</span>
                        </div>
                        <div className="h-1.5 w-full rounded bg-gray-700">
                          <div
                            className="h-1.5 rounded bg-indigo-500 transition-all duration-500"
                            style={{ width: `${Math.max(job.progressPercent, 4)}%` }}
                          />
                        </div>
                      </>
                    )}
                    {isDone && (
                      <p className="text-xs text-emerald-400">✓ Sync completed</p>
                    )}
                    {isFailed && (
                      <p className="text-xs text-red-400">
                        ✗ Sync failed
                        {job.errors[0] ? `: ${job.errors[0]}` : ''}
                      </p>
                    )}
                  </div>
                )}

                <div className="flex gap-2">
                  <button
                    onClick={() => startSync(r.name)}
                    disabled={isRunning}
                    className="rounded bg-indigo-600 px-3 py-1 text-xs font-semibold hover:bg-indigo-500 disabled:opacity-60"
                  >
                    {isRunning ? 'Syncing…' : 'Sync'}
                  </button>
                  <button
                    onClick={() => {
                      if (confirm(`Remove "${r.name}" from analysis? (cloned files are kept)`)) {
                        removeMutation.mutate(r.name)
                      }
                    }}
                    disabled={removeMutation.isPending}
                    className="rounded border border-red-700 px-3 py-1 text-xs text-red-400 hover:bg-red-900/40 disabled:opacity-60"
                  >
                    Remove
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}

      <AddRepoModal open={showAddModal} onClose={() => setShowAddModal(false)} />
    </div>
  )
}