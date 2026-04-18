import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { createRoom } from '../api/rooms'
import { useRoomStore } from '../store/roomStore'

export default function CreateRoomPage() {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()
  const { addRoom } = useRoomStore()

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setError('')
    try {
      const room = await createRoom(name, description || undefined)
      addRoom(room)
      navigate(`/rooms/${room.id}`)
    } catch (e: unknown) {
      const err = e as Error
      setError(err.message || 'Failed to create room')
    }
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white flex items-center justify-center">
      <div className="bg-gray-800 p-8 rounded-lg w-full max-w-md">
        <h1 className="text-2xl font-bold mb-6">Create a Room</h1>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <input
            type="text"
            placeholder="Room name (max 100 chars)"
            value={name}
            onChange={(e) => setName(e.target.value)}
            maxLength={100}
            required
            className="bg-gray-700 rounded px-3 py-2 outline-none focus:ring-2 focus:ring-indigo-500"
          />
          <textarea
            placeholder="Description (optional)"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={3}
            className="bg-gray-700 rounded px-3 py-2 outline-none focus:ring-2 focus:ring-indigo-500 resize-none"
          />
          {error && <p className="text-red-400 text-sm">{error}</p>}
          <div className="flex gap-3">
            <button type="button" onClick={() => navigate('/rooms')}
              className="flex-1 bg-gray-700 hover:bg-gray-600 rounded py-2 text-sm transition-colors">
              Cancel
            </button>
            <button type="submit"
              className="flex-1 bg-indigo-600 hover:bg-indigo-700 rounded py-2 font-semibold transition-colors">
              Create
            </button>
          </div>
        </form>
      </div>
    </div>
  )
}
