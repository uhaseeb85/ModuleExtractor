import { BrowserRouter, Link, Route, Routes, useLocation } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { LayoutDashboard, Puzzle, Sun, Moon, Boxes, Home, Sparkles, BarChart3 } from 'lucide-react'
import { cn } from '@/lib/utils'
import Dashboard from './pages/Dashboard'
import CandidatesPage from './pages/CandidatesPage'
import LandingPage from './pages/LandingPage'
import SyncStatusBadge from './components/SyncStatusBadge'
import { AnalysisModeProvider, useAnalysisMode } from './context/AnalysisModeContext'

const NAV_ITEMS = [
  { to: '/',           label: 'Home',                   icon: Home            },
  { to: '/dashboard',  label: 'Dashboard',              icon: LayoutDashboard },
  { to: '/candidates', label: 'Module Recommendations', icon: Puzzle          },
]

function Sidebar({ dark, toggleDark }: { dark: boolean; toggleDark: () => void }) {
  const location = useLocation()
  const { mode, aiAvailable, checkingAi } = useAnalysisMode()
  return (
    <aside className="flex h-screen w-58 shrink-0 flex-col bg-background border-r border-border/60">
      {/* Logo */}
      <div className="flex h-16 items-center gap-3 px-5">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary shadow-neu-raised-sm">
          <Boxes className="h-5 w-5 text-primary-foreground" />
        </div>
        <div>
          <p className="text-sm font-bold leading-none text-foreground">ModuleExtractor</p>
          <p className="mt-0.5 text-[10px] text-muted-foreground leading-none">Dependency Analyser</p>
        </div>
      </div>

      {/* Nav */}
      <nav className="flex-1 space-y-1.5 overflow-y-auto px-3 py-2">
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => {
          const active = location.pathname === to
          return (
            <Link
              key={to}
              to={to}
              className={cn(
                'flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-all duration-150',
                active
                  ? 'neu-nav-active text-primary'
                  : 'text-muted-foreground hover:text-foreground hover:neu-flat'
              )}
            >
              <Icon className={cn('h-4 w-4 shrink-0', active && 'text-primary')} />
              <span className="truncate">{label}</span>
              {active && (
                <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary animate-pulse-glow" />
              )}
            </Link>
          )
        })}
      </nav>

      {/* Footer */}
      <div className="border-t border-border/60 p-3 space-y-2">
        <SyncStatusBadge />
        {/* Analysis mode badge */}
        <div className="flex items-center gap-2 rounded-xl px-3 py-2 text-xs text-muted-foreground">
          {mode === 'ai' ? <Sparkles className="h-3.5 w-3.5 text-primary" /> : <BarChart3 className="h-3.5 w-3.5" />}
          <span className="flex-1">
            {mode === 'ai' ? 'AI Mode' : mode === 'demo' ? 'Demo Mode' : 'Static Mode'}
          </span>
          {mode === 'ai' && (
            <span className={cn(
              'h-2 w-2 rounded-full',
              checkingAi ? 'bg-amber-400 animate-pulse' : aiAvailable ? 'bg-emerald-500' : 'bg-red-400',
            )} />
          )}
        </div>
        <button
          onClick={toggleDark}
          className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-xs text-muted-foreground hover:text-foreground hover:neu-flat transition-all duration-150"
        >
          {dark ? <Sun className="h-3.5 w-3.5" /> : <Moon className="h-3.5 w-3.5" />}
          {dark ? 'Light mode' : 'Dark mode'}
        </button>
      </div>
    </aside>
  )
}

function AppShell({ dark, toggleDark }: { dark: boolean; toggleDark: () => void }) {
  return (
    <AnalysisModeProvider>
      <div className="flex h-screen bg-background">
        <Sidebar dark={dark} toggleDark={toggleDark} />
        <main className="flex-1 min-h-0 overflow-hidden bg-background">
          <Routes>
            <Route path="/dashboard"  element={<Dashboard />} />
            <Route path="/candidates" element={<CandidatesPage />} />
          </Routes>
        </main>
      </div>
    </AnalysisModeProvider>
  )
}

export default function App() {
  const [dark, setDark] = useState(() => document.documentElement.classList.contains('dark'))

  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark)
  }, [dark])

  const toggleDark = () => setDark((d) => !d)

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<LandingPage dark={dark} toggleDark={toggleDark} />} />
        <Route path="/*" element={<AppShell dark={dark} toggleDark={toggleDark} />} />
      </Routes>
    </BrowserRouter>
  )
}
