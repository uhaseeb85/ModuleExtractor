import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Boxes, Sun, Moon, ArrowRight, Eye, EyeOff,
  Sparkles, BarChart3, Play,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import ModelSelector from '@/components/ModelSelector'
import { getApiKey, setApiKey, hasAiConfig } from '@/api/ai-config'

interface Props {
  dark: boolean
  toggleDark: () => void
}

type Strategy = 'ai' | 'static' | 'demo'

// ── Decorative background blobs ─────────────────────────────────────────────
function BlobBg() {
  return (
    <div className="pointer-events-none absolute inset-0 overflow-hidden" aria-hidden>
      <div className="absolute -top-20 -left-20 h-80 w-80 rounded-full bg-primary/10 blur-3xl animate-float" />
      <div className="absolute top-40 right-10 h-64 w-64 rounded-full bg-teal-400/10 blur-3xl animate-float [animation-delay:2s]" />
      <div className="absolute bottom-10 left-1/3 h-72 w-72 rounded-full bg-primary/8 blur-3xl animate-float [animation-delay:4s]" />
    </div>
  )
}

// ── Animated dependency graph graphic ───────────────────────────────────────
function GraphGraphic() {
  const nodes = [
    { cx: 50,  cy: 50,  r: 18, label: 'Core'     },
    { cx: 160, cy: 30,  r: 14, label: 'API'       },
    { cx: 160, cy: 90,  r: 14, label: 'Repo'      },
    { cx: 260, cy: 50,  r: 14, label: 'Service'   },
    { cx: 50,  cy: 150, r: 12, label: 'Utils'     },
    { cx: 160, cy: 160, r: 12, label: 'Model'     },
    { cx: 260, cy: 140, r: 10, label: 'DTO'       },
  ]
  const edges = [
    [0,1],[0,2],[1,3],[2,3],[0,4],[0,5],[5,6],[3,6],
  ]
  return (
    <svg viewBox="0 0 310 200" className="w-full opacity-90 drop-shadow-lg">
      <defs>
        <linearGradient id="edge-grad" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%"   stopColor="hsl(174 72% 46%)" stopOpacity="0.4" />
          <stop offset="100%" stopColor="hsl(186 80% 50%)" stopOpacity="0.8" />
        </linearGradient>
        <filter id="node-glow">
          <feGaussianBlur stdDeviation="2" result="blur" />
          <feMerge><feMergeNode in="blur" /><feMergeNode in="SourceGraphic" /></feMerge>
        </filter>
      </defs>
      {edges.map(([s, t], i) => (
        <line
          key={i}
          x1={nodes[s].cx} y1={nodes[s].cy}
          x2={nodes[t].cx} y2={nodes[t].cy}
          stroke="url(#edge-grad)" strokeWidth="1.5" strokeLinecap="round"
        />
      ))}
      {nodes.map((n, i) => (
        <g key={i} filter="url(#node-glow)">
          <circle
            cx={n.cx} cy={n.cy} r={n.r}
            fill="hsl(var(--background))"
            stroke="hsl(174 72% 46%)"
            strokeWidth="1.5"
            opacity={i === 0 ? 1 : 0.8}
          />
          <text
            x={n.cx} y={n.cy}
            textAnchor="middle" dominantBaseline="middle"
            fontSize="6.5" fontFamily="Inter, sans-serif" fontWeight="600"
            fill="hsl(174 72% 40%)"
          >
            {n.label}
          </text>
        </g>
      ))}
    </svg>
  )
}

// ── Strategy radio card ─────────────────────────────────────────────────────
function StrategyCard({
  icon: Icon, label, description, value, selected, onSelect,
}: {
  icon: React.ElementType
  label: string
  description: string
  value: Strategy
  selected: boolean
  onSelect: (v: Strategy) => void
}) {
  return (
    <button
      type="button"
      onClick={() => onSelect(value)}
      className={`w-full text-left rounded-xl p-4 transition-all ${
        selected ? 'neu-inset' : 'neu-raised-sm hover:scale-[1.01]'
      }`}
    >
      <div className="flex items-start gap-3">
        <div className={`mt-0.5 h-9 w-9 rounded-lg flex items-center justify-center shrink-0 ${
          selected ? 'bg-primary/15 text-primary' : 'bg-muted/50 text-muted-foreground'
        }`}>
          <Icon className="h-4.5 w-4.5" />
        </div>
        <div className="min-w-0">
          <p className={`text-sm font-semibold ${selected ? 'text-primary' : 'text-foreground'}`}>
            {label}
          </p>
          <p className="text-xs text-muted-foreground mt-0.5 leading-relaxed">{description}</p>
        </div>
        <div className={`ml-auto mt-1 h-4 w-4 rounded-full border-2 shrink-0 transition-colors ${
          selected ? 'border-primary bg-primary' : 'border-border'
        }`}>
          {selected && (
            <div className="h-full w-full rounded-full flex items-center justify-center">
              <div className="h-1.5 w-1.5 rounded-full bg-primary-foreground" />
            </div>
          )}
        </div>
      </div>
    </button>
  )
}

// ── Main component ───────────────────────────────────────────────────────────
export default function LandingPage({ dark, toggleDark }: Props) {
  const navigate = useNavigate()

  const [apiKey, setKey] = useState(() => getApiKey())
  const [showKey, setShowKey] = useState(false)
  const [strategy, setStrategy] = useState<Strategy>('static')

  const handleKeyChange = (v: string) => {
    setKey(v)
    setApiKey(v)
  }

  const canLaunch = strategy !== 'ai' || hasAiConfig()

  return (
    <div className="min-h-screen bg-background text-foreground overflow-x-hidden">
      {/* ── Navbar ──────────────────────────────────────────────────────── */}
      <header className="sticky top-0 z-50 bg-background/80 backdrop-blur-md border-b border-border/50">
        <div className="mx-auto max-w-6xl flex h-16 items-center justify-between px-6">
          <div className="flex items-center gap-3">
            <div className="h-9 w-9 rounded-xl neu-raised-sm flex items-center justify-center">
              <Boxes className="h-5 w-5 text-primary" />
            </div>
            <div>
              <p className="text-sm font-bold leading-none">ModuleExtractor</p>
              <p className="text-[10px] text-muted-foreground leading-none mt-0.5">Dependency Analyser</p>
            </div>
          </div>

          <button
            onClick={toggleDark}
            className="h-9 w-9 rounded-xl neu-btn flex items-center justify-center text-muted-foreground hover:text-foreground"
            aria-label="Toggle dark mode"
          >
            {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>
        </div>
      </header>

      {/* ── Two-column hero + config ────────────────────────────────────── */}
      <section className="relative min-h-[calc(100vh-4rem)] flex items-center overflow-hidden">
        <BlobBg />

        <div className="relative mx-auto max-w-6xl w-full px-6 py-16 grid lg:grid-cols-2 gap-16 items-center">
          {/* Left: branding + graph */}
          <div className="space-y-10 animate-slide-up">
            <div className="space-y-6">
              <div className="inline-flex items-center gap-2 neu-raised-sm rounded-full px-4 py-1.5 text-xs font-medium text-primary">
                <Sparkles className="h-3.5 w-3.5" />
                AI-Enhanced Decomposition
              </div>

              <h1 className="text-4xl md:text-5xl font-extrabold leading-tight tracking-tight">
                Understand your{' '}
                <span className="text-gradient-teal">Java monolith,</span>
                <br />
                extract with confidence
              </h1>

              <p className="text-lg text-muted-foreground leading-relaxed max-w-md">
                Map every class, interface, and dependency. Score extraction candidates.
                Optionally enhance results with AI-powered analysis via OpenRouter.
              </p>
            </div>

            {/* Graph card */}
            <div className="neu-raised rounded-3xl p-6 max-w-sm">
              <div className="neu-inset rounded-2xl p-5">
                <div className="animate-float">
                  <GraphGraphic />
                </div>
              </div>
              <div className="mt-3 flex items-center justify-between gap-2">
                <div className="flex-1 neu-raised-sm rounded-xl px-3 py-2">
                  <p className="text-[10px] text-muted-foreground">Nodes</p>
                  <p className="text-lg font-bold text-foreground">2,847</p>
                </div>
                <div className="flex-1 neu-raised-sm rounded-xl px-3 py-2">
                  <p className="text-[10px] text-muted-foreground">Edges</p>
                  <p className="text-lg font-bold text-foreground">9,132</p>
                </div>
                <div className="flex-1 neu-raised-sm rounded-xl px-3 py-2">
                  <p className="text-[10px] text-muted-foreground">Score</p>
                  <p className="text-lg font-bold text-primary">87%</p>
                </div>
              </div>
            </div>

            <div className="flex flex-wrap gap-3">
              {['Maven & Gradle', 'Java 8+', 'No database', 'JGraphT'].map((tag) => (
                <span key={tag} className="text-xs text-muted-foreground neu-flat rounded-full px-3 py-1">
                  {tag}
                </span>
              ))}
            </div>
          </div>

          {/* Right: config form */}
          <div className="animate-slide-up [animation-delay:150ms]">
            <div className="neu-raised rounded-3xl p-8 space-y-7">
              <div className="space-y-1">
                <h2 className="text-xl font-bold text-foreground">Get Started</h2>
                <p className="text-sm text-muted-foreground">
                  Choose your analysis strategy to begin.
                </p>
              </div>

              {/* ── Analysis Strategy ───────────────────────── */}
              <div className="space-y-3">
                <Label className="section-header">Analysis Strategy</Label>
                <div className="space-y-2.5">
                  <StrategyCard
                    icon={Sparkles}
                    label="AI Analysis"
                    description="AI-enhanced decomposition powered by OpenRouter. Refine boundaries, generate migration plans, identify bounded contexts."
                    value="ai"
                    selected={strategy === 'ai'}
                    onSelect={setStrategy}
                  />
                  <StrategyCard
                    icon={BarChart3}
                    label="Static Analysis"
                    description="Graph-based scoring using JGraphT. Fast, deterministic, no API key required."
                    value="static"
                    selected={strategy === 'static'}
                    onSelect={setStrategy}
                  />
                  <StrategyCard
                    icon={Play}
                    label="Demo Mode"
                    description="Explore the UI with sample data. No repository or API key needed."
                    value="demo"
                    selected={strategy === 'demo'}
                    onSelect={setStrategy}
                  />
                </div>
              </div>

              {/* ── AI Config (conditional) ─────────────────── */}
              {strategy === 'ai' && (
                <div className="space-y-4 animate-slide-up">
                  <div className="space-y-2">
                    <Label htmlFor="api-key" className="text-xs font-medium">
                      OpenRouter API Key
                    </Label>
                    <div className="relative">
                      <Input
                        id="api-key"
                        type={showKey ? 'text' : 'password'}
                        placeholder="sk-or-…"
                        value={apiKey}
                        onChange={(e) => handleKeyChange(e.target.value)}
                        className="neu-inset pr-10"
                        autoComplete="off"
                      />
                      <button
                        type="button"
                        onClick={() => setShowKey(!showKey)}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                        aria-label={showKey ? 'Hide key' : 'Show key'}
                      >
                        {showKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                      </button>
                    </div>
                    <p className="text-[11px] text-muted-foreground">
                      Stored in your browser only. Never sent to our servers.
                    </p>
                  </div>

                  {apiKey && (
                    <ModelSelector className="animate-slide-up" />
                  )}
                </div>
              )}

              {/* ── Launch button ───────────────────────────── */}
              <Button
                size="lg"
                className="w-full gap-2 rounded-xl text-base"
                disabled={!canLaunch}
                onClick={() => navigate('/dashboard')}
              >
                Launch App <ArrowRight className="h-4.5 w-4.5" />
              </Button>

              {!canLaunch && (
                <p className="text-xs text-destructive text-center">
                  Please enter your API key and select a model to continue.
                </p>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* ── Footer ──────────────────────────────────────────────────────── */}
      <footer className="border-t border-border/50 py-8">
        <div className="mx-auto max-w-6xl px-6 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-muted-foreground">
          <div className="flex items-center gap-2">
            <Boxes className="h-4 w-4 text-primary" />
            <span className="font-semibold text-foreground">ModuleExtractor</span>
            <span>· Dependency Analyser</span>
          </div>
          <span>AI-Enhanced Decomposition</span>
        </div>
      </footer>
    </div>
  )
}
