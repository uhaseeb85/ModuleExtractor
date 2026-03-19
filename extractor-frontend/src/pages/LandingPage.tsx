import { useNavigate } from 'react-router-dom'
import {
  GitFork, Puzzle, Share2, Server, Boxes,
  Sun, Moon, ArrowRight, ChevronRight,
  Database, Zap, Code2, GitBranch,
} from 'lucide-react'
import { Button } from '@/components/ui/button'

interface Props {
  dark: boolean
  toggleDark: () => void
}

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
    <div className="w-full max-w-xs mx-auto animate-float">
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
        {/* Edges */}
        {edges.map(([s, t], i) => (
          <line
            key={i}
            x1={nodes[s].cx} y1={nodes[s].cy}
            x2={nodes[t].cx} y2={nodes[t].cy}
            stroke="url(#edge-grad)" strokeWidth="1.5" strokeLinecap="round"
          />
        ))}
        {/* Nodes */}
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
    </div>
  )
}

// ── Feature card ─────────────────────────────────────────────────────────────
function FeatureCard({
  icon: Icon, title, description, delay = 0,
}: {
  icon: React.ElementType
  title: string
  description: string
  delay?: number
}) {
  return (
    <div
      className="neu-raised rounded-2xl p-6 space-y-4 animate-slide-up"
      style={{ animationDelay: `${delay}ms` }}
    >
      <div className="h-11 w-11 rounded-xl neu-raised-sm flex items-center justify-center">
        <Icon className="h-5 w-5 text-primary" />
      </div>
      <div>
        <h3 className="font-semibold text-foreground text-base">{title}</h3>
        <p className="mt-1.5 text-sm text-muted-foreground leading-relaxed">{description}</p>
      </div>
    </div>
  )
}

// ── Step badge ───────────────────────────────────────────────────────────────
function Step({ num, title, desc }: { num: number; title: string; desc: string }) {
  return (
    <div className="flex flex-col items-center text-center space-y-3 animate-slide-up">
      <div className="h-12 w-12 rounded-full neu-raised flex items-center justify-center text-lg font-bold text-primary">
        {num}
      </div>
      <div>
        <p className="font-semibold text-foreground">{title}</p>
        <p className="mt-1 text-sm text-muted-foreground">{desc}</p>
      </div>
    </div>
  )
}

// ── Stat pill ────────────────────────────────────────────────────────────────
function StatPill({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col items-center gap-1 neu-flat rounded-2xl px-8 py-4">
      <span className="text-2xl font-bold text-gradient-teal">{value}</span>
      <span className="text-xs text-muted-foreground uppercase tracking-wide">{label}</span>
    </div>
  )
}

// ── Main component ───────────────────────────────────────────────────────────
export default function LandingPage({ dark, toggleDark }: Props) {
  const navigate = useNavigate()

  const features = [
    {
      icon: GitFork,
      title: 'Visual Dependency Graph',
      description: 'Explore class-level relationships and cross-module references through an interactive, filterable graph powered by JGraphT.',
      delay: 0,
    },
    {
      icon: Puzzle,
      title: 'Smart Module Candidates',
      description: 'AI-scored extraction recommendations rank which packages are safest to pull into independent modules — with composite isolation scores.',
      delay: 100,
    },
    {
      icon: Share2,
      title: 'Cross-Repo Analysis',
      description: 'Add multiple repositories simultaneously. Shared entities and cross-repo dependencies are mapped and highlighted automatically.',
      delay: 200,
    },
    {
      icon: Server,
      title: 'Zero Infrastructure',
      description: 'No database, no message broker — just Maven or Gradle and a JDK. The entire dependency graph lives in-memory with JGraphT.',
      delay: 300,
    },
  ]

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

          <nav className="hidden md:flex items-center gap-1">
            {['Features', 'How it works', 'Get started'].map((item) => (
              <button
                key={item}
                onClick={() => {
                  const el = document.getElementById(item.toLowerCase().replace(' ', '-'))
                  el?.scrollIntoView({ behavior: 'smooth' })
                }}
                className="px-3 py-1.5 text-sm text-muted-foreground hover:text-foreground rounded-lg hover:neu-flat transition-all"
              >
                {item}
              </button>
            ))}
          </nav>

          <div className="flex items-center gap-2">
            <button
              onClick={toggleDark}
              className="h-9 w-9 rounded-xl neu-btn flex items-center justify-center text-muted-foreground hover:text-foreground"
              aria-label="Toggle dark mode"
            >
              {dark ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
            </button>
            <Button
              variant="default"
              size="sm"
              className="gap-1.5 rounded-xl"
              onClick={() => navigate('/dashboard')}
            >
              Launch App <ArrowRight className="h-3.5 w-3.5" />
            </Button>
          </div>
        </div>
      </header>

      {/* ── Hero ────────────────────────────────────────────────────────── */}
      <section className="relative py-24 md:py-32 overflow-hidden">
        <BlobBg />
        <div className="relative mx-auto max-w-6xl px-6 grid md:grid-cols-2 gap-16 items-center">
          <div className="space-y-8 animate-slide-up">
            <div className="inline-flex items-center gap-2 neu-raised-sm rounded-full px-4 py-1.5 text-xs font-medium text-primary">
              <Zap className="h-3.5 w-3.5" />
              Phase 1 · Foundation &amp; Visibility
            </div>

            <h1 className="text-4xl md:text-5xl font-extrabold leading-tight tracking-tight">
              Understand your{' '}
              <span className="text-gradient-teal">Java monolith,</span>
              <br />
              extract with confidence
            </h1>

            <p className="text-lg text-muted-foreground leading-relaxed max-w-md">
              Map every class, interface, and dependency in your codebase.
              Score module extraction candidates. Make informed strangler-fig decisions — backed by data.
            </p>

            <div className="flex flex-wrap gap-3">
              <Button
                size="lg"
                className="gap-2 rounded-xl px-8"
                onClick={() => navigate('/dashboard')}
              >
                Launch App <ArrowRight className="h-4 w-4" />
              </Button>
              <Button
                size="lg"
                variant="outline"
                className="gap-2 rounded-xl px-8"
                onClick={() => {
                  const el = document.getElementById('features')
                  el?.scrollIntoView({ behavior: 'smooth' })
                }}
              >
                See features <ChevronRight className="h-4 w-4" />
              </Button>
            </div>

            <div className="flex flex-wrap gap-4 pt-2">
              {['Maven &amp; Gradle', 'Java 8+', 'No database required', 'JGraphT-powered'].map((tag) => (
                <span key={tag} className="text-xs text-muted-foreground neu-flat rounded-full px-3 py-1"
                  dangerouslySetInnerHTML={{ __html: tag }} />
              ))}
            </div>
          </div>

          <div className="relative flex items-center justify-center">
            <div className="relative w-full max-w-sm">
              {/* Outer neumorphic frame */}
              <div className="neu-raised rounded-3xl p-8">
                <div className="neu-inset rounded-2xl p-6">
                  <GraphGraphic />
                </div>
                {/* Floating info chips */}
                <div className="mt-4 flex items-center justify-between gap-2">
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
            </div>
          </div>
        </div>
      </section>

      {/* ── Stats bar ───────────────────────────────────────────────────── */}
      <section className="py-10 border-y border-border/50">
        <div className="mx-auto max-w-4xl px-6">
          <div className="flex flex-wrap justify-center gap-4 md:gap-0 md:divide-x divide-border">
            <StatPill value="Maven + Gradle" label="Build tools" />
            <StatPill value="In-memory" label="Graph store" />
            <StatPill value="Java 8–21" label="Supported JDKs" />
            <StatPill value="Phase 1" label="Current release" />
          </div>
        </div>
      </section>

      {/* ── Features grid ───────────────────────────────────────────────── */}
      <section id="features" className="py-24">
        <div className="mx-auto max-w-6xl px-6 space-y-12">
          <div className="text-center space-y-4">
            <p className="text-xs font-semibold uppercase tracking-widest text-primary">Features</p>
            <h2 className="text-3xl font-bold text-foreground">
              Everything you need for monolith decomposition
            </h2>
            <p className="text-muted-foreground max-w-xl mx-auto">
              From raw Git repositories to actionable module extraction plans — all in one clean UI.
            </p>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((f) => (
              <FeatureCard key={f.title} {...f} />
            ))}
          </div>
        </div>
      </section>

      {/* ── How it works ────────────────────────────────────────────────── */}
      <section id="how-it-works" className="py-24 bg-background">
        <div className="mx-auto max-w-4xl px-6 space-y-16">
          <div className="text-center space-y-4">
            <p className="text-xs font-semibold uppercase tracking-widest text-primary">Workflow</p>
            <h2 className="text-3xl font-bold text-foreground">Up and running in three steps</h2>
          </div>

          <div className="grid md:grid-cols-3 gap-8 relative">
            {/* Connector line (desktop only) */}
            <div className="hidden md:block absolute top-6 left-[calc(16.67%+24px)] right-[calc(16.67%+24px)] h-px bg-gradient-to-r from-primary/30 via-primary/60 to-primary/30" />

            <Step
              num={1}
              title="Add Repository"
              desc="Point ModuleExtractor at a local path or Git URL. Maven and Gradle projects are detected automatically."
            />
            <Step
              num={2}
              title="Sync &amp; Analyse"
              desc="JavaParser scans every .java file, extracts classes, interfaces, and enums, then builds the JGraphT dependency graph."
            />
            <Step
              num={3}
              title="Extract Modules"
              desc="Browse the visual graph, review scored extraction candidates, and identify which packages can safely become standalone services."
            />
          </div>
        </div>
      </section>

      {/* ── Tech stack callout ──────────────────────────────────────────── */}
      <section className="py-16 border-y border-border/50">
        <div className="mx-auto max-w-5xl px-6">
          <div className="neu-raised rounded-3xl p-8 md:p-12">
            <div className="grid md:grid-cols-2 gap-10 items-center">
              <div className="space-y-4">
                <p className="text-xs font-semibold uppercase tracking-widest text-primary">Tech Stack</p>
                <h2 className="text-2xl font-bold text-foreground">Built for Java teams, by developers</h2>
                <p className="text-muted-foreground text-sm leading-relaxed">
                  Spring Boot backend with JGit, JavaParser &amp; JGraphT.
                  React 18 + Vite frontend with React Query and XY Flow.
                  No external dependencies — docker-compose optional.
                </p>
              </div>
              <div className="grid grid-cols-2 gap-3">
                {[
                  { icon: Code2,     label: 'JavaParser', sub: 'AST analysis'      },
                  { icon: Database,  label: 'JGraphT',    sub: 'In-memory graph'   },
                  { icon: GitBranch, label: 'JGit',       sub: 'Git integration'   },
                  { icon: Zap,       label: 'React 18',   sub: 'Vite + Tailwind'   },
                ].map(({ icon: Icon, label, sub }) => (
                  <div key={label} className="neu-inset-sm rounded-xl p-4 flex items-center gap-3">
                    <Icon className="h-5 w-5 text-primary shrink-0" />
                    <div>
                      <p className="text-sm font-semibold text-foreground">{label}</p>
                      <p className="text-xs text-muted-foreground">{sub}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ── CTA banner ──────────────────────────────────────────────────── */}
      <section id="get-started" className="py-28">
        <div className="mx-auto max-w-2xl px-6 text-center space-y-8">
          <div className="relative inline-block">
            <div className="h-20 w-20 rounded-3xl neu-raised mx-auto flex items-center justify-center">
              <Boxes className="h-10 w-10 text-primary" />
            </div>
          </div>
          <div className="space-y-4">
            <h2 className="text-4xl font-extrabold text-foreground">Ready to refactor?</h2>
            <p className="text-muted-foreground text-lg">
              No sign-up. No cloud account. Just start the app and point it at your monolith.
            </p>
          </div>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <Button
              size="lg"
              className="gap-2 rounded-xl px-10 text-base"
              onClick={() => navigate('/dashboard')}
            >
              Launch App <ArrowRight className="h-5 w-5" />
            </Button>
            <Button
              size="lg"
              variant="outline"
              className="gap-2 rounded-xl px-10 text-base"
              onClick={() => navigate('/repos')}
            >
              Add Repository <GitFork className="h-4 w-4" />
            </Button>
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
          <span>Phase 1 · Foundation &amp; Visibility</span>
        </div>
      </footer>
    </div>
  )
}
