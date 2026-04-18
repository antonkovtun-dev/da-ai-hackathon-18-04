import { useEffect, useRef } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useMessageStore } from '../store/messageStore'
import { useRoomStore } from '../store/roomStore'
import { useAuthStore } from '../store/authStore'
import type { Message } from '../api/messages'

interface RoomEvent {
  type: 'MESSAGE_NEW' | 'MESSAGE_EDITED' | 'MESSAGE_DELETED' | 'MEMBER_JOINED' | 'MEMBER_LEFT' | 'MEMBER_KICKED' | 'PRESENCE_UPDATE'
  message?: Message
  messageId?: string
  content?: string
  editedAt?: string
  userId?: string
  username?: string
  status?: string
}

export function useRoomSocket(roomId: string | null) {
  const clientRef = useRef<Client | null>(null)
  const { appendMessage, updateMessage, markDeleted } = useMessageStore()
  const { incrementUnread, removeRoom, setPresence } = useRoomStore()
  const { user } = useAuthStore()

  // Keep activeRoomId current inside the closure without re-triggering the effect
  const activeRoomIdRef = useRef<string | null>(useRoomStore.getState().activeRoomId)
  useEffect(() => {
    return useRoomStore.subscribe((s) => {
      activeRoomIdRef.current = s.activeRoomId
    })
  }, [])

  useEffect(() => {
    if (!roomId || !user) return
    const rid = roomId
    const uid = user.id

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws') as WebSocket,
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe(`/topic/rooms/${rid}`, (frame) => {
          const event: RoomEvent = JSON.parse(frame.body)
          handleEvent(event)
        })
      },
    })

    function handleEvent(event: RoomEvent) {
      switch (event.type) {
        case 'MESSAGE_NEW':
          if (event.message) {
            appendMessage(rid, event.message)
            if (activeRoomIdRef.current !== rid) {
              incrementUnread(rid)
            }
          }
          break
        case 'MESSAGE_EDITED':
          if (event.messageId) {
            updateMessage(rid, {
              id: event.messageId,
              content: event.content ?? null,
              editedAt: event.editedAt ?? null,
            })
          }
          break
        case 'MESSAGE_DELETED':
          if (event.messageId) markDeleted(rid, event.messageId)
          break
        case 'MEMBER_KICKED':
          if (event.userId === uid) {
            removeRoom(rid)
            window.location.href = '/rooms'
          }
          break
        case 'PRESENCE_UPDATE':
          if (event.userId && event.status) {
            setPresence(event.userId, event.status)
          }
          break
      }
    }

    client.activate()
    clientRef.current = client

    return () => {
      client.deactivate()
      clientRef.current = null
    }
  }, [roomId, user])
}
