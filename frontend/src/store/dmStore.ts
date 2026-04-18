import { create } from 'zustand'
import type { DmThread, DmMessage } from '../api/dm'

interface DmStore {
  threads: DmThread[]
  messages: Record<string, DmMessage[]>   // threadId -> messages (newest first)
  setThreads: (threads: DmThread[]) => void
  addThread: (thread: DmThread) => void
  setMessages: (threadId: string, msgs: DmMessage[]) => void
  prependMessages: (threadId: string, msgs: DmMessage[]) => void
  appendMessage: (threadId: string, msg: DmMessage) => void
  updateMessage: (threadId: string, updated: Partial<DmMessage> & { id: string }) => void
  markDeleted: (threadId: string, messageId: string) => void
}

export const useDmStore = create<DmStore>((set) => ({
  threads: [],
  messages: {},

  setThreads: (threads) => set({ threads }),
  addThread: (thread) =>
    set((s) => ({
      threads: s.threads.find((t) => t.id === thread.id) ? s.threads : [thread, ...s.threads],
    })),

  setMessages: (threadId, msgs) =>
    set((s) => ({ messages: { ...s.messages, [threadId]: msgs } })),

  prependMessages: (threadId, msgs) =>
    set((s) => ({
      messages: { ...s.messages, [threadId]: [...(s.messages[threadId] ?? []), ...msgs] },
    })),

  appendMessage: (threadId, msg) =>
    set((s) => ({
      messages: { ...s.messages, [threadId]: [msg, ...(s.messages[threadId] ?? [])] },
    })),

  updateMessage: (threadId, updated) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [threadId]: (s.messages[threadId] ?? []).map((m) =>
          m.id === updated.id ? { ...m, ...updated } : m
        ),
      },
    })),

  markDeleted: (threadId, messageId) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [threadId]: (s.messages[threadId] ?? []).map((m) =>
          m.id === messageId ? { ...m, content: null, deleted: true } : m
        ),
      },
    })),
}))
