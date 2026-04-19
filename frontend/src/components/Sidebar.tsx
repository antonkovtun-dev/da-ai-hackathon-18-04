import { useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useRoomStore } from '../store/roomStore'
import { useDmStore } from '../store/dmStore'
import { listThreads } from '../api/dm'
import { getUnreadCounts } from '../api/rooms'

export default function Sidebar() {
  const { myRooms, unread, setUnread, presence } = useRoomStore()
  const { threads, setThreads } = useDmStore()
  const navigate = useNavigate()
  const { pathname } = useLocation()

  const activeRoomId = pathname.startsWith('/rooms/') ? pathname.split('/')[2] : undefined
  const activeDmId = pathname.startsWith('/dm/') ? pathname.split('/')[2] : undefined

  useEffect(() => {
    listThreads().then(setThreads).catch(() => {})
    getUnreadCounts().then(setUnread).catch(() => {})
  }, [])

  function dot(userId?: string) {
    if (!userId) return null
    const s = presence[userId] ?? 'OFFLINE'
    const cls = s === 'ONLINE' ? 'bg-green-400' : s === 'AFK' ? 'bg-yellow-400' : 'bg-gray-600'
    return <span className={`w-2 h-2 rounded-full flex-shrink-0 ${cls}`} />
  }

  return (
    <aside className="w-56 flex-shrink-0 bg-gray-800 border-r border-gray-700 flex flex-col">
      {/* Rooms */}
      <div className="p-4 border-b border-gray-700">
        <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">My Rooms</p>
      </div>
      <nav className="flex-1 overflow-y-auto py-2">
        {myRooms.length === 0 && (
          <p className="px-4 py-2 text-gray-500 text-sm">No rooms yet</p>
        )}
        {myRooms.map((room) => (
          <button
            key={room.id}
            onClick={() => navigate(`/rooms/${room.id}`)}
            className={`w-full text-left px-4 py-2 text-sm flex items-center justify-between hover:bg-gray-700 transition-colors ${
              room.id === activeRoomId ? 'bg-gray-700 text-white' : 'text-gray-300'
            }`}
          >
            <span className="truncate"># {room.name}</span>
            {(unread[room.id] ?? 0) > 0 && (
              <span className="ml-2 bg-indigo-600 text-white text-xs rounded-full px-1.5 py-0.5 min-w-[1.25rem] text-center">
                {unread[room.id]}
              </span>
            )}
          </button>
        ))}
      </nav>

      {/* DMs */}
      {threads.length > 0 && (
        <>
          <div className="px-4 py-2 border-t border-gray-700">
            <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Direct Messages</p>
          </div>
          <nav className="overflow-y-auto py-1">
            {threads.map((t) => (
              <button
                key={t.id}
                onClick={() => navigate(`/dm/${t.id}`)}
                className={`w-full text-left px-4 py-2 text-sm hover:bg-gray-700 transition-colors ${
                  t.id === activeDmId ? 'bg-gray-700 text-white' : 'text-gray-300'
                }`}
              >
                <span className="flex items-center gap-1.5 truncate">
                  {dot(t.otherUserId)}
                  @ {t.otherUsername}
                </span>
              </button>
            ))}
          </nav>
        </>
      )}

      <div className="p-3 border-t border-gray-700 space-y-1">
        <button
          onClick={() => navigate('/rooms')}
          className="w-full text-left text-gray-400 hover:text-white text-sm px-2 py-1 rounded hover:bg-gray-700 transition-colors"
        >
          + Browse Rooms
        </button>
        <button
          onClick={() => navigate('/friends')}
          className="w-full text-left text-gray-400 hover:text-white text-sm px-2 py-1 rounded hover:bg-gray-700 transition-colors"
        >
          Friends
        </button>
        <button
          onClick={() => navigate('/settings')}
          className="w-full text-left text-gray-400 hover:text-white text-sm px-2 py-1 rounded hover:bg-gray-700 transition-colors"
        >
          Settings
        </button>
      </div>
    </aside>
  )
}
