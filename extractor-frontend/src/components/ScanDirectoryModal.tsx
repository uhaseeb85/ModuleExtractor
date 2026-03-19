import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api, ScanDirectoryRequest } from '../api/client'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Button } from '@/components/ui/button'
import { Checkbox } from '@/components/ui/checkbox'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'

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
    <Dialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Scan Local Directory</DialogTitle>
          <DialogDescription>
            Point to a local directory containing Git repositories. Build tool is
            auto-detected from{' '}
            <code className="rounded bg-muted px-1 font-mono text-xs">pom.xml</code> or{' '}
            <code className="rounded bg-muted px-1 font-mono text-xs">build.gradle</code>.
          </DialogDescription>
        </DialogHeader>

        {result !== null ? (
          <div className="space-y-4">
            {result.length === 0 ? (
              <p className="rounded-md bg-amber-50 px-3 py-2 text-xs text-amber-700 dark:bg-amber-900/30 dark:text-amber-300">
                No new Git repositories were found in that directory.
              </p>
            ) : (
              <div className="rounded-md bg-emerald-50 px-3 py-2 text-xs text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-300">
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
            <DialogFooter>
              <Button variant="outline" onClick={handleClose}>
                Close
              </Button>
            </DialogFooter>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="space-y-1.5">
              <Label htmlFor="dir-path">
                Directory Path <span className="text-destructive">*</span>
              </Label>
              <Input
                id="dir-path"
                value={form.directoryPath}
                placeholder="/path/to/local/repos"
                onChange={(e) =>
                  setForm((f) => ({ ...f, directoryPath: e.target.value }))
                }
              />
              <p className="text-xs text-muted-foreground">
                Absolute path. Can be a single repo or a folder containing
                multiple repos.
              </p>
            </div>

            <div className="space-y-1.5">
              <Label htmlFor="dir-branch">Default Branch</Label>
              <Input
                id="dir-branch"
                value={form.branch}
                placeholder="main"
                onChange={(e) =>
                  setForm((f) => ({ ...f, branch: e.target.value }))
                }
              />
            </div>

            <div className="space-y-1.5">
              <Label>Fallback Build Tool</Label>
              <Select
                value={form.buildTool}
                onValueChange={(v) => setForm((f) => ({ ...f, buildTool: v }))}
              >
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="MAVEN">Maven</SelectItem>
                  <SelectItem value="GRADLE">Gradle</SelectItem>
                </SelectContent>
              </Select>
              <p className="text-xs text-muted-foreground">
                Used only when neither pom.xml nor build.gradle is found.
              </p>
            </div>

            <div className="flex items-center gap-2">
              <Checkbox
                id="dir-sync"
                checked={syncNow}
                onCheckedChange={(v) => setSyncNow(v === true)}
              />
              <Label htmlFor="dir-sync" className="text-sm font-normal">
                Start ingestion immediately after scanning
              </Label>
            </div>

            {error && (
              <p className="rounded-md bg-destructive/10 px-3 py-2 text-xs text-destructive">
                {error}
              </p>
            )}

            <DialogFooter>
              <Button type="button" variant="outline" onClick={handleClose}>
                Cancel
              </Button>
              <Button type="submit" disabled={mutation.isPending}>
                {mutation.isPending ? 'Scanning…' : 'Scan Directory'}
              </Button>
            </DialogFooter>
          </form>
        )}
      </DialogContent>
    </Dialog>
  )
}
