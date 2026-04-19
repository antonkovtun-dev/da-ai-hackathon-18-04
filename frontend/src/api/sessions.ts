export interface SessionInfo {
  sessionId: string
  createdAt: string
  lastActiveAt: string
  userAgent: string | null
  ipAddress: string | null
  current: boolean
}

export async function getSessions(): Promise<SessionInfo[]> {
  const res = await fetch('/api/sessions', { credentials: 'include' })
  if (!res.ok) throw new Error('Failed')
  return res.json()
}

export async function revokeSession(sessionId: string): Promise<void> {
  const res = await fetch(`/api/sessions/${sessionId}`, {
    method: 'DELETE',
    credentials: 'include',
  })
  if (!res.ok) throw new Error('Failed')
}
