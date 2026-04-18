async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, { credentials: 'include', ...init })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw Object.assign(new Error(text || res.statusText), { status: res.status })
  }
  const ct = res.headers.get('content-type') ?? ''
  return ct.includes('application/json') ? res.json() : (undefined as T)
}

export const sendHeartbeat = (tabId: string, active: boolean) =>
  request<void>('/api/presence/heartbeat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ tabId, active }),
  })
