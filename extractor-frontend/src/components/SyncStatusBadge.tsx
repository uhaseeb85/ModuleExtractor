import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { RefreshCw, CheckCircle2, XCircle } from 'lucide-react'
import { api, type SyncJobResponse } from '../api/client'

export default function SyncStatusBadge() {
  const [jobId, setJobId] = useState<string | null>(null)

  const { data: job } = useQuery<SyncJobResponse>({
    queryKey: ['sync-job', jobId],
    queryFn: () => api.getJobStatus(jobId!),
    enabled: !!jobId,
    refetchInterval: (query) => {
      const s = query.state.data?.status
      return s === 'RUNNING' || s === 'PENDING' ? 1500 : false
    },
  })

  const handleSync = async () => {
    try {
      const resp = await api.triggerFullSync()
      if (resp?.jobId) setJobId(resp.jobId)
    } catch { /* ignore */ }
  }

  const isRunning = job?.status === 'RUNNING' || job?.status === 'PENDING'
  const isDone    = job?.status === 'COMPLETED'
  const isFailed  = job?.status === 'FAILED'

  return (
    <div className="space-y-1.5 px-1">
      {job && (
        <div className="px-2 space-y-1">
          {isRunning && (
            <>
              <div className="flex items-center justify-between text-[10px] text-slate-400">
                <span className="flex items-center gap-1">
                  <span className="h-1.5 w-1.5 rounded-full bg-amber-400 animate-pulse" />
                  Syncing
                </span>
                <span>{job.progressPercent}%</span>
              </div>
              <div className="h-1 rounded-full bg-slate-700 overflow-hidden">
                <div
                  className="h-full rounded-full bg-indigo-500 transition-all duration-500"
                  style={{ width: `${job.progressPercent}%` }}
                />
              </div>
            </>
          )}
          {isDone && (
            <p className="flex items-center gap-1 text-[10px] text-emerald-400">
              <CheckCircle2 className="h-3 w-3" /> Sync complete
            </p>
          )}
          {isFailed && (
            <p className="flex items-center gap-1 text-[10px] text-red-400">
              <XCircle className="h-3 w-3" />
              {job.errors[0] ? job.errors[0].substring(0, 40) : 'Sync failed'}
            </p>
          )}
          {isDone && job.warnings?.length > 0 && (
            <p className="text-[10px] text-amber-400 truncate" title={job.warnings[0]}>
              {job.warnings[0]}
            </p>
          )}
        </div>
      )}
      <button
        onClick={handleSync}
        disabled={isRunning}
        className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-xs text-slate-400 hover:bg-slate-800 hover:text-slate-200 disabled:text-slate-600 disabled:cursor-not-allowed transition-colors"
      >
        <RefreshCw className="h-3.5 w-3.5 [.disabled_&]:animate-spin" />
        {isRunning ? 'Syncing...' : 'Sync All'}
      </button>
    </div>
  )
}
