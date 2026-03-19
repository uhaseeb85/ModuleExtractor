import { useQuery } from '@tanstack/react-query'
import { api, type ProjectTreeNodeResponse } from '../api/client'
import { FileCode, Copy, Check, Loader2, AlertCircle, ArrowRight } from 'lucide-react'
import { useState, useCallback } from 'react'
import { cn } from '@/lib/utils'

interface FilePreviewProps {
  node: ProjectTreeNodeResponse
  moduleName: string
  repoName: string
}

export default function FilePreview({ node, moduleName, repoName }: FilePreviewProps) {
  const [copied, setCopied] = useState(false)

  const { data, isLoading, error } = useQuery({
    queryKey: ['scaffold-file', moduleName, repoName, node.path],
    queryFn: () => api.getScaffoldFile(moduleName, repoName, node.path),
    enabled: node.hasContent,
  })

  const content = data?.content ?? null

  const copyToClipboard = useCallback(() => {
    if (!content) return
    navigator.clipboard.writeText(content).then(() => {
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    })
  }, [content])

  // Source-reference only (no generated content)
  if (!node.hasContent && node.sourceRef) {
    return (
      <div className="h-full flex flex-col">
        <Header name={node.name} />
        <div className="flex-1 flex items-center justify-center p-8">
          <div className="rounded-xl neu-raised-sm p-6 text-center max-w-md space-y-3">
            <ArrowRight className="mx-auto h-8 w-8 text-blue-400" />
            <p className="text-sm font-medium text-foreground">Move from source</p>
            <p className="text-xs text-muted-foreground font-mono break-all">
              {node.sourceRef}
            </p>
            <p className="text-xs text-muted-foreground/70">
              This file will be copied from the original repository during module extraction.
            </p>
          </div>
        </div>
      </div>
    )
  }

  // No content at all
  if (!node.hasContent) {
    return (
      <div className="h-full flex flex-col">
        <Header name={node.name} />
        <div className="flex-1 flex items-center justify-center text-muted-foreground text-sm">
          No preview available
        </div>
      </div>
    )
  }

  return (
    <div className="h-full flex flex-col">
      <div className="flex items-center justify-between border-b px-4 py-2">
        <div className="flex items-center gap-2">
          <FileCode className="h-4 w-4 text-muted-foreground" />
          <span className="font-mono text-xs text-foreground">{node.name}</span>
        </div>
        <button
          onClick={copyToClipboard}
          disabled={!content}
          className={cn(
            'flex items-center gap-1 rounded-md px-2 py-1 text-xs transition-colors',
            'hover:bg-accent text-muted-foreground',
            copied && 'text-emerald-500',
          )}
        >
          {copied ? <Check className="h-3.5 w-3.5" /> : <Copy className="h-3.5 w-3.5" />}
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>

      <div className="flex-1 overflow-auto">
        {isLoading ? (
          <div className="flex items-center justify-center h-full">
            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
          </div>
        ) : error ? (
          <div className="flex items-center justify-center h-full gap-2 text-destructive text-sm">
            <AlertCircle className="h-4 w-4" />
            Failed to load file
          </div>
        ) : (
          <pre className="p-4 text-xs font-mono leading-relaxed text-foreground whitespace-pre overflow-x-auto">
            {content?.split('\n').map((line, i) => (
              <div key={i} className="flex">
                <span className="inline-block w-10 shrink-0 text-right pr-4 text-muted-foreground/40 select-none">
                  {i + 1}
                </span>
                <span>{line}</span>
              </div>
            ))}
          </pre>
        )}
      </div>
    </div>
  )
}

function Header({ name }: { name: string }) {
  return (
    <div className="flex items-center gap-2 border-b px-4 py-2">
      <FileCode className="h-4 w-4 text-muted-foreground" />
      <span className="font-mono text-xs text-foreground">{name}</span>
    </div>
  )
}
