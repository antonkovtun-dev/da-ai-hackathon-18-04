import { useState, useRef, type KeyboardEvent, type ClipboardEvent } from 'react'
import { sendMessage } from '../api/messages'
import { uploadAttachment } from '../api/attachments'

interface Props { roomId: string }

export default function MessageComposer({ roomId }: Props) {
  const [content, setContent] = useState('')
  const [sending, setSending] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

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

  async function sendFile(file: File) {
    if (sending) return
    setSending(true)
    try {
      await uploadAttachment(roomId, file, content.trim() || undefined)
      setContent('')
    } catch (e: unknown) {
      const err = e as { message?: string; status?: number }
      if (err.status === 400) {
        alert('File too large or invalid (images ≤ 3 MB, files ≤ 20 MB)')
      } else {
        alert(err.message || 'Upload failed')
      }
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

  function onPaste(e: ClipboardEvent<HTMLTextAreaElement>) {
    const file = Array.from(e.clipboardData.items)
      .find(item => item.kind === 'file')
      ?.getAsFile()
    if (file) {
      e.preventDefault()
      sendFile(file)
    }
  }

  function onFileChange(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0]
    if (file) sendFile(file)
    e.target.value = ''
  }

  return (
    <div className="flex-shrink-0 p-3 border-t border-gray-700 bg-gray-900">
      <input
        ref={fileInputRef}
        type="file"
        className="hidden"
        onChange={onFileChange}
      />
      <div className="flex gap-2 items-end">
        <button
          onClick={() => fileInputRef.current?.click()}
          disabled={sending}
          title="Attach file"
          className="flex-shrink-0 bg-gray-700 hover:bg-gray-600 disabled:opacity-50 p-2 rounded-lg transition-colors"
        >
          📎
        </button>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onKeyDown={onKeyDown}
          onPaste={onPaste}
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
