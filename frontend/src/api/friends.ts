export type FriendRequestStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELED'

export interface FriendRequest {
  id: string
  senderId: string
  senderUsername: string
  receiverId: string
  receiverUsername: string
  message: string | null
  status: FriendRequestStatus
  createdAt: string
  updatedAt: string
}

export interface Friend {
  userId: string
  username: string
  friendsSince: string
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

export const sendFriendRequest = (targetUsername: string, message?: string) =>
  request<FriendRequest>('/api/friends/requests', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ targetUsername, message }),
  })

export const getIncomingRequests = () =>
  request<FriendRequest[]>('/api/friends/requests/incoming')

export const getOutgoingRequests = () =>
  request<FriendRequest[]>('/api/friends/requests/outgoing')

export const acceptRequest = (id: string) =>
  request<void>(`/api/friends/requests/${id}/accept`, { method: 'POST' })

export const declineRequest = (id: string) =>
  request<void>(`/api/friends/requests/${id}/decline`, { method: 'POST' })

export const cancelRequest = (id: string) =>
  request<void>(`/api/friends/requests/${id}`, { method: 'DELETE' })

export const getFriends = () =>
  request<Friend[]>('/api/friends')

export const removeFriend = (userId: string) =>
  request<void>(`/api/friends/${userId}`, { method: 'DELETE' })

export const blockUser = (targetUserId: string) =>
  request<void>('/api/blocks', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ targetUserId }),
  })

export const unblockUser = (userId: string) =>
  request<void>(`/api/blocks/${userId}`, { method: 'DELETE' })
