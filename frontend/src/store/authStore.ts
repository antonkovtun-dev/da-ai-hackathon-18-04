// frontend/src/store/authStore.ts
import { create } from 'zustand'
import type { User } from '../api/auth'
import { getMe } from '../api/auth'

interface AuthState {
  user: User | null
  loading: boolean
  setUser: (user: User | null) => void
  init: () => Promise<void>
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  loading: true,
  setUser: (user) => set({ user }),
  init: async () => {
    const user = await getMe()
    set({ user, loading: false })
  },
}))
