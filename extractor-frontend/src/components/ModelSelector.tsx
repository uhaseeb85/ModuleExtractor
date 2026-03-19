import { useState, useEffect } from 'react'
import { useQuery } from '@tanstack/react-query'
import { api, AiModelResponse } from '../api/client'
import { getApiKey, getModelId, setModelId } from '../api/ai-config'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Label } from '@/components/ui/label'

interface Props {
  /** Called whenever user picks a different model. */
  onChange?: (modelId: string) => void
  className?: string
}

export default function ModelSelector({ onChange, className }: Props) {
  const apiKey = getApiKey()
  const [selected, setSelected] = useState(() => getModelId())

  const { data: models, isLoading, isError } = useQuery<AiModelResponse[]>({
    queryKey: ['ai-models', apiKey],
    queryFn: () => api.getAiModels(apiKey),
    enabled: !!apiKey,
    staleTime: 5 * 60_000,
  })

  // Persist selection
  useEffect(() => {
    setModelId(selected)
    onChange?.(selected)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selected])

  if (!apiKey) return null

  const fmtPrice = (p: string | null | undefined) =>
    p ? `$${(parseFloat(p) * 1_000_000).toFixed(2)}/M` : '—'

  return (
    <div className={className}>
      <Label className="mb-1.5 block text-xs font-medium opacity-70">Model</Label>
      <Select value={selected} onValueChange={setSelected}>
        <SelectTrigger className="neu-inset w-full">
          <SelectValue placeholder={isLoading ? 'Loading…' : isError ? 'Error' : 'Choose model'} />
        </SelectTrigger>
        <SelectContent>
          {(models ?? []).map((m) => (
            <SelectItem key={m.id} value={m.id}>
              <span className="font-medium">{m.name}</span>
              <span className="ml-2 text-xs opacity-60">
                {fmtPrice(m.promptPrice)} / {fmtPrice(m.completionPrice)}
              </span>
            </SelectItem>
          ))}
        </SelectContent>
      </Select>
    </div>
  )
}
