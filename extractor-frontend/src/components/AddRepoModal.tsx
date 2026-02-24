import { useState, useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api, AddRepoRequest } from '../api/client'

interface Props {
  open: boolean
  onClose: () => void
}

const EMPTY: AddRepoRequest = {
  name: '',
  url: '',
  branch: 'main',
  buildTool: 'MAVEN',
  localPath: '',
}

export default function AddRepoModal({ open, onClose }: Props) {
  const queryClient = useQueryClient()
  const [form, setForm] = useState<AddRepoRequest>(EMPTY)
  const [localPathTouched, setLocalPathTouched] = useState(false)
  const [syncNow, setSyncNow] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // Auto-fill localPath from name unless user has manually edited it
  useEffect(() => {
    if (!localPathTouched) {
      setForm((f) => ({ ...f, localPath: f.name ? `/repos/${f.name}` : '' }))
    }
  }, [form.name, localPathTouched])

  const mutation = useMutation({
    mutationFn: () => api.addRepo(form, syncNow),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repos'] })
      setForm(EMPTY)
      setLocalPathTouched(false)
      setSyncNow(false)
      setError(null)
      onClose()
    },
    onError: (err: Error) => {
      setError(err.message)
    },
  })

  if (!open) return null

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!form.name.trim() || !form.url.trim()) {
      setError('Name and URL are required.')
      return
    }
    setError(null)
    mutation.mutate()
  }

  const field = (
    label: string,
    key: keyof AddRepoRequest,
    opts?: {
      placeholder?: string
      required?: boolean
      onManualEdit?: () => void
    }
  ) => (
    <div>
      <label className="mb-1 block text-xs font-medium text-gray-300">
        {label}
        {opts?.required && <span className="ml-0.5 text-red-400">*</span>}
      </label>
      <input
        type="text"
        value={form[key] as string}
        placeholder={opts?.placeholder}
        onChange={(e) => {
          opts?.onManualEdit?.()
          setForm((f) => ({ ...f, [key]: e.target.value }))
        }}
        className="w-full rounded border border-gray-600 bg-gray-900 px-3 py-1.5 text-sm text-white placeholder-gray-500 focus:border-indigo-500 focus:outline-none"
      />
    </div>
  )

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-xl bg-gray-800 p-6 shadow-2xl">
        <h2 className="mb-1 text-lg font-semibold">Add Repository</h2>
        <p className="mb-5 text-xs text-gray-400">
          Register a Git repository for analysis. Add multiple linked repos and
          sync them together — cross-repo dependencies are tracked automatically.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          {field('Name', 'name', { placeholder: 'payments-service', required: true })}
          {field('Git URL', 'url', {
            placeholder: 'https://github.com/org/repo.git',
            required: true,
          })}
          {field('Branch', 'branch', { placeholder: 'main' })}

          <div>
            <label className="mb-1 block text-xs font-medium text-gray-300">
              Build Tool
            </label>
            <select
              value={form.buildTool}
              onChange={(e) => setForm((f) => ({ ...f, buildTool: e.target.value }))}
              className="w-full rounded border border-gray-600 bg-gray-900 px-3 py-1.5 text-sm text-white focus:border-indigo-500 focus:outline-none"
            >
              <option value="MAVEN">Maven</option>
              <option value="GRADLE">Gradle</option>
            </select>
          </div>

          {field('Local Path (in container)', 'localPath', {
            placeholder: '/repos/payments-service',
            onManualEdit: () => setLocalPathTouched(true),
          })}

          <label className="flex items-center gap-2 text-sm text-gray-300">
            <input
              type="checkbox"
              checked={syncNow}
              onChange={(e) => setSyncNow(e.target.checked)}
              className="rounded accent-indigo-500"
            />
            Start ingestion immediately after adding
          </label>

          {error && (
            <p className="rounded bg-red-900/40 px-3 py-2 text-xs text-red-300">{error}</p>
          )}

          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => {
                setForm(EMPTY)
                setLocalPathTouched(false)
                setError(null)
                onClose()
              }}
              className="rounded px-4 py-1.5 text-sm text-gray-400 hover:text-white"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={mutation.isPending}
              className="rounded bg-indigo-600 px-4 py-1.5 text-sm font-semibold hover:bg-indigo-500 disabled:opacity-60"
            >
              {mutation.isPending ? 'Adding…' : 'Add Repository'}
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
