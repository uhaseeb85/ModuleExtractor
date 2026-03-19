import { useCallback, useMemo, useState } from 'react'
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  type Node,
  type Edge,
  type NodeProps,
  Handle,
  Position,
  useNodesState,
  useEdgesState,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { X } from 'lucide-react'
import type { GraphEdgeResponse, GraphNodeResponse } from '../api/client'
import { cn } from '@/lib/utils'
import { Button } from '@/components/ui/button'

const PALETTE = [
  '#6366f1', '#10b981', '#f59e0b', '#ef4444',
  '#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6',
  '#f97316', '#84cc16', '#06b6d4', '#a855f7',
]

function repoColour(repo: string, index: Map<string, number>): string {
  if (!index.has(repo)) index.set(repo, index.size)
  return PALETTE[index.get(repo)! % PALETTE.length]
}

// ── Custom node ──────────────────────────────────────────────────────

type ClassNodeData = {
  label: string
  repo: string
  colour: string
  methodCount: number
  classType: string
}

function ClassNode({ data, selected }: NodeProps<Node<ClassNodeData>>) {
  return (
    <div
      className={cn(
        'rounded-lg border bg-card px-3 py-2 shadow-sm transition-shadow',
        selected && 'ring-2 ring-primary shadow-md'
      )}
      style={{ borderLeftWidth: 4, borderLeftColor: data.colour }}
    >
      <Handle type="target" position={Position.Top} className="!bg-muted-foreground !w-2 !h-2" />
      <p className="text-xs font-semibold text-foreground truncate max-w-[140px]">
        {data.label}
      </p>
      <p className="text-[10px] text-muted-foreground">
        {data.classType} · {data.methodCount}m · {data.repo}
      </p>
      <Handle type="source" position={Position.Bottom} className="!bg-muted-foreground !w-2 !h-2" />
    </div>
  )
}

const nodeTypes = { classNode: ClassNode }

// ── Simple grid layout (rows per repo) ───────────────────────────────

function layoutNodes(
  raw: GraphNodeResponse[],
  repoIndex: Map<string, number>
): Node<ClassNodeData>[] {
  const groups = new Map<string, GraphNodeResponse[]>()
  for (const n of raw) {
    const list = groups.get(n.repoName) ?? []
    list.push(n)
    groups.set(n.repoName, list)
  }

  const result: Node<ClassNodeData>[] = []
  let yOffset = 0
  const COL_WIDTH = 220
  const ROW_HEIGHT = 80

  for (const [repo, members] of groups) {
    members.forEach((n, i) => {
      const cols = Math.max(4, Math.ceil(Math.sqrt(members.length)))
      result.push({
        id: n.fqn,
        type: 'classNode',
        position: {
          x: (i % cols) * COL_WIDTH,
          y: yOffset + Math.floor(i / cols) * ROW_HEIGHT,
        },
        data: {
          label: n.simpleName,
          repo,
          colour: repoColour(repo, repoIndex),
          methodCount: n.methodCount,
          classType: n.classType,
        },
      })
    })
    const cols = Math.max(4, Math.ceil(Math.sqrt(members.length)))
    yOffset += (Math.ceil(members.length / cols) + 1) * ROW_HEIGHT
  }
  return result
}

// ── Props ────────────────────────────────────────────────────────────

interface Props {
  nodes: GraphNodeResponse[]
  edges: GraphEdgeResponse[]
  onNodeClick?: (node: GraphNodeResponse) => void
}

export default function GraphCanvas({ nodes: rawNodes, edges: rawEdges, onNodeClick }: Props) {
  const [selected, setSelected] = useState<GraphNodeResponse | null>(null)
  const repoIndex = useMemo(() => new Map<string, number>(), [])

  const nodeLookup = useMemo(
    () => new Map(rawNodes.map((n) => [n.fqn, n])),
    [rawNodes]
  )

  const initialNodes = useMemo(
    () => layoutNodes(rawNodes, repoIndex),
    [rawNodes, repoIndex]
  )

  const initialEdges: Edge[] = useMemo(
    () =>
      rawEdges.map((e, i) => ({
        id: `e-${i}`,
        source: e.sourceFqn,
        target: e.targetFqn,
        animated: e.isCrossRepo,
        style: {
          stroke: e.isCrossRepo ? '#f97316' : 'hsl(var(--muted-foreground))',
          strokeWidth: e.isCrossRepo ? 2 : 1,
        },
        markerEnd: { type: 'arrowclosed' as const },
      })),
    [rawEdges]
  )

  const [flowNodes, , onNodesChange] = useNodesState(initialNodes)
  const [flowEdges, , onEdgesChange] = useEdgesState(initialEdges)

  const handleNodeClick = useCallback(
    (_: React.MouseEvent, node: Node) => {
      const gn = nodeLookup.get(node.id) ?? null
      setSelected(gn)
      if (gn) onNodeClick?.(gn)
    },
    [nodeLookup, onNodeClick]
  )

  return (
    <div style={{ width: '100%', height: '100%' }} className="relative flex">
      <ReactFlow
        nodes={flowNodes}
        edges={flowEdges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        nodeTypes={nodeTypes}
        onNodeClick={handleNodeClick}
        fitView
        minZoom={0.1}
        maxZoom={2}
        style={{ flex: 1 }}
      >
        <Background gap={20} size={1} />
        <Controls className="!bg-card !border-border !shadow-sm [&>button]:!bg-card [&>button]:!border-border [&>button]:!text-foreground" />
        <MiniMap
          nodeColor={(n) => (n.data as ClassNodeData)?.colour ?? '#94a3b8'}
          className="!bg-card !border-border"
        />
      </ReactFlow>

      {selected && (
        <aside className="w-72 shrink-0 overflow-y-auto border-l bg-card p-4 text-sm">
          <Button
            variant="ghost"
            size="icon"
            className="mb-3 h-6 w-6"
            onClick={() => setSelected(null)}
          >
            <X className="h-4 w-4" />
          </Button>
          <h2 className="mb-1 text-base font-bold text-foreground">{selected.simpleName}</h2>
          <p className="mb-3 break-all text-xs text-muted-foreground">{selected.fqn}</p>
          <dl className="space-y-1.5">
            {([
              ['Type', selected.classType],
              ['Repo', selected.repoName],
              ['Package', selected.packageName],
              ['Abstract', selected.isAbstract ? 'yes' : 'no'],
              ['Methods', String(selected.methodCount)],
            ] as const).map(([k, v]) => (
              <div key={k} className="flex justify-between">
                <dt className="text-muted-foreground">{k}</dt>
                <dd className="font-medium text-foreground">{v}</dd>
              </div>
            ))}
          </dl>
        </aside>
      )}
    </div>
  )
}
