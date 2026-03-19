import { BrowserRouter, Link, Navigate, Route, Routes, useLocation } from 'react-router-dom'
import { useState, useEffect } from 'react'
import { LayoutDashboard, GitFork, FolderGit2, Puzzle, Sun, Moon, Boxes } from 'lucide-react'
import { cn } from '@/lib/utils'
import Dashboard from './pages/Dashboard'
import GraphPage from './pages/GraphPage'
import ReposPage from './pages/ReposPage'
import CandidatesPage from './pages/CandidatesPage'
import SyncStatusBadge from './components/SyncStatusBadge'

const NAV_ITEMS = [
  { to: '/dashboard',  label: 'Dashboard',              icon: LayoutDashboard },
  { to: '/graph',      label: 'Dependency Graph',       icon: GitFork         },
  { to: '/repos',      label: 'Repositories',           icon: FolderGit2      },
  { to: '/candidates', label: 'Module Recommendations', icon: Puzzle          },
]

function Sidebar({ dark, toggleDark }: { dark: boolean; toggleDark: () => void }) {
  const location = useLocation()
  return (
    <aside className="flex h-screen w-56 shrink-0 flex-col bg-slate-900 text-slate-200 border-r border-slate-800">
      <div className="flex h-14 items-center gap-2.5 border-b border-slate-800 px-4">
        <div className="flex h-7 w-7 items-center justify-center rounded-lg bg-indigo-500 shadow-sm">
          <Boxes className="h-4 w-4 text-white" />
        </div>
        <div>
          <p className="text-sm font-semibold leading-none">ModuleExtractor</p>
          <p className="mt-0.5 text-[10px] text-slate-500 leading-none">Dependency Analyser</p>
        </div>
      </div>

      <nav className="flex-1 space-y-0.5 overflow-y-auto p-2 pt-3">
        {NAV_ITEMS.map(({ to, label, icon: Icon }) => {
          const active = location.pathname === to || (location.pathname === '/' && to === '/dashboard')
          return (
            <Link
              key={to}
              to={to}
              className={cn(
                'flex items-center gap-2.5 rounded-md px-3 py-2 text-sm transition-colors',
                active
                  ? 'bg-indigo-600 text-white font-medium shadow-sm'
                  : 'text-slate-400 hover:bg-slate-800 hover:text-slate-100'
              )}
            >
              <Icon className="h-4 w-4 shrink-0" />
              <span className="truncate">{label}</span>
            </Link>
          )
        })}
      </nav>

      <div className="border-t border-slate-800 p-2 space-y-1">
        <SyncStatusBadge />
        <button
          onClick={toggleDark}
          className="flex w-full items-center gap-2 rounded-md px-3 py-2 text-xs text-slate-500 hover:bg-slate-800 hover:text-slate-200 transition-colors"
        >
          {dark ? <Sun className="h-3.5 w-3.5" /> : <Moon className="h-3.5 w-3.5" />}
          {dark ? 'Light mode' : 'Dark mode'}
        </button>
      </div>
    </aside>
  )
}

export default function App() {
  const [dark, setDark] = useState(() => document.documentElement.classList.contains('dark'))

  useEffect(() => {
    document.documentElement.classList.toggle('dark', dark)
  }, [dark])

  return (
    <BrowserRouter>
      <div className="flex h-screen bg-background">
        <Sidebar dark={dark} toggleDark={() => setDark((d) => !d)} />
        <main className="flex-1 min-h-0 overflow-hidden">
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/graph" element={<GraphPage />} />
            <Route path="/repos" element={<ReposPage />} />
            <Route path="/candidates" element={<CandidatesPage />} />
          </Routes>
        </main>
      </div>
    </BrowserRouter>
  )
}
