import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'
import { GitFork, Box, AlertTriangle, Share2 } from 'lucide-react'
import { Card, CardContent } from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Badge } from '@/components/ui/badge'
import SectionHeader from '@/components/SectionHeader'

const STAT_ICONS = [GitFork, Box, Share2, AlertTriangle] as const

export default function Dashboard() {
  const { data: repos, isLoading } = useQuery({
    queryKey: ['repos'],
    queryFn: api.getRepos,
  })

  const { data: sharedEntities } = useQuery({
    queryKey: ['shared-entities'],
    queryFn: api.getSharedEntities,
  })

  const totalNodes = repos?.reduce((s, r) => s + r.nodeCount, 0) ?? 0

  const stats = [
    { label: 'Repositories', value: repos?.length ?? '—' },
    { label: 'Total Nodes', value: totalNodes || '—' },
    { label: 'Shared Entities', value: sharedEntities?.length ?? '—' },
    { label: 'Cross-Repo Risks', value: sharedEntities?.length ?? '—' },
  ]

  return (
    <div className="space-y-8 p-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Dashboard</h1>
        <p className="mt-1 text-sm text-muted-foreground">
          Overview of your registered repositories and dependency analysis
        </p>
      </div>

      {/* Stat cards */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        {stats.map((s, i) => {
          const Icon = STAT_ICONS[i]
          return (
            <Card key={s.label}>
              <CardContent className="flex items-center gap-4 p-5">
                <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-primary/10 text-primary">
                  <Icon className="h-5 w-5" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-foreground">{s.value}</p>
                  <p className="text-xs text-muted-foreground">{s.label}</p>
                </div>
              </CardContent>
            </Card>
          )
        })}
      </div>

      {/* Repo table */}
      <section>
        <SectionHeader>Repositories</SectionHeader>
        {isLoading ? (
          <p className="text-sm text-muted-foreground">Loading…</p>
        ) : (
          <Card>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Build</TableHead>
                  <TableHead>Branch</TableHead>
                  <TableHead className="text-right">Nodes</TableHead>
                  <TableHead>Last Sync</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {repos?.map((r) => (
                  <TableRow key={r.name}>
                    <TableCell className="font-medium">{r.name}</TableCell>
                    <TableCell>
                      <Badge variant="secondary">{r.buildTool}</Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{r.branch}</TableCell>
                    <TableCell className="text-right">{r.nodeCount}</TableCell>
                    <TableCell className="text-muted-foreground">
                      {r.syncedAt ? new Date(r.syncedAt).toLocaleString() : 'Never'}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Card>
        )}
      </section>

      {/* Shared entities warning */}
      {sharedEntities && sharedEntities.length > 0 && (
        <section>
          <SectionHeader>
            <span className="flex items-center gap-2">
              <AlertTriangle className="h-4 w-4 text-amber-500" />
              Shared Entities ({sharedEntities.length})
            </span>
          </SectionHeader>
          <div className="space-y-1.5">
            {sharedEntities.map((e) => (
              <Card key={e.fqn}>
                <CardContent className="flex items-center gap-3 px-4 py-3">
                  <span className="font-medium text-foreground">{e.simpleName}</span>
                  <span className="text-xs text-muted-foreground">{e.fqn}</span>
                </CardContent>
              </Card>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}
