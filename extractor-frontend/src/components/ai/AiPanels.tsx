import { useState } from 'react'
import type { AiAnalysisResponse, AiPipelineResponse } from '../../api/client'
import {
  Sparkles, AlertTriangle, Loader2, ChevronDown, ChevronRight,
  GitBranch, Map, Layers,
} from 'lucide-react'
import { Badge } from '@/components/ui/badge'

// ── Shared helpers ───────────────────────────────────────────────────

function TokenBadge({ resp }: { resp: AiAnalysisResponse }) {
  return (
    <div className="flex items-center gap-3 text-xs text-muted-foreground">
      {resp.modelUsed && <Badge variant="secondary" className="text-[10px]">{resp.modelUsed}</Badge>}
      {(resp.promptTokens != null || resp.completionTokens != null) && (
        <span>{resp.promptTokens ?? 0} + {resp.completionTokens ?? 0} tokens</span>
      )}
    </div>
  )
}

function parseJsonContent(content: string | null): unknown | null {
  if (!content) return null
  try {
    return JSON.parse(content)
  } catch {
    return null
  }
}

function ErrorCard({ error }: { error: string }) {
  return (
    <div className="flex flex-col items-center justify-center gap-3 py-8 text-sm text-center">
      <div className="rounded-full bg-destructive/10 p-3">
        <AlertTriangle className="h-5 w-5 text-destructive" />
      </div>
      <p className="font-medium text-foreground">Analysis failed</p>
      <p className="text-xs text-muted-foreground max-w-md">{error}</p>
    </div>
  )
}

// ── Section wrapper ──────────────────────────────────────────────────

function AiSection({
  title,
  icon: Icon,
  resp,
  defaultOpen = true,
  children,
}: {
  title: string
  icon: React.ElementType
  resp: AiAnalysisResponse
  defaultOpen?: boolean
  children: React.ReactNode
}) {
  const [open, setOpen] = useState(defaultOpen)

  if (resp.error) {
    return (
      <div className="rounded-xl neu-raised-sm overflow-hidden">
        <button
          onClick={() => setOpen(v => !v)}
          className="w-full flex items-center gap-2 px-4 py-3 text-left hover:neu-flat transition-all"
        >
          <Icon className="h-4 w-4 text-destructive" />
          <span className="text-sm font-medium text-foreground flex-1">{title}</span>
          <Badge variant="destructive" className="text-[10px]">Error</Badge>
          {open ? <ChevronDown className="h-4 w-4 text-muted-foreground" /> : <ChevronRight className="h-4 w-4 text-muted-foreground" />}
        </button>
        {open && (
          <div className="border-t px-4 pb-4 pt-3">
            <ErrorCard error={resp.error} />
          </div>
        )}
      </div>
    )
  }

  return (
    <div className="rounded-xl neu-raised-sm overflow-hidden">
      <button
        onClick={() => setOpen(v => !v)}
        className="w-full flex items-center gap-2 px-4 py-3 text-left hover:neu-flat transition-all"
      >
        <Icon className="h-4 w-4 text-primary" />
        <span className="text-sm font-medium text-foreground flex-1">{title}</span>
        <TokenBadge resp={resp} />
        {open ? <ChevronDown className="h-4 w-4 text-muted-foreground" /> : <ChevronRight className="h-4 w-4 text-muted-foreground" />}
      </button>
      {open && (
        <div className="border-t px-4 pb-4 pt-3">
          {children}
        </div>
      )}
    </div>
  )
}

// ── Boundaries panel ─────────────────────────────────────────────────

function BoundariesContent({ resp }: { resp: AiAnalysisResponse }) {
  const json = parseJsonContent(resp.content) as Record<string, unknown> | null

  if (!json) {
    return (
      <pre className="whitespace-pre-wrap text-xs text-foreground font-mono leading-relaxed">
        {resp.content}
      </pre>
    )
  }

  const suggested = (json.suggested_moves ?? json.suggestedMoves ?? []) as Array<Record<string, string>>
  const reasoning = (json.reasoning ?? json.summary ?? '') as string

  return (
    <div className="space-y-3">
      {reasoning && (
        <p className="text-xs text-muted-foreground leading-relaxed">{String(reasoning)}</p>
      )}
      {suggested.length > 0 && (
        <div className="space-y-2">
          <h4 className="text-xs font-semibold uppercase tracking-wide text-muted-foreground">Suggested Moves</h4>
          {suggested.map((item, i) => (
            <div key={i} className="rounded-lg bg-accent/30 p-3 space-y-1">
              <p className="text-xs font-medium text-foreground">{item.class || item.className || item.name || `Item ${i + 1}`}</p>
              <p className="text-xs text-muted-foreground">{item.reason || item.rationale || ''}</p>
              {item.from && item.to && (
                <p className="text-[10px] text-muted-foreground/70 font-mono">{item.from} → {item.to}</p>
              )}
            </div>
          ))}
        </div>
      )}
      {!reasoning && suggested.length === 0 && (
        <pre className="whitespace-pre-wrap text-xs text-foreground font-mono leading-relaxed">
          {JSON.stringify(json, null, 2)}
        </pre>
      )}
    </div>
  )
}

// ── Migration plan panel ─────────────────────────────────────────────

function MigrationContent({ resp }: { resp: AiAnalysisResponse }) {
  const json = parseJsonContent(resp.content) as Record<string, unknown> | null

  if (!json) {
    return (
      <pre className="whitespace-pre-wrap text-xs text-foreground font-mono leading-relaxed">
        {resp.content}
      </pre>
    )
  }

  const phases = (json.phases ?? json.steps ?? json.plan ?? []) as Array<Record<string, unknown>>
  const overview = (json.overview ?? json.summary ?? '') as string

  return (
    <div className="space-y-3">
      {overview && (
        <p className="text-xs text-muted-foreground leading-relaxed">{String(overview)}</p>
      )}
      {Array.isArray(phases) && phases.length > 0 ? (
        <div className="space-y-2">
          {phases.map((phase, i) => (
            <div key={i} className="rounded-lg border border-border/50 p-3 space-y-1.5">
              <div className="flex items-center gap-2">
                <span className="flex h-5 w-5 items-center justify-center rounded-full bg-primary/10 text-[10px] font-bold text-primary">
                  {i + 1}
                </span>
                <span className="text-xs font-medium text-foreground">
                  {String(phase.name ?? phase.title ?? phase.phase ?? `Phase ${i + 1}`)}
                </span>
              </div>
              <p className="text-xs text-muted-foreground pl-7">
                {String(phase.description ?? phase.details ?? '')}
              </p>
              {Array.isArray(phase.tasks) && (
                <ul className="pl-7 space-y-1">
                  {(phase.tasks as string[]).map((t, j) => (
                    <li key={j} className="flex gap-2 text-xs text-muted-foreground">
                      <span className="mt-0.5 h-1.5 w-1.5 shrink-0 rounded-full bg-primary/40" />
                      {String(t)}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          ))}
        </div>
      ) : (
        <pre className="whitespace-pre-wrap text-xs text-foreground font-mono leading-relaxed">
          {JSON.stringify(json, null, 2)}
        </pre>
      )}
    </div>
  )
}

// ── Bounded contexts panel ───────────────────────────────────────────

function ContextsContent({ resp }: { resp: AiAnalysisResponse }) {
  const json = parseJsonContent(resp.content) as Record<string, unknown> | null

  if (!json) {
    return (
      <pre className="whitespace-pre-wrap text-xs text-foreground font-mono leading-relaxed">
        {resp.content}
      </pre>
    )
  }

  const contexts = (json.bounded_contexts ?? json.boundedContexts ?? json.contexts ?? []) as Array<Record<string, unknown>>

  return (
    <div className="space-y-3">
      {Array.isArray(contexts) && contexts.length > 0 ? (
        <div className="grid gap-2">
          {contexts.map((ctx, i) => (
            <div key={i} className="rounded-lg bg-accent/30 p-3 space-y-2">
              <div className="flex items-center gap-2">
                <Layers className="h-3.5 w-3.5 text-primary" />
                <span className="text-xs font-medium text-foreground">
                  {String(ctx.name ?? ctx.context ?? `Context ${i + 1}`)}
                </span>
              </div>
              {ctx.description != null && (
                <p className="text-xs text-muted-foreground">{String(ctx.description)}</p>
              )}
              {Array.isArray(ctx.modules) && (
                <div className="flex flex-wrap gap-1">
                  {(ctx.modules as string[]).map((m, j) => (
                    <Badge key={j} variant="secondary" className="text-[10px]">{String(m)}</Badge>
                  ))}
                </div>
              )}
              {Array.isArray(ctx.packages) && (
                <div className="flex flex-wrap gap-1">
                  {(ctx.packages as string[]).map((p, j) => (
                    <span key={j} className="inline-block rounded-md neu-flat px-2 py-0.5 font-mono text-[10px] text-muted-foreground">
                      {String(p)}
                    </span>
                  ))}
                </div>
              )}
            </div>
          ))}
        </div>
      ) : (
        <pre className="whitespace-pre-wrap text-xs text-foreground font-mono leading-relaxed">
          {JSON.stringify(json, null, 2)}
        </pre>
      )}
    </div>
  )
}

// ── Combined pipeline panel ──────────────────────────────────────────

export function AiPipelinePanel({
  result,
  loading,
}: {
  result: AiPipelineResponse | null
  loading: boolean
}) {
  if (loading) {
    return (
      <div className="flex flex-col items-center justify-center h-full gap-3 text-muted-foreground text-sm">
        <Loader2 className="h-6 w-6 animate-spin text-primary" />
        <div className="text-center">
          <p className="font-medium">Running AI pipeline...</p>
          <p className="text-xs text-muted-foreground/60 mt-1">
            Analyzing boundaries, generating migration plan, identifying contexts
          </p>
        </div>
      </div>
    )
  }

  if (!result) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-center px-6">
        <div className="rounded-full neu-flat p-4 mb-4">
          <Sparkles className="h-8 w-8 text-muted-foreground/50" />
        </div>
        <p className="text-sm font-medium text-muted-foreground">AI analysis will run automatically</p>
        <p className="mt-1 text-xs text-muted-foreground/60">
          Select a module to trigger the AI pipeline
        </p>
      </div>
    )
  }

  return (
    <div className="h-full overflow-y-auto p-6 space-y-4">
      <AiSection title="Boundary Refinement" icon={GitBranch} resp={result.boundaries}>
        <BoundariesContent resp={result.boundaries} />
      </AiSection>

      <AiSection title="Migration Plan" icon={Map} resp={result.migration}>
        <MigrationContent resp={result.migration} />
      </AiSection>

      <AiSection title="Bounded Contexts" icon={Layers} resp={result.contexts} defaultOpen={false}>
        <ContextsContent resp={result.contexts} />
      </AiSection>
    </div>
  )
}

// ── Legacy single-result panel (for manual AI buttons in static mode) ──

export function AiResultPanel({ result, loading }: { result: AiAnalysisResponse | null; loading: boolean }) {
  if (loading) {
    return (
      <div className="flex items-center justify-center h-full gap-2 text-muted-foreground text-sm">
        <Loader2 className="h-4 w-4 animate-spin" /> Running AI analysis...
      </div>
    )
  }
  if (!result) {
    return (
      <div className="flex flex-col items-center justify-center h-full text-center px-6">
        <div className="rounded-full neu-flat p-4 mb-4">
          <Sparkles className="h-8 w-8 text-muted-foreground/50" />
        </div>
        <p className="text-sm font-medium text-muted-foreground">No AI analysis yet</p>
        <p className="mt-1 text-xs text-muted-foreground/60">
          Use the AI buttons in the toolbar to run an analysis on the selected module
        </p>
      </div>
    )
  }
  if (result.error) {
    return <ErrorCard error={result.error} />
  }

  const json = parseJsonContent(result.content)

  return (
    <div className="h-full overflow-y-auto p-6 space-y-4">
      <TokenBadge resp={result} />
      <div className="rounded-xl neu-raised-sm p-4">
        {json ? (
          <pre className="whitespace-pre-wrap text-xs text-foreground font-mono leading-relaxed">
            {JSON.stringify(json, null, 2)}
          </pre>
        ) : (
          <pre className="whitespace-pre-wrap text-xs text-foreground font-mono leading-relaxed">
            {result.content}
          </pre>
        )}
      </div>
    </div>
  )
}
