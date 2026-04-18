import { useEffect, useRef, useState, type KeyboardEvent } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { useDmStore } from '../store/dmStore'
import { useDmSocket } from '../hooks/useDmSocket'
import { getDmMessages, sendDmMessage, editDmMessage, deleteDmMessage, type DmMessage } from '../api/dm'
import { logout } from '../api/auth'

export default function DmChatPage() {
  const { id: threadId } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user, setUser } = useAuthStore()
  const { threads, messages, setMessages, prependMessages, updateMessage, markDeleted } = useDmStore()
  const thread = threads.find((t) => t.id === threadId)
  const roomMessages = threadId ? (messages[threadId] ?? []) : []
  const [content, setContent] = useState('')
  const [sending, setSending] = useState(false)
  const [editingId, setEditingId] = useState<string | null>(null)
  const [editContent, setEditContent] = useState('')
  const [loadingMore, setLoadingMore] = useState(false)
  const [hasMore, setHasMore] = useState(true)
  const bottomRef = useRef<HTMLDivElement>(null)

  useDmSocket(threadId ?? null)

  useEffect(() => {
    if (!threadId) return
    getDmMessages(threadId).then((msgs) => setMessages(threadId, msgs))
  }, [threadId])

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [roomMessages.length === 0])

  async function handleSend() {
    const text = content.trim()
    if (!text || sending || !threadId) return
    setSending(true)
    try {
      await sendDmMessage(threadId, text)
      setContent('')
    } catch (e: unknown) {
      const err = e as Error
      alert(err.message || 'Failed to send')
    } finally {
      setSending(false)
    }
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); handleSend() }
  }

  async function loadMore() {
    if (loadingMore || !hasMore || roomMessages.length === 0 || !threadId) return
    setLoadingMore(true)
    const oldest = roomMessages[roomMessages.length - 1]
    try {
      const older = await getDmMessages(threadId, oldest.id, 50)
      if (older.length === 0) setHasMore(false)
      else prependMessages(threadId, older)
    } finally {
      setLoadingMore(false)
    }
  }

  function startEdit(msg: DmMessage) {
    setEditingId(msg.id)
    setEditContent(msg.content ?? '')
  }

  async function submitEdit(id: string) {
    if (!threadId) return
    try {
      const updated = await editDmMessage(id, editContent)
      updateMessage(threadId, updated)
    } catch { /* ignore */ }
    setEditingId(null)
  }

  async function handleDelete(id: string) {
    if (!threadId || !confirm('Delete this message?')) return
    try {
      await deleteDmMessage(id)
      markDeleted(threadId, id)
    } catch { /* ignore */ }
  }

  async function handleLogout() {
    await logout()
    setUser(null)
    navigate('/login')
  }

  return (
    <div className="flex flex-col h-screen bg-gray-900 text-white">
      <header className="flex items-center justify-between px-4 py-3 bg-gray-800 border-b border-gray-700 flex-shrink-0">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('/friends')}
            className="text-gray-400 hover:text-white text-sm transition-colors">
            Friends
          </button>
          {thread && <span className="text-gray-500">›</span>}
          {thread && <span className="font-semibold">@{thread.otherUsername}</span>}
        </div>
        <div className="flex items-center gap-3">
          <span className="text-gray-400 text-sm">@{user?.username}</span>
          <button onClick={handleLogout}
            className="text-sm bg-gray-700 hover:bg-gray-600 px-3 py-1 rounded transition-colors">
            Logout
          </button>
        </div>
      </header>

      {/* Messages */}
      <div
        className="flex-1 overflow-y-auto p-4 space-y-2"
        onScroll={(e) => { if (e.currentTarget.scrollTop < 100) loadMore() }}
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
                      <button onClick={() => startEdit(msg)}
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

      {/* Composer */}
      <div className="flex-shrink-0 p-3 border-t border-gray-700 bg-gray-900">
        <div className="flex gap-2 items-end">
          <textarea
            value={content}
            onChange={(e) => setContent(e.target.value)}
            onKeyDown={onKeyDown}
            placeholder="Type a message…"
            maxLength={3000}
            rows={1}
            className="flex-1 bg-gray-800 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
            style={{ minHeight: '2.5rem', maxHeight: '8rem' }}
          />
          <button
            onClick={handleSend}
            disabled={!content.trim() || sending}
            className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 px-4 py-2 rounded-lg text-sm font-medium transition-colors"
          >
            Send
          </button>
        </div>
      </div>
    </div>
  )
}
