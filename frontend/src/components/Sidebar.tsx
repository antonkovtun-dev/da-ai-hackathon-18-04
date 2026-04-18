import { useNavigate } from 'react-router-dom'
import { useRoomStore } from '../store/roomStore'

interface Props {
  activeRoomId?: string
}

export default function Sidebar({ activeRoomId }: Props) {
  const { myRooms, unread } = useRoomStore()
  const navigate = useNavigate()

  return (
    <aside className="w-56 flex-shrink-0 bg-gray-800 border-r border-gray-700 flex flex-col">
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
      <div className="p-3 border-t border-gray-700">
        <button
          onClick={() => navigate('/rooms')}
          className="w-full text-left text-gray-400 hover:text-white text-sm px-2 py-1 rounded hover:bg-gray-700 transition-colors"
        >
          + Browse Rooms
        </button>
      </div>
    </aside>
  )
}
