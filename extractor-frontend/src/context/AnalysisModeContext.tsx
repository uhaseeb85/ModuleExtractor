import { createContext, useContext, useState, useEffect, useCallback, type ReactNode } from 'react'
import { getAnalysisMode, setAnalysisMode as persistMode, type AnalysisMode, getApiKey, hasAiConfig } from '../api/ai-config'
import { api } from '../api/client'

interface AnalysisModeContextValue {
  mode: AnalysisMode
  setMode: (m: AnalysisMode) => void
  aiAvailable: boolean
  checkingAi: boolean
}

const Ctx = createContext<AnalysisModeContextValue>({
  mode: 'static',
  setMode: () => {},
  aiAvailable: false,
  checkingAi: false,
})

export function AnalysisModeProvider({ children }: { children: ReactNode }) {
  const [mode, setModeState] = useState<AnalysisMode>(getAnalysisMode)
  const [aiAvailable, setAiAvailable] = useState(false)
  const [checkingAi, setCheckingAi] = useState(false)

  const setMode = useCallback((m: AnalysisMode) => {
    persistMode(m)
    setModeState(m)
  }, [])

  // Auto-check AI health when mode is 'ai' and config exists
  useEffect(() => {
    if (mode !== 'ai' || !hasAiConfig()) {
      setAiAvailable(false)
      return
    }

    let cancelled = false
    setCheckingAi(true)

    api.aiHealthCheck(getApiKey())
      .then((res) => {
        if (!cancelled) setAiAvailable(res.available)
      })
      .catch(() => {
        if (!cancelled) setAiAvailable(false)
      })
      .finally(() => {
        if (!cancelled) setCheckingAi(false)
      })

    return () => { cancelled = true }
  }, [mode])

  return (
    <Ctx.Provider value={{ mode, setMode, aiAvailable, checkingAi }}>
      {children}
    </Ctx.Provider>
  )
}

export function useAnalysisMode() {
  return useContext(Ctx)
}
