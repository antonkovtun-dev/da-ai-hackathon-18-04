import { useRef, useEffect, useState } from 'react'
import { useMessageStore } from '../store/messageStore'
import { useAuthStore } from '../store/authStore'
import { getMessages, editMessage, deleteMessage, type Message, type Attachment } from '../api/messages'

interface Props { roomId: string; isAdmin?: boolean }

function AttachmentView({ attachment }: { attachment: Attachment }) {
  const url = `/api/attachments/${attachment.id}`
  if (attachment.contentType.startsWith('image/')) {
    return (
      <a href={url} download={attachment.filename}>
        <img
          src={url}
          alt={attachment.filename}
          className="max-w-xs max-h-64 rounded mt-1 border border-gray-700 cursor-pointer"
        />
      </a>
    )
  }
  return (
    <a
      href={url}
      download={attachment.filename}
      className="flex items-center gap-2 mt-1 text-indigo-400 hover:text-indigo-300 text-sm underline"
    >
      📄 {attachment.filename} ({Math.round(attachment.size / 1024)} KB)
    </a>
  )
}

export default function MessageList({ roomId, isAdmin = false }: Props) {
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
      // Pass only changed fields to avoid wiping the attachment field
      updateMessage(roomId, { id: updated.id, content: updated.content, editedAt: updated.editedAt })
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
                {(msg.authorId === user?.id || isAdmin) && editingId !== msg.id && (
                  <div className="ml-auto hidden group-hover:flex gap-2">
                    {msg.authorId === user?.id && (
                      <button onClick={() => handleEdit(msg)}
                        className="text-gray-400 hover:text-white text-xs">edit</button>
                    )}
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
                <>
                  {msg.content && (
                    <p className="text-gray-100 text-sm mt-0.5 whitespace-pre-wrap break-words">{msg.content}</p>
                  )}
                  {msg.attachment && <AttachmentView attachment={msg.attachment} />}
                </>
              )}
            </>
          )}
        </div>
      ))}
      <div ref={bottomRef} />
    </div>
  )
}
