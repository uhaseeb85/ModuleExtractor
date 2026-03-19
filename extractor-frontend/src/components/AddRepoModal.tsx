import { useState, useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api, AddRepoRequest } from '../api/client'
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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!form.name.trim() || !form.url.trim()) {
      setError('Name and URL are required.')
      return
    }
    setError(null)
    mutation.mutate()
  }

  const handleClose = () => {
    setForm(EMPTY)
    setLocalPathTouched(false)
    setError(null)
    onClose()
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && handleClose()}>
      <DialogContent className="sm:max-w-md">
        <DialogHeader>
          <DialogTitle>Add Repository</DialogTitle>
          <DialogDescription>
            Register a Git repository for analysis. Cross-repo dependencies are
            tracked automatically.
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1.5">
            <Label htmlFor="repo-name">
              Name <span className="text-destructive">*</span>
            </Label>
            <Input
              id="repo-name"
              value={form.name}
              placeholder="payments-service"
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="repo-url">
              Git URL <span className="text-destructive">*</span>
            </Label>
            <Input
              id="repo-url"
              value={form.url}
              placeholder="https://github.com/org/repo.git"
              onChange={(e) => setForm((f) => ({ ...f, url: e.target.value }))}
            />
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="repo-branch">Branch</Label>
            <Input
              id="repo-branch"
              value={form.branch}
              placeholder="main"
              onChange={(e) => setForm((f) => ({ ...f, branch: e.target.value }))}
            />
          </div>

          <div className="space-y-1.5">
            <Label>Build Tool</Label>
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
          </div>

          <div className="space-y-1.5">
            <Label htmlFor="repo-path">Local Path (in container)</Label>
            <Input
              id="repo-path"
              value={form.localPath}
              placeholder="/repos/payments-service"
              onChange={(e) => {
                setLocalPathTouched(true)
                setForm((f) => ({ ...f, localPath: e.target.value }))
              }}
            />
          </div>

          <div className="flex items-center gap-2">
            <Checkbox
              id="sync-now"
              checked={syncNow}
              onCheckedChange={(v) => setSyncNow(v === true)}
            />
            <Label htmlFor="sync-now" className="text-sm font-normal">
              Start ingestion immediately after adding
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
              {mutation.isPending ? 'Adding…' : 'Add Repository'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
