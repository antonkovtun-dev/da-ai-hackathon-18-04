import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useFriendStore } from '../store/friendStore'
import {
  sendFriendRequest, acceptRequest, declineRequest,
  cancelRequest, removeFriend, blockUser
} from '../api/friends'
import { getOrCreateThread } from '../api/dm'
import { useDmStore } from '../store/dmStore'

export default function FriendsPage() {
  const { friends, incoming, outgoing, fetchAll, removeIncoming, removeOutgoing, setFriends } = useFriendStore()
  const { addThread } = useDmStore()
  const navigate = useNavigate()
  const [targetUsername, setTargetUsername] = useState('')
  const [requestMessage, setRequestMessage] = useState('')
  const [sendError, setSendError] = useState('')
  const [sendSuccess, setSendSuccess] = useState(false)

  useEffect(() => { fetchAll() }, [])

  async function handleSendRequest() {
    setSendError('')
    setSendSuccess(false)
    try {
      await sendFriendRequest(targetUsername.trim(), requestMessage.trim() || undefined)
      setSendSuccess(true)
      setTargetUsername('')
      setRequestMessage('')
      fetchAll()
    } catch (e: unknown) {
      const err = e as Error
      setSendError(err.message || 'Failed to send request')
    }
  }

  async function handleAccept(id: string) {
    try {
      await acceptRequest(id)
      removeIncoming(id)
      fetchAll()
    } catch (e: unknown) {
      const err = e as Error
      alert(err.message || 'Failed to accept request')
    }
  }

  async function handleDecline(id: string) {
    try {
      await declineRequest(id)
      removeIncoming(id)
    } catch (e: unknown) {
      const err = e as Error
      alert(err.message || 'Failed to decline request')
    }
  }

  async function handleCancel(id: string) {
    try {
      await cancelRequest(id)
      removeOutgoing(id)
    } catch (e: unknown) {
      const err = e as Error
      alert(err.message || 'Failed to cancel request')
    }
  }

  async function handleRemoveFriend(userId: string) {
    if (!confirm('Remove this friend?')) return
    try {
      await removeFriend(userId)
      setFriends(friends.filter((f) => f.userId !== userId))
    } catch (e: unknown) {
      const err = e as Error
      alert(err.message || 'Failed to remove friend')
    }
  }

  async function handleBlock(userId: string) {
    if (!confirm('Block this user? This will also remove the friendship.')) return
    try {
      await blockUser(userId)
      setFriends(friends.filter((f) => f.userId !== userId))
    } catch (e: unknown) {
      const err = e as Error
      alert(err.message || 'Failed to block user')
    }
  }

  async function handleOpenDm(userId: string) {
    try {
      const thread = await getOrCreateThread(userId)
      addThread(thread)
      navigate(`/dm/${thread.id}`)
    } catch (e: unknown) {
      const err = e as Error
      alert(err.message || 'Cannot open DM')
    }
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <div className="max-w-2xl mx-auto p-6 space-y-8">
        <div className="flex items-center justify-between">
          <h1 className="text-2xl font-bold">Friends</h1>
          <button onClick={() => navigate('/rooms')}
            className="text-gray-400 hover:text-white text-sm transition-colors">
            Back to Rooms
          </button>
        </div>

        {/* Send friend request */}
        <section className="bg-gray-800 rounded-lg p-4">
          <h2 className="font-semibold mb-3">Add Friend</h2>
          <div className="flex gap-2 mb-2">
            <input
              type="text"
              placeholder="Username"
              value={targetUsername}
              onChange={(e) => setTargetUsername(e.target.value)}
              className="flex-1 bg-gray-700 rounded px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <button onClick={handleSendRequest}
              className="bg-indigo-600 hover:bg-indigo-700 px-4 py-2 rounded text-sm transition-colors">
              Send
            </button>
          </div>
          <input
            type="text"
            placeholder="Message (optional)"
            value={requestMessage}
            onChange={(e) => setRequestMessage(e.target.value)}
            className="w-full bg-gray-700 rounded px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
          />
          {sendError && <p className="text-red-400 text-xs mt-2">{sendError}</p>}
          {sendSuccess && <p className="text-green-400 text-xs mt-2">Request sent!</p>}
        </section>

        {/* Incoming requests */}
        {incoming.length > 0 && (
          <section className="bg-gray-800 rounded-lg p-4">
            <h2 className="font-semibold mb-3">Incoming Requests ({incoming.length})</h2>
            <div className="space-y-2">
              {incoming.map((req) => (
                <div key={req.id} className="flex items-center justify-between">
                  <div>
                    <span className="text-sm font-medium">{req.senderUsername}</span>
                    {req.message && <p className="text-gray-400 text-xs">{req.message}</p>}
                  </div>
                  <div className="flex gap-2">
                    <button onClick={() => handleAccept(req.id)}
                      className="bg-green-600 hover:bg-green-700 px-3 py-1 rounded text-xs transition-colors">
                      Accept
                    </button>
                    <button onClick={() => handleDecline(req.id)}
                      className="bg-gray-600 hover:bg-gray-500 px-3 py-1 rounded text-xs transition-colors">
                      Decline
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Outgoing requests */}
        {outgoing.length > 0 && (
          <section className="bg-gray-800 rounded-lg p-4">
            <h2 className="font-semibold mb-3">Sent Requests ({outgoing.length})</h2>
            <div className="space-y-2">
              {outgoing.map((req) => (
                <div key={req.id} className="flex items-center justify-between">
                  <span className="text-sm">{req.receiverUsername}</span>
                  <button onClick={() => handleCancel(req.id)}
                    className="text-gray-400 hover:text-red-400 text-xs transition-colors">
                    Cancel
                  </button>
                </div>
              ))}
            </div>
          </section>
        )}

        {/* Friends list */}
        <section className="bg-gray-800 rounded-lg p-4">
          <h2 className="font-semibold mb-3">Friends ({friends.length})</h2>
          {friends.length === 0 ? (
            <p className="text-gray-400 text-sm">No friends yet. Send a request above.</p>
          ) : (
            <div className="space-y-2">
              {friends.map((f) => (
                <div key={f.userId} className="flex items-center justify-between">
                  <span className="text-sm font-medium">{f.username}</span>
                  <div className="flex gap-2">
                    <button onClick={() => handleOpenDm(f.userId)}
                      className="bg-indigo-600 hover:bg-indigo-700 px-3 py-1 rounded text-xs transition-colors">
                      Message
                    </button>
                    <button onClick={() => handleRemoveFriend(f.userId)}
                      className="text-gray-400 hover:text-white text-xs transition-colors">
                      Remove
                    </button>
                    <button onClick={() => handleBlock(f.userId)}
                      className="text-gray-400 hover:text-red-400 text-xs transition-colors">
                      Block
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
