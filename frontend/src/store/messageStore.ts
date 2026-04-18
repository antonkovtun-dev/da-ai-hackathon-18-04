import { create } from 'zustand'
import type { Message } from '../api/messages'

interface MessageStore {
  messages: Record<string, Message[]>
  setMessages: (roomId: string, msgs: Message[]) => void
  prependMessages: (roomId: string, msgs: Message[]) => void
  appendMessage: (roomId: string, msg: Message) => void
  updateMessage: (roomId: string, updated: Partial<Message> & { id: string }) => void
  markDeleted: (roomId: string, messageId: string) => void
}

export const useMessageStore = create<MessageStore>((set) => ({
  messages: {},

  setMessages: (roomId, msgs) =>
    set((s) => ({ messages: { ...s.messages, [roomId]: msgs } })),

  prependMessages: (roomId, msgs) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [roomId]: [...(s.messages[roomId] ?? []), ...msgs],
      },
    })),

  appendMessage: (roomId, msg) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [roomId]: [msg, ...(s.messages[roomId] ?? [])],
      },
    })),

  updateMessage: (roomId, updated) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [roomId]: (s.messages[roomId] ?? []).map((m) =>
          m.id === updated.id ? { ...m, ...updated } : m
        ),
      },
    })),

  markDeleted: (roomId, messageId) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [roomId]: (s.messages[roomId] ?? []).map((m) =>
          m.id === messageId ? { ...m, content: null, deleted: true } : m
        ),
      },
    })),
}))
