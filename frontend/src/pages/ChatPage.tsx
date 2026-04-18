import { useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { useRoomStore } from '../store/roomStore'
import { useMessageStore } from '../store/messageStore'
import { getMessages } from '../api/messages'
import { getRoom, markRoomRead } from '../api/rooms'
import { logout } from '../api/auth'
import { useRoomSocket } from '../hooks/useRoomSocket'
import Sidebar from '../components/Sidebar'
import MessageList from '../components/MessageList'
import MessageComposer from '../components/MessageComposer'

export default function ChatPage() {
  const { id: roomId } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user, setUser } = useAuthStore()
  const { myRooms, setActiveRoom, clearUnread, fetchMyRooms } = useRoomStore()
  const { setMessages } = useMessageStore()

  const room = myRooms.find((r) => r.id === roomId)

  useRoomSocket(roomId ?? null)

  useEffect(() => {
    if (!roomId) return
    setActiveRoom(roomId)
    clearUnread(roomId)
    markRoomRead(roomId).catch(() => {})

    getRoom(roomId).catch(() => navigate('/rooms'))
    getMessages(roomId).then((msgs) => setMessages(roomId, msgs))
    fetchMyRooms()

    return () => setActiveRoom(null)
  }, [roomId])

  async function handleLogout() {
    await logout()
    setUser(null)
    navigate('/login')
  }

  return (
    <div className="flex flex-col h-screen bg-gray-900 text-white">
      <header className="flex items-center justify-between px-4 py-3 bg-gray-800 border-b border-gray-700 flex-shrink-0">
        <div className="flex items-center gap-3">
          <button onClick={() => navigate('/rooms')}
            className="text-gray-400 hover:text-white text-sm transition-colors">
            Browse Rooms
          </button>
          {room && <span className="text-gray-500">›</span>}
          {room && <span className="font-semibold">#{room.name}</span>}
        </div>
        <div className="flex items-center gap-3">
          <span className="text-gray-400 text-sm">@{user?.username}</span>
          <button onClick={handleLogout}
            className="text-sm bg-gray-700 hover:bg-gray-600 px-3 py-1 rounded transition-colors">
            Logout
          </button>
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        <Sidebar activeRoomId={roomId} />
        <main className="flex-1 flex flex-col overflow-hidden">
          {roomId ? (
            <>
              <MessageList roomId={roomId} />
              <MessageComposer roomId={roomId} />
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-gray-400">
              Select a room to start chatting
            </div>
          )}
        </main>
      </div>
    </div>
  )
}
