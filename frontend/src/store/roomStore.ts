import { create } from 'zustand'
import type { Room } from '../api/rooms'
import { getRooms, joinRoom, leaveRoom } from '../api/rooms'

interface RoomStore {
  myRooms: Room[]
  activeRoomId: string | null
  unread: Record<string, number>
  setMyRooms: (rooms: Room[]) => void
  addRoom: (room: Room) => void
  removeRoom: (roomId: string) => void
  setActiveRoom: (roomId: string | null) => void
  incrementUnread: (roomId: string) => void
  clearUnread: (roomId: string) => void
  fetchMyRooms: () => Promise<void>
  join: (roomId: string) => Promise<void>
  leave: (roomId: string) => Promise<void>
}

export const useRoomStore = create<RoomStore>((set, get) => ({
  myRooms: [],
  activeRoomId: null,
  unread: {},

  setMyRooms: (rooms) => set({ myRooms: rooms }),
  addRoom: (room) => set((s) => ({ myRooms: [...s.myRooms, room] })),
  removeRoom: (roomId) => set((s) => ({ myRooms: s.myRooms.filter((r) => r.id !== roomId) })),
  setActiveRoom: (roomId) => set({ activeRoomId: roomId }),
  incrementUnread: (roomId) =>
    set((s) => ({ unread: { ...s.unread, [roomId]: (s.unread[roomId] ?? 0) + 1 } })),
  clearUnread: (roomId) =>
    set((s) => ({ unread: { ...s.unread, [roomId]: 0 } })),

  fetchMyRooms: async () => {
    const page = await getRooms('', 0)
    set({ myRooms: page.content })
  },

  join: async (roomId) => {
    await joinRoom(roomId)
  },

  leave: async (roomId) => {
    await leaveRoom(roomId)
    get().removeRoom(roomId)
  },
}))
