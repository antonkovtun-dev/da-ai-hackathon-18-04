import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import type { Room } from '../api/rooms'
import { getRooms, joinRoom } from '../api/rooms'
import { useRoomStore } from '../store/roomStore'

export default function RoomCatalogPage() {
  const [rooms, setRooms] = useState<Room[]>([])
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const navigate = useNavigate()
  const { addRoom } = useRoomStore()

  useEffect(() => {
    setLoading(true)
    getRooms(search, page)
      .then((data) => {
        setRooms(data.content)
        setTotalPages(data.totalPages)
      })
      .finally(() => setLoading(false))
  }, [search, page])

  async function handleJoin(room: Room) {
    try {
      await joinRoom(room.id)
      addRoom(room)
      navigate(`/rooms/${room.id}`)
    } catch (e: unknown) {
      const err = e as { status?: number }
      if (err.status === 409) navigate(`/rooms/${room.id}`)
      else alert('Could not join room')
    }
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <div className="max-w-3xl mx-auto p-6">
        <div className="flex items-center justify-between mb-6">
          <h1 className="text-2xl font-bold">Browse Rooms</h1>
          <button
            onClick={() => navigate('/rooms/new')}
            className="bg-indigo-600 hover:bg-indigo-700 px-4 py-2 rounded text-sm font-medium transition-colors"
          >
            + Create Room
          </button>
        </div>

        <input
          type="text"
          placeholder="Search rooms..."
          value={search}
          onChange={(e) => { setSearch(e.target.value); setPage(0) }}
          className="w-full bg-gray-800 rounded px-4 py-2 mb-4 outline-none focus:ring-2 focus:ring-indigo-500"
        />

        {loading ? (
          <p className="text-gray-400">Loading...</p>
        ) : rooms.length === 0 ? (
          <p className="text-gray-400">No rooms found.</p>
        ) : (
          <div className="space-y-2">
            {rooms.map((room) => (
              <div
                key={room.id}
                className="bg-gray-800 rounded-lg p-4 flex items-center justify-between"
              >
                <div>
                  <p className="font-medium">#{room.name}</p>
                  {room.description && (
                    <p className="text-gray-400 text-sm mt-1">{room.description}</p>
                  )}
                  <p className="text-gray-500 text-xs mt-1">{room.memberCount} members</p>
                </div>
                <button
                  onClick={() => handleJoin(room)}
                  className="bg-indigo-600 hover:bg-indigo-700 px-3 py-1 rounded text-sm transition-colors"
                >
                  Join
                </button>
              </div>
            ))}
          </div>
        )}

        {totalPages > 1 && (
          <div className="flex gap-2 mt-4 justify-center">
            <button disabled={page === 0} onClick={() => setPage(p => p - 1)}
              className="px-3 py-1 rounded bg-gray-700 disabled:opacity-40">Prev</button>
            <span className="px-3 py-1 text-gray-400">{page + 1} / {totalPages}</span>
            <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}
              className="px-3 py-1 rounded bg-gray-700 disabled:opacity-40">Next</button>
          </div>
        )}
      </div>
    </div>
  )
}
