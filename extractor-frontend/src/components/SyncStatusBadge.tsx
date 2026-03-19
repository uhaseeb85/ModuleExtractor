import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { RefreshCw } from 'lucide-react'
import { api, type SyncJobResponse } from '../api/client'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'

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

  const isRunning = job?.status === 'RUNNING' || job?.status === 'PENDING'

  return (
    <div className="flex items-center gap-2">
      {job && (
        <div className="flex flex-col items-end gap-1">
          <Badge
            variant={
              job.status === 'COMPLETED' ? 'default' :
              job.status === 'FAILED' ? 'destructive' :
              'secondary'
            }
            className={cn(
              'gap-1.5',
              isRunning && 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-300'
            )}
          >
            {isRunning && (
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-amber-500 opacity-75" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-amber-500" />
              </span>
            )}
            {job.status}
            {isRunning && ` ${job.progressPercent}%`}
          </Badge>
          {job.warnings?.length > 0 && job.status === 'COMPLETED' && (
            <span className="max-w-xs truncate text-right text-xs text-amber-600 dark:text-amber-400"
              title={job.warnings.join('\n')}>
              ⚠ {job.warnings[0]}
            </span>
          )}
          {job.errors?.length > 0 && (
            <span className="max-w-xs truncate text-right text-xs text-destructive"
              title={job.errors.join('\n')}>
              {job.errors[0]}
            </span>
          )}
        </div>
      )}
      <Button
        variant="outline"
        size="sm"
        onClick={handleSync}
        disabled={isRunning}
        className="h-8 gap-1.5 text-xs"
      >
        <RefreshCw className={cn('h-3 w-3', isRunning && 'animate-spin')} />
        Sync All
      </Button>
    </div>
  )
}
