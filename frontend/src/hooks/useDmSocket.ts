import { useEffect } from 'react'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useDmStore } from '../store/dmStore'
import { useAuthStore } from '../store/authStore'
import type { DmMessage } from '../api/dm'

interface DmEvent {
  type: 'DM_MESSAGE_NEW' | 'DM_MESSAGE_EDITED' | 'DM_MESSAGE_DELETED'
  message?: DmMessage
  messageId?: string
  content?: string
  editedAt?: string
}

export function useDmSocket(threadId: string | null) {
  const { appendMessage, updateMessage, markDeleted } = useDmStore()
  const { user } = useAuthStore()

  useEffect(() => {
    if (!threadId || !user) return
    const tid = threadId

    const client = new Client({
      webSocketFactory: () => new SockJS('/ws') as WebSocket,
      reconnectDelay: 3000,
      onConnect: () => {
        client.subscribe(`/topic/dm/${tid}`, (frame) => {
          const event: DmEvent = JSON.parse(frame.body)
          switch (event.type) {
            case 'DM_MESSAGE_NEW':
              if (event.message) appendMessage(tid, event.message)
              break
            case 'DM_MESSAGE_EDITED':
              if (event.messageId) {
                updateMessage(tid, {
                  id: event.messageId,
                  content: event.content ?? null,
                  editedAt: event.editedAt ?? null,
                })
              }
              break
            case 'DM_MESSAGE_DELETED':
              if (event.messageId) markDeleted(tid, event.messageId)
              break
          }
        })
      },
    })

    client.activate()
    return () => { client.deactivate() }
  }, [threadId, user])
}
