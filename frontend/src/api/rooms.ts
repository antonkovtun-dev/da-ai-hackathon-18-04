export interface Room {
  id: string
  name: string
  description: string | null
  ownerId: string
  memberCount: number
  createdAt: string
}

export interface Member {
  userId: string
  username: string
  role: 'OWNER' | 'ADMIN' | 'MEMBER'
  joinedAt: string
}

export interface RoomPage {
  content: Room[]
  totalPages: number
  totalElements: number
  number: number
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

export const getRooms = (search = '', page = 0) =>
  request<RoomPage>(`/api/rooms?search=${encodeURIComponent(search)}&page=${page}&size=20`)

export const getRoom = (id: string) => request<Room>(`/api/rooms/${id}`)

export const createRoom = (name: string, description?: string) =>
  request<Room>('/api/rooms', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, description }),
  })

export const joinRoom = (id: string) =>
  request<void>(`/api/rooms/${id}/join`, { method: 'POST' })

export const leaveRoom = (id: string) =>
  request<void>(`/api/rooms/${id}/leave`, { method: 'POST' })

export const deleteRoom = (id: string) =>
  request<void>(`/api/rooms/${id}`, { method: 'DELETE' })

export const getMembers = (id: string) =>
  request<Member[]>(`/api/rooms/${id}/members`)

export const setMemberRole = (roomId: string, userId: string, role: 'ADMIN' | 'MEMBER') =>
  request<void>(`/api/rooms/${roomId}/members/${userId}/role`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ role }),
  })

export const banUser = (roomId: string, userId: string, reason?: string) =>
  request<void>(`/api/rooms/${roomId}/bans`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, reason }),
  })

export const markRoomRead = (id: string) =>
  request<void>(`/api/rooms/${id}/read`, { method: 'POST' })

export const getUnreadCounts = () =>
  request<Record<string, number>>('/api/rooms/unread')
