import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, type SyncJobResponse } from '../api/client'

/**
 * Polls the last known job every 2 s while it is running.
 * Shows a coloured badge: IDLE | RUNNING | COMPLETED | FAILED.
 */
export default function SyncStatusBadge() {
  const [jobId, setJobId] = useState<string | null>(null)

  const { data: job } = useQuery<SyncJobResponse>({
    queryKey: ['sync-job', jobId],
    queryFn: () => api.getJobStatus(jobId!),
    enabled: !!jobId,
    refetchInterval: (query) => {
      const s = query.state.data?.status
      return s === 'RUNNING' || s === 'PENDING' ? 2000 : false
    },
  })

  const handleSync = async () => {
    const resp = await api.triggerFullSync()
    if (resp?.jobId) setJobId(resp.jobId)
  }

  const colour =
    job?.status === 'RUNNING' ? 'bg-yellow-500' :
    job?.status === 'COMPLETED' ? 'bg-green-500' :
    job?.status === 'FAILED' ? 'bg-red-500' :
    'bg-gray-600'

  return (
    <div className="flex items-center gap-3">
      {job && (
        <span className={`inline-flex items-center gap-1.5 rounded-full px-3 py-1 text-xs font-medium ${colour}`}>
          {job.status === 'RUNNING' && (
            <span className="h-2 w-2 animate-ping rounded-full bg-white opacity-75" />
          )}
          {job.status}
          {job.status === 'RUNNING' && ` — ${job.progressPercent}%`}
        </span>
      )}
      <button
        onClick={handleSync}
        className="rounded bg-indigo-600 px-3 py-1 text-xs font-semibold hover:bg-indigo-500"
      >
        Sync All
      </button>
    </div>
  )
}
