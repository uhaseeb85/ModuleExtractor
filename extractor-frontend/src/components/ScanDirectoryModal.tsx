import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api, ScanDirectoryRequest } from '../api/client'

interface Props {
  open: boolean
  onClose: () => void
}

const EMPTY: ScanDirectoryRequest = {
  directoryPath: '',
  buildTool: 'MAVEN',
  branch: 'main',
}

export default function ScanDirectoryModal({ open, onClose }: Props) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState<ScanDirectoryRequest>(EMPTY)
  const [syncNow, setSyncNow] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [result, setResult] = useState<string[] | null>(null)

  const mutation = useMutation({
    mutationFn: () => api.scanDirectory(form, syncNow),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['repos'] })
      const names = data.registered ?? []
      if (names.length === 0) {
        setResult([])
      } else {
        setResult(names)
        setForm(EMPTY)
        setSyncNow(false)
        setError(null)
      }
    },
    onError: (err: Error) => {
      setError(err.message)
    },
  })

  if (!open) return null

  const handleClose = () => {
    setForm(EMPTY)
    setSyncNow(false)
    setError(null)
    setResult(null)
    onClose()
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!form.directoryPath.trim()) {
      setError('Directory path is required.')
      return
    }
    setError(null)
    setResult(null)
    mutation.mutate()
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-xl bg-gray-800 p-6 shadow-2xl">
        <h2 className="mb-1 text-lg font-semibold">Scan Local Directory</h2>
        <p className="mb-5 text-xs text-gray-400">
          Point to a local directory that contains one or more Git repositories.
          Each discovered repo is registered for module extraction using the same
          pipeline as GitHub repositories. Build tool is auto-detected from
          <code className="mx-0.5 rounded bg-gray-700 px-1 text-indigo-300">pom.xml</code>
          or
          <code className="mx-0.5 rounded bg-gray-700 px-1 text-indigo-300">build.gradle</code>.
        </p>

        {result !== null ? (
          <div className="mb-4">
            {result.length === 0 ? (
              <p className="rounded bg-yellow-900/40 px-3 py-2 text-xs text-yellow-300">
                No new Git repositories were found in that directory.
              </p>
            ) : (
              <div className="rounded bg-emerald-900/30 px-3 py-2 text-xs text-emerald-300">
                <p className="mb-1 font-semibold">
                  ✓ Registered {result.length} repo{result.length !== 1 ? 's' : ''}:
                </p>
                <ul className="list-inside list-disc space-y-0.5">
                  {result.map((name) => (
                    <li key={name}>{name}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="mb-1 block text-xs font-medium text-gray-300">
                Directory Path<span className="ml-0.5 text-red-400">*</span>
              </label>
              <input
                type="text"
                value={form.directoryPath}
                placeholder="/path/to/local/repos"
                onChange={(e) => setForm((f) => ({ ...f, directoryPath: e.target.value }))}
                className="w-full rounded border border-gray-600 bg-gray-900 px-3 py-1.5 text-sm text-white placeholder-gray-500 focus:border-indigo-500 focus:outline-none"
              />
              <p className="mt-1 text-xs text-gray-500">
                Absolute path. Can be a single repo or a folder containing multiple repos.
              </p>
            </div>

            <div>
              <label className="mb-1 block text-xs font-medium text-gray-300">
                Default Branch
              </label>
              <input
                type="text"
                value={form.branch}
                placeholder="main"
                onChange={(e) => setForm((f) => ({ ...f, branch: e.target.value }))}
                className="w-full rounded border border-gray-600 bg-gray-900 px-3 py-1.5 text-sm text-white placeholder-gray-500 focus:border-indigo-500 focus:outline-none"
              />
            </div>

            <div>
              <label className="mb-1 block text-xs font-medium text-gray-300">
                Fallback Build Tool
              </label>
              <select
                value={form.buildTool}
                onChange={(e) => setForm((f) => ({ ...f, buildTool: e.target.value }))}
                className="w-full rounded border border-gray-600 bg-gray-900 px-3 py-1.5 text-sm text-white focus:border-indigo-500 focus:outline-none"
              >
                <option value="MAVEN">Maven</option>
                <option value="GRADLE">Gradle</option>
              </select>
              <p className="mt-1 text-xs text-gray-500">
                Used only when neither pom.xml nor build.gradle is found.
              </p>
            </div>

            <label className="flex items-center gap-2 text-sm text-gray-300">
              <input
                type="checkbox"
                checked={syncNow}
                onChange={(e) => setSyncNow(e.target.checked)}
                className="rounded accent-indigo-500"
              />
              Start ingestion immediately after scanning
            </label>

            {error && (
              <p className="rounded bg-red-900/40 px-3 py-2 text-xs text-red-300">{error}</p>
            )}

            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={handleClose}
                className="rounded px-4 py-1.5 text-sm text-gray-400 hover:text-white"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={mutation.isPending}
                className="rounded bg-indigo-600 px-4 py-1.5 text-sm font-semibold hover:bg-indigo-500 disabled:opacity-60"
              >
                {mutation.isPending ? 'Scanning…' : 'Scan Directory'}
              </button>
            </div>
          </form>
        )}

        {result !== null && (
          <div className="flex justify-end pt-2">
            <button
              onClick={handleClose}
              className="rounded bg-gray-700 px-4 py-1.5 text-sm font-semibold hover:bg-gray-600"
            >
              Close
            </button>
          </div>
        )}
      </div>
    </div>
  )
}
