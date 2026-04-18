import { useRef, useEffect, useState } from 'react'
import { useMessageStore } from '../store/messageStore'
import { useAuthStore } from '../store/authStore'
import { getMessages, editMessage, deleteMessage, type Message } from '../api/messages'

interface Props { roomId: string }

export default function MessageList({ roomId }: Props) {
  const { messages, prependMessages, updateMessage, markDeleted } = useMessageStore()
  const { user } = useAuthStore()
  const roomMessages = messages[roomId] ?? []
  const bottomRef = useRef<HTMLDivElement>(null)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editContent, setEditContent] = useState('')
  const [loadingMore, setLoadingMore] = useState(false)
  const [hasMore, setHasMore] = useState(true)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [roomMessages.length === 0])

  async function loadMore() {
    if (loadingMore || !hasMore || roomMessages.length === 0) return
    setLoadingMore(true)
    const oldest = roomMessages[roomMessages.length - 1]
    try {
      const older = await getMessages(roomId, oldest.id, 50)
      if (older.length === 0) setHasMore(false)
      else prependMessages(roomId, older)
    } finally {
      setLoadingMore(false)
    }
  }

  function handleEdit(msg: Message) {
    setEditingId(msg.id)
    setEditContent(msg.content ?? '')
  }

  async function submitEdit(id: string) {
    try {
      const updated = await editMessage(id, editContent)
      updateMessage(roomId, updated)
    } catch { /* ignore */ }
    setEditingId(null)
  }

  async function handleDelete(id: string) {
    if (!confirm('Delete this message?')) return
    try {
      await deleteMessage(id)
      markDeleted(roomId, id)
    } catch { /* ignore */ }
  }

  return (
    <div
      className="flex-1 overflow-y-auto p-4 space-y-2"
      onScroll={(e) => {
        if (e.currentTarget.scrollTop < 100) loadMore()
      }}
    >
      {loadingMore && <p className="text-center text-gray-500 text-xs">Loading...</p>}
      {[...roomMessages].reverse().map((msg) => (
        <div key={msg.id} className="group flex flex-col">
          {msg.deleted ? (
            <span className="text-gray-500 text-sm italic">[message deleted]</span>
          ) : (
            <>
              <div className="flex items-baseline gap-2">
                <span className="font-semibold text-indigo-400 text-sm">{msg.authorUsername}</span>
                <span className="text-gray-500 text-xs">
                  {new Date(msg.createdAt).toLocaleTimeString()}
                  {msg.editedAt && ' (edited)'}
                </span>
                {msg.authorId === user?.id && editingId !== msg.id && (
                  <div className="ml-auto hidden group-hover:flex gap-2">
                    <button onClick={() => handleEdit(msg)}
                      className="text-gray-400 hover:text-white text-xs">edit</button>
                    <button onClick={() => handleDelete(msg.id)}
                      className="text-gray-400 hover:text-red-400 text-xs">delete</button>
                  </div>
                )}
              </div>
              {editingId === msg.id ? (
                <div className="flex gap-2 mt-1">
                  <input
                    autoFocus
                    value={editContent}
                    onChange={(e) => setEditContent(e.target.value)}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); submitEdit(msg.id) }
                      if (e.key === 'Escape') setEditingId(null)
                    }}
                    className="flex-1 bg-gray-700 rounded px-2 py-1 text-sm outline-none focus:ring-1 focus:ring-indigo-500"
                  />
                  <button onClick={() => submitEdit(msg.id)}
                    className="text-xs bg-indigo-600 px-2 rounded">Save</button>
                  <button onClick={() => setEditingId(null)}
                    className="text-xs text-gray-400">Cancel</button>
                </div>
              ) : (
                <p className="text-gray-100 text-sm mt-0.5 whitespace-pre-wrap break-words">{msg.content}</p>
              )}
            </>
          )}
        </div>
      ))}
      <div ref={bottomRef} />
    </div>
  )
}
