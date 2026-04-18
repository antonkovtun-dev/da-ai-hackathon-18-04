import { useState, type KeyboardEvent } from 'react'
import { sendMessage } from '../api/messages'

interface Props { roomId: string }

export default function MessageComposer({ roomId }: Props) {
  const [content, setContent] = useState('')
  const [sending, setSending] = useState(false)

  async function send() {
    const text = content.trim()
    if (!text || sending) return
    setSending(true)
    try {
      await sendMessage(roomId, text)
      setContent('')
    } catch (e: unknown) {
      const err = e as Error
      alert(err.message || 'Failed to send message')
    } finally {
      setSending(false)
    }
  }

  function onKeyDown(e: KeyboardEvent<HTMLTextAreaElement>) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send()
    }
  }

  return (
    <div className="flex-shrink-0 p-3 border-t border-gray-700 bg-gray-900">
      <div className="flex gap-2 items-end">
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder="Type a message… (Enter to send, Shift+Enter for newline)"
          maxLength={3000}
          rows={1}
          className="flex-1 bg-gray-800 rounded-lg px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
          style={{ minHeight: '2.5rem', maxHeight: '8rem' }}
        />
        <button
          onClick={send}
          disabled={!content.trim() || sending}
          className="bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 px-4 py-2 rounded-lg text-sm font-medium transition-colors"
        >
          Send
        </button>
      </div>
    </div>
  )
}
