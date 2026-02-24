import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import GraphPage from './pages/GraphPage'
import ReposPage from './pages/ReposPage'
import CandidatesPage from './pages/CandidatesPage'
import SyncStatusBadge from './components/SyncStatusBadge'

export default function App() {
  return (
    <BrowserRouter>
      <div className="flex h-screen flex-col">
        {/* ── Nav bar ────────────────────────────────────────────── */}
        <header className="flex items-center gap-6 border-b border-gray-800 bg-gray-900 px-6 py-3">
          <span className="text-lg font-bold tracking-tight">
            Module Extractor
          </span>
          <nav className="flex gap-4 text-sm">
            <Link to="/dashboard" className="hover:text-indigo-400">
              Dashboard
            </Link>
            <Link to="/graph" className="hover:text-indigo-400">
              Graph
            </Link>
            <Link to="/repos" className="hover:text-indigo-400">
              Repos
            </Link>
            <Link to="/candidates" className="hover:text-indigo-400">
              Candidates
            </Link>
          </nav>
          <div className="ml-auto">
            <SyncStatusBadge />
          </div>
        </header>

        {/* ── Main content ───────────────────────────────────────── */}
        <main className="flex-1 overflow-auto">
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
