export interface Message {
  id: string
  roomId: string
  authorId: string
  authorUsername: string
  content: string | null
  createdAt: string
  editedAt: string | null
  deleted: boolean
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(path, { credentials: 'include', ...init })
  if (!res.ok) throw Object.assign(new Error(res.statusText), { status: res.status })
  const ct = res.headers.get('content-type') ?? ''
  return ct.includes('application/json') ? res.json() : (undefined as T)
}

export const getMessages = (roomId: string, before?: string, limit = 50) => {
  const params = new URLSearchParams({ limit: String(limit) })
  if (before) params.set('before', before)
  return request<Message[]>(`/api/rooms/${roomId}/messages?${params}`)
}

export const sendMessage = (roomId: string, content: string) =>
  request<Message>(`/api/rooms/${roomId}/messages`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content }),
  })

export const editMessage = (id: string, content: string) =>
  request<Message>(`/api/messages/${id}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ content }),
  })

export const deleteMessage = (id: string) =>
  request<void>(`/api/messages/${id}`, { method: 'DELETE' })
