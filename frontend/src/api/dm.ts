export interface DmThread {
  id: string
  otherUserId: string
  otherUsername: string
  createdAt: string
}

export interface DmMessage {
  id: string
  threadId: string
  authorId: string
  authorUsername: string
  content: string | null
  createdAt: string
  editedAt: string | null
  deleted: boolean
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, { credentials: 'include', ...init })
  if (!res.ok) {
    const text = await res.text().catch(() => '')
    throw Object.assign(new Error(text || res.statusText), { status: res.status })
  }
  const ct = res.headers.get('content-type') ?? ''
  return ct.includes('application/json') ? res.json() : (undefined as T)
}

export const getOrCreateThread = (targetUserId: string) =>
  request<DmThread>('/api/dm/threads', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ targetUserId }),
  })

export const listThreads = () =>
  request<DmThread[]>('/api/dm/threads')

export const getDmMessages = (threadId: string, before?: string, limit = 50) => {
  const params = new URLSearchParams({ limit: String(limit) })
  if (before) params.set('before', before)
  return request<DmMessage[]>(`/api/dm/threads/${threadId}/messages?${params}`)
}

export const sendDmMessage = (threadId: string, content: string) =>
  request<DmMessage>(`/api/dm/threads/${threadId}/messages`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content }),
  })

export const editDmMessage = (id: string, content: string) =>
  request<DmMessage>(`/api/dm/messages/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content }),
  })

export const deleteDmMessage = (id: string) =>
  request<void>(`/api/dm/messages/${id}`, { method: 'DELETE' })
