import { create } from 'zustand'
import type { Friend, FriendRequest } from '../api/friends'
import { getFriends, getIncomingRequests, getOutgoingRequests } from '../api/friends'

interface FriendStore {
  friends: Friend[]
  incoming: FriendRequest[]
  outgoing: FriendRequest[]
  fetchAll: () => Promise<void>
  setFriends: (friends: Friend[]) => void
  setIncoming: (reqs: FriendRequest[]) => void
  setOutgoing: (reqs: FriendRequest[]) => void
  removeIncoming: (id: string) => void
  removeOutgoing: (id: string) => void
}

export const useFriendStore = create<FriendStore>((set) => ({
  friends: [],
  incoming: [],
  outgoing: [],

  fetchAll: async () => {
    const [friends, incoming, outgoing] = await Promise.all([
      getFriends(),
      getIncomingRequests(),
      getOutgoingRequests(),
    ])
    set({ friends, incoming, outgoing })
  },

  setFriends: (friends) => set({ friends }),
  setIncoming: (reqs) => set({ incoming: reqs }),
  setOutgoing: (reqs) => set({ outgoing: reqs }),
  removeIncoming: (id) => set((s) => ({ incoming: s.incoming.filter((r) => r.id !== id) })),
  removeOutgoing: (id) => set((s) => ({ outgoing: s.outgoing.filter((r) => r.id !== id) })),
}))
