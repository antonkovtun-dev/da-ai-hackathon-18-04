import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { useRoomStore } from '../store/roomStore'
import { useMessageStore } from '../store/messageStore'
import { getMessages } from '../api/messages'
import { getRoom, markRoomRead, deleteRoom, getMembers } from '../api/rooms'
import { logout } from '../api/auth'
import { useRoomSocket } from '../hooks/useRoomSocket'
import { usePresenceHeartbeat } from '../hooks/usePresenceHeartbeat'
import Sidebar from '../components/Sidebar'
import MessageList from '../components/MessageList'
import MessageComposer from '../components/MessageComposer'
import RoomMembersPanel from '../components/RoomMembersPanel'
import BanListModal from '../components/BanListModal'

export default function ChatPage() {
  const { id: roomId } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const { user, setUser } = useAuthStore()
  const { myRooms, setActiveRoom, clearUnread, fetchMyRooms, removeRoom } = useRoomStore()
  const { setMessages } = useMessageStore()
  const [banListOpen, setBanListOpen] = useState(false)
  const [isAdmin, setIsAdmin] = useState(false)

  const room = myRooms.find((r) => r.id === roomId)

  useRoomSocket(roomId ?? null)
  usePresenceHeartbeat()

  useEffect(() => {
    if (!roomId) return
    setIsAdmin(false)
    setActiveRoom(roomId)
    clearUnread(roomId)
    markRoomRead(roomId).catch(() => {})

    getRoom(roomId).catch(() => navigate('/rooms'))
    getMessages(roomId).then((msgs) => setMessages(roomId, msgs))
    fetchMyRooms()

    getMembers(roomId).then((members) => {
      const me = members.find((m) => m.userId === user?.id)
      setIsAdmin(me?.role === 'OWNER' || me?.role === 'ADMIN')
    }).catch(() => {})

    return () => setActiveRoom(null)
  }, [roomId])

  async function handleLogout() {
    await logout()
    setUser(null)
    navigate('/login')
  }

  async function handleDeleteRoom() {
    if (!roomId) return
    if (!confirm(`Permanently delete room #${room?.name ?? roomId}? All messages and files will be lost.`)) return
    try {
      await deleteRoom(roomId)
      removeRoom(roomId)
      navigate('/rooms')
    } catch (e: unknown) {
      alert((e as Error).message || 'Failed to delete room')
    }
  }

  return (
    <div className="flex flex-col h-screen bg-gray-900 text-white">
      <header className="flex items-center justify-between px-4 py-3 bg-gray-800 border-b border-gray-700 flex-shrink-0">
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate('/rooms')}
            className="text-gray-400 hover:text-white text-sm transition-colors"
          >
            Browse Rooms
          </button>
          {room && <span className="text-gray-500">›</span>}
          {room && <span className="font-semibold">#{room.name}</span>}
        </div>
        <div className="flex items-center gap-3">
          <span className="text-gray-400 text-sm">@{user?.username}</span>
          <button
            onClick={handleLogout}
            className="text-sm bg-gray-700 hover:bg-gray-600 px-3 py-1 rounded transition-colors"
          >
            Logout
          </button>
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        <Sidebar activeRoomId={roomId} />
        <main className="flex-1 flex flex-col overflow-hidden">
          {roomId ? (
            <>
              <MessageList roomId={roomId} isAdmin={isAdmin} />
              <MessageComposer roomId={roomId} />
            </>
          ) : (
            <div className="flex-1 flex items-center justify-center text-gray-400">
              Select a room to start chatting
            </div>
          )}
        </main>
        {roomId && (
          <RoomMembersPanel
            roomId={roomId}
            onOpenBanList={() => setBanListOpen(true)}
            onDeleteRoom={handleDeleteRoom}
          />
        )}
      </div>

      {banListOpen && roomId && (
        <BanListModal roomId={roomId} onClose={() => setBanListOpen(false)} />
      )}
    </div>
  )
}
