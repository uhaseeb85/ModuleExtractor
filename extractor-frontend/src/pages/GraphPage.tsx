import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import GraphCanvas from '../components/GraphCanvas'

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
      <div className="flex items-center gap-4 border-b border-gray-800 bg-gray-900 px-4 py-2 text-sm">
        <label className="flex items-center gap-2">
          <span className="text-gray-400">Repo</span>
          <input
            className="rounded border border-gray-700 bg-gray-800 px-2 py-1 text-sm outline-none focus:border-indigo-500"
            placeholder="All repos"
            value={repoFilter}
            onChange={(e) => setRepoFilter(e.target.value)}
          />
        </label>
        <label className="flex items-center gap-2">
          <span className="text-gray-400">Type</span>
          <select
            className="rounded border border-gray-700 bg-gray-800 px-2 py-1 text-sm outline-none"
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
          >
            <option value="">All</option>
            {['CLASS', 'INTERFACE', 'ENUM', 'ANNOTATION', 'RECORD'].map((t) => (
              <option key={t}>{t}</option>
            ))}
          </select>
        </label>
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            checked={crossRepoOnly}
            onChange={(e) => setCrossRepoOnly(e.target.checked)}
          />
          <span className="text-gray-400">Cross-repo edges only</span>
        </label>
        <span className="ml-auto text-gray-500">
          {loading ? 'Loading…' : `${nodes.length} nodes · ${edges.length} edges`}
        </span>
      </div>

      {/* Canvas */}
      <div className="flex-1 overflow-hidden">
        {!loading && <GraphCanvas nodes={nodes} edges={edges} />}
      </div>
    </div>
  )
}
