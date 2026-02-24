import { useQuery } from '@tanstack/react-query'
import { api } from '../api/client'

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

  return (
    <div className="p-6">
      <h1 className="mb-6 text-2xl font-bold">Dashboard</h1>

      {/* Summary cards */}
      <div className="mb-8 grid grid-cols-2 gap-4 sm:grid-cols-4">
        {[
          ['Repositories', String(repos?.length ?? '…')],
          ['Total Nodes', String(totalNodes || '…')],
          ['Shared Entities', String(sharedEntities?.length ?? '…')],
          ['Cross-Repo Risks', String(sharedEntities?.length ?? '…')],
        ].map(([label, value]) => (
          <div key={label} className="rounded-lg bg-gray-800 p-5">
            <div className="text-3xl font-bold">{value}</div>
            <div className="mt-1 text-sm text-gray-400">{label}</div>
          </div>
        ))}
      </div>

      {/* Repo table */}
      <section>
        <h2 className="mb-3 text-lg font-semibold">Repositories</h2>
        {isLoading ? (
          <p className="text-gray-400">Loading…</p>
        ) : (
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-gray-700 text-gray-400">
                <th className="pb-2 pr-4">Name</th>
                <th className="pb-2 pr-4">Build</th>
                <th className="pb-2 pr-4">Branch</th>
                <th className="pb-2 pr-4">Nodes</th>
                <th className="pb-2 pr-4">Last Sync</th>
              </tr>
            </thead>
            <tbody>
              {repos?.map((r) => (
                <tr key={r.name} className="border-b border-gray-800 hover:bg-gray-800">
                  <td className="py-2 pr-4 font-medium">{r.name}</td>
                  <td className="py-2 pr-4 text-gray-300">{r.buildTool}</td>
                  <td className="py-2 pr-4 text-gray-300">{r.branch}</td>
                  <td className="py-2 pr-4">{r.nodeCount}</td>
                  <td className="py-2 pr-4 text-gray-400">
                    {r.syncedAt ? new Date(r.syncedAt).toLocaleString() : 'Never'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>

      {/* Shared entities warning */}
      {sharedEntities && sharedEntities.length > 0 && (
        <section className="mt-8">
          <h2 className="mb-3 text-lg font-semibold text-orange-400">
            ⚠ Shared Entities ({sharedEntities.length})
          </h2>
          <ul className="space-y-1 text-sm">
            {sharedEntities.map((e) => (
              <li key={e.fqn} className="rounded bg-gray-800 px-3 py-2">
                <span className="font-medium">{e.simpleName}</span>
                <span className="ml-2 text-gray-400">{e.fqn}</span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
