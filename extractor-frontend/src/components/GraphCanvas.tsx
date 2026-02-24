import { useEffect, useRef, useState } from 'react'
import cytoscape from 'cytoscape'
// @ts-expect-error — no official TS types for this layout
import coseBilkent from 'cytoscape-cose-bilkent'
import type { GraphEdgeResponse, GraphNodeResponse } from '../api/client'

cytoscape.use(coseBilkent)

// ── Palette: one colour per repo (up to 12) ─────────────────────────
const PALETTE = [
  '#6366f1', '#10b981', '#f59e0b', '#ef4444',
  '#3b82f6', '#8b5cf6', '#ec4899', '#14b8a6',
  '#f97316', '#84cc16', '#06b6d4', '#a855f7',
]

function repoColour(repo: string, index: Map<string, number>): string {
  if (!index.has(repo)) index.set(repo, index.size)
  return PALETTE[index.get(repo)! % PALETTE.length]
}

interface Props {
  nodes: GraphNodeResponse[]
  edges: GraphEdgeResponse[]
  onNodeClick?: (node: GraphNodeResponse) => void
}

/**
 * Cytoscape.js canvas.
 * - Node size scales with methodCount (coupling proxy).
 * - Node colour is unique per repo.
 * - Cross-repo edges are highlighted in orange; same-repo edges in grey.
 * - Clicking a node fires {@link onNodeClick}.
 */
export default function GraphCanvas({ nodes, edges, onNodeClick }: Props) {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const cyRef = useRef<cytoscape.Core | null>(null)
  const [selected, setSelected] = useState<GraphNodeResponse | null>(null)
  const repoIndex = new Map<string, number>()

  // Build a lookup from fqn → node for click handler
  const nodeLookup = new Map(nodes.map((n) => [n.fqn, n]))

  useEffect(() => {
    if (!containerRef.current) return

    const cy: cytoscape.Core = cytoscape({
      container: containerRef.current,
      elements: [
        ...nodes.map((n) => ({
          data: {
            id: n.fqn,
            label: n.simpleName,
            repo: n.repoName,
            colour: repoColour(n.repoName, repoIndex),
            size: Math.max(20, Math.min(60, 20 + n.methodCount * 2)),
          },
        })),
        ...edges.map((e, i) => ({
          data: {
            id: `e-${i}`,
            source: e.sourceFqn,
            target: e.targetFqn,
            crossRepo: e.isCrossRepo,
          },
        })),
      ],
      style: [
        {
          selector: 'node',
          style: {
            'background-color': 'data(colour)',
            'label': 'data(label)',
            'color': '#f1f5f9',
            'font-size': '10px',
            'text-valign': 'center' as const,
            'text-halign': 'center' as const,
            'text-wrap': 'ellipsis' as const,
            'text-max-width': '80px',
            'width': 'data(size)',
            'height': 'data(size)',
          },
        },
        {
          selector: 'node:selected',
          style: {
            'border-width': 3,
            'border-color': '#f8fafc',
          },
        },
        {
          selector: 'edge',
          style: {
            'line-color': '#475569',
            'target-arrow-color': '#475569',
            'target-arrow-shape': 'triangle' as const,
            'curve-style': 'bezier' as const,
            'width': 1,
          },
        },
        {
          selector: 'edge[?crossRepo]',
          style: {
            'line-color': '#f97316',
            'target-arrow-color': '#f97316',
            'width': 2,
          },
        },
      ],
      layout: {
        name: 'cose-bilkent',
        animate: false,
        nodeDimensionsIncludeLabels: true,
      } as cytoscape.LayoutOptions,
      wheelSensitivity: 0.3,
    })

    cy.on('tap', 'node', (evt: cytoscape.EventObject) => {
      const fqn: string = evt.target.id()
      const node = nodeLookup.get(fqn) ?? null
      setSelected(node)
      onNodeClick?.(node!)
    })

    cyRef.current = cy
    return () => cy.destroy()
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [nodes, edges])

  return (
    <div className="relative flex h-full w-full">
      {/* Canvas */}
      <div ref={containerRef} className="flex-1 bg-gray-950" />

      {/* Side panel */}
      {selected && (
        <aside className="w-72 shrink-0 overflow-y-auto border-l border-gray-700 bg-gray-900 p-4 text-sm">
          <button
            className="mb-3 text-xs text-gray-400 hover:text-white"
            onClick={() => setSelected(null)}
          >
            ✕ Close
          </button>
          <h2 className="mb-1 text-base font-bold">{selected.simpleName}</h2>
          <p className="mb-3 break-all text-xs text-gray-400">{selected.fqn}</p>
          <dl className="space-y-1">
            {[
              ['Type', selected.classType],
              ['Repo', selected.repoName],
              ['Package', selected.packageName],
              ['Abstract', selected.isAbstract ? 'yes' : 'no'],
              ['Methods', String(selected.methodCount)],
            ].map(([k, v]) => (
              <div key={k} className="flex justify-between">
                <dt className="text-gray-400">{k}</dt>
                <dd className="font-medium">{v}</dd>
              </div>
            ))}
          </dl>
        </aside>
      )}
    </div>
  )
}
