// frontend/src/api/auth.ts
export interface User {
  id: string
  email: string
  username: string
  createdAt: string
}

export interface ValidationError {
  field: string
  message: string
}

export async function register(email: string, username: string, password: string): Promise<User> {
  const res = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, username, password }),
    credentials: 'include',
  })
  if (!res.ok) {
    const errors: ValidationError[] = await res.json()
    throw errors
  }
  return res.json()
}

export async function login(email: string, password: string): Promise<User> {
  const res = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
    credentials: 'include',
  })
  if (!res.ok) throw new Error('Invalid credentials')
  return res.json()
}

export async function logout(): Promise<void> {
  await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' })
}

export async function getMe(): Promise<User | null> {
  const res = await fetch('/api/auth/me', { credentials: 'include' })
  if (res.status === 401) return null
  return res.json()
}

export async function changePassword(currentPassword: string, newPassword: string): Promise<void> {
  const res = await fetch('/api/users/me/password', {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ currentPassword, newPassword }),
    credentials: 'include',
  })
  if (!res.ok) throw new Error((await res.json().catch(() => ({}))).message || 'Failed')
}

export async function forgotPassword(email: string): Promise<void> {
  await fetch('/api/auth/forgot-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
    credentials: 'include',
  })
}

export async function resetPassword(token: string, newPassword: string): Promise<void> {
  const res = await fetch('/api/auth/reset-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, newPassword }),
    credentials: 'include',
  })
  if (!res.ok) throw new Error('Invalid or expired token')
}

export async function deleteAccount(): Promise<void> {
  const res = await fetch('/api/users/me', { method: 'DELETE', credentials: 'include' })
  if (!res.ok) throw new Error('Failed to delete account')
}
