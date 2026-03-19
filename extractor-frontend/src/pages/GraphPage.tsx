import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import GraphCanvas from '../components/GraphCanvas'
import { Card, CardContent } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

export default function GraphPage() {
  const [repoFilter, setRepoFilter] = useState('')
  const [typeFilter, setTypeFilter] = useState('')
  const [crossRepoOnly, setCrossRepoOnly] = useState(false)

  const { data: nodes = [], isLoading: loadingNodes } = useQuery({
    queryKey: ['nodes', repoFilter, typeFilter],
    queryFn: () => api.getNodes({ repo: repoFilter || undefined, type: typeFilter || undefined }),
  })

  const { data: edges = [], isLoading: loadingEdges } = useQuery({
    queryKey: ['edges', repoFilter, crossRepoOnly],
    queryFn: () => api.getEdges({ repo: repoFilter || undefined, crossRepoOnly }),
  })

  const loading = loadingNodes || loadingEdges

  return (
    <div className="flex h-full flex-col">
      {/* Filter bar */}
      <Card className="mx-4 mt-4 shrink-0 rounded-lg">
        <CardContent className="flex flex-wrap items-end gap-4 px-4 py-3">
          <div className="space-y-1">
            <Label className="text-xs">Repo</Label>
            <Input
              placeholder="All repos"
              value={repoFilter}
              onChange={(e) => setRepoFilter(e.target.value)}
              className="h-8 w-40"
            />
          </div>
          <div className="space-y-1">
            <Label className="text-xs">Type</Label>
            <Select value={typeFilter} onValueChange={setTypeFilter}>
              <SelectTrigger className="h-8 w-36">
                <SelectValue placeholder="All" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">All</SelectItem>
                {['CLASS', 'INTERFACE', 'ENUM', 'ANNOTATION', 'RECORD'].map((t) => (
                  <SelectItem key={t} value={t}>{t}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="flex items-center gap-2 pb-0.5">
            <Checkbox
              id="cross-repo"
              checked={crossRepoOnly}
              onCheckedChange={(v) => setCrossRepoOnly(v === true)}
            />
            <Label htmlFor="cross-repo" className="text-xs font-normal">
              Cross-repo edges only
            </Label>
          </div>
          <span className="ml-auto pb-0.5 text-xs text-muted-foreground">
            {loading ? 'Loading…' : `${nodes.length} nodes · ${edges.length} edges`}
          </span>
        </CardContent>
      </Card>

      {/* Canvas */}
      <div className="relative flex-1 min-h-0">
        <div className="absolute inset-4 overflow-hidden rounded-lg border bg-card">
          {!loading && <GraphCanvas nodes={nodes} edges={edges} />}
        </div>
      </div>
    </div>
  )
}
