/** Helpers for storing/retrieving AI configuration in localStorage. */

const KEYS = {
  apiKey:   'me_openrouter_key',
  modelId:  'me_openrouter_model',
} as const

export function getApiKey(): string {
  return localStorage.getItem(KEYS.apiKey) ?? ''
}

export function setApiKey(key: string): void {
  if (key) localStorage.setItem(KEYS.apiKey, key)
  else localStorage.removeItem(KEYS.apiKey)
}

export function getModelId(): string {
  return localStorage.getItem(KEYS.modelId) ?? ''
}

export function setModelId(id: string): void {
  if (id) localStorage.setItem(KEYS.modelId, id)
  else localStorage.removeItem(KEYS.modelId)
}

export function hasAiConfig(): boolean {
  return !!getApiKey() && !!getModelId()
}
