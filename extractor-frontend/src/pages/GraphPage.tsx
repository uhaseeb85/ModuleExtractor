import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import GraphCanvas from '../components/GraphCanvas'
import { api } from '../api/client'
import { Share2, Filter, Loader2 } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import {
  Select, SelectContent, SelectItem, SelectTrigger, SelectValue,
} from '@/components/ui/select'

export default function GraphPage() {
  const [repoFilter, setRepoFilter]  = useState('')
  const [typeFilter, setTypeFilter]  = useState<string>('ALL')
  const [crossRepo,  setCrossRepo]   = useState(false)

  const { data: nodes, isLoading } = useQuery({
    queryKey: ['graph-nodes', repoFilter, typeFilter, crossRepo],
    queryFn: () =>
      api.getNodes({ repo: repoFilter || undefined, type: typeFilter !== 'ALL' ? typeFilter : undefined }),
  })

  const { data: edges } = useQuery({
    queryKey: ['graph-edges', repoFilter, crossRepo],
    queryFn: () =>
      api.getEdges({ repo: repoFilter || undefined, crossRepoOnly: crossRepo || undefined }),
  })

  const nodeCount = nodes?.length ?? 0

  return (
    <div className="flex h-full flex-col">
      {/* Toolbar */}
      <div className="flex shrink-0 items-center gap-3 border-b bg-background px-6 py-3">
        <div className="flex items-center gap-1 text-xs text-muted-foreground">
          <Filter className="h-3.5 w-3.5" />
          <span>Filter</span>
        </div>
        <Input
          placeholder="Repository name�"
          value={repoFilter}
          onChange={(e) => setRepoFilter(e.target.value)}
          className="h-8 w-44 text-xs"
        />
        <Select value={typeFilter} onValueChange={setTypeFilter}>
          <SelectTrigger className="h-8 w-36 text-xs">
            <SelectValue placeholder="All types" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">All types</SelectItem>
            <SelectItem value="CLASS">Class</SelectItem>
            <SelectItem value="INTERFACE">Interface</SelectItem>
            <SelectItem value="ENUM">Enum</SelectItem>
            <SelectItem value="ANNOTATION">Annotation</SelectItem>
          </SelectContent>
        </Select>
        <label className="flex cursor-pointer items-center gap-1.5 text-xs text-muted-foreground select-none">
          <input
            type="checkbox"
            checked={crossRepo}
            onChange={(e) => setCrossRepo(e.target.checked)}
            className="rounded"
          />
          Cross-repo only
        </label>

        <div className="ml-auto flex items-center gap-3">
          {isLoading ? (
            <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
              <Loader2 className="h-3.5 w-3.5 animate-spin" />Loading�
            </span>
          ) : (
            <Badge variant="secondary" className="text-xs font-mono">
              {nodeCount.toLocaleString()} node{nodeCount !== 1 ? 's' : ''}
            </Badge>
          )}
        </div>
      </div>

      {/* Canvas area */}
      <div className="relative flex-1 min-h-0">
        {!isLoading && nodeCount === 0 ? (
          <div className="absolute inset-0 flex flex-col items-center justify-center text-center">
            <Share2 className="mb-4 h-12 w-12 text-muted-foreground/20" />
            <p className="text-sm font-medium text-muted-foreground">No graph data</p>
            <p className="mt-1 text-xs text-muted-foreground/60">
              Sync a repository first, then return here to explore dependencies
            </p>
          </div>
        ) : (
          <div className="absolute inset-2 overflow-hidden rounded-2xl neu-raised">
            <GraphCanvas nodes={nodes ?? []} edges={edges ?? []} />
          </div>
        )}
      </div>
    </div>
  )
}
