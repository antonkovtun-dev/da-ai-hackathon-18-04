import { useState, useEffect } from 'react'
import { listBans, unbanUser } from '../api/rooms'
import type { BanResponse } from '../api/rooms'

interface Props {
  roomId: string
  onClose: () => void
}

export default function BanListModal({ roomId, onClose }: Props) {
  const [bans, setBans] = useState<BanResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [unbanning, setUnbanning] = useState<Record<string, boolean>>({})

  useEffect(() => {
    listBans(roomId)
      .then(setBans)
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [roomId])

  async function handleUnban(userId: string, username: string) {
    if (!confirm(`Unban @${username}?`)) return
    setUnbanning((u) => ({ ...u, [userId]: true }))
    try {
      await unbanUser(roomId, userId)
      setBans((prev) => prev.filter((b) => b.userId !== userId))
    } catch (e: unknown) {
      alert((e as Error).message || 'Failed to unban')
    } finally {
      setUnbanning((u) => { const n = { ...u }; delete n[userId]; return n })
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-gray-800 border border-gray-700 rounded-lg w-full max-w-md p-5 shadow-xl">
        <div className="flex items-center justify-between mb-4">
          <h2 className="font-semibold text-white">Ban List</h2>
          <button
            type="button"
            onClick={onClose}
            className="text-gray-400 hover:text-white text-xl leading-none"
          >
            &times;
          </button>
        </div>

        {loading ? (
          <p className="text-gray-400 text-sm">Loading…</p>
        ) : bans.length === 0 ? (
          <p className="text-gray-400 text-sm">No banned users.</p>
        ) : (
          <ul className="space-y-3 max-h-80 overflow-y-auto">
            {bans.map((ban) => (
              <li
                key={ban.id}
                className="flex items-start justify-between gap-3 border-b border-gray-700 pb-3 last:border-0"
              >
                <div className="min-w-0">
                  <p className="text-sm text-white font-medium">@{ban.username}</p>
                  {ban.reason && (
                    <p className="text-xs text-gray-400 mt-0.5">Reason: {ban.reason}</p>
                  )}
                  <p className="text-xs text-gray-500 mt-0.5">
                    Banned {new Date(ban.bannedAt).toLocaleDateString()}
                  </p>
                </div>
                <button
                  type="button"
                  disabled={unbanning[ban.userId]}
                  onClick={() => handleUnban(ban.userId, ban.username)}
                  className="flex-shrink-0 text-xs bg-gray-700 hover:bg-gray-600 disabled:opacity-50 px-2 py-1 rounded transition-colors"
                >
                  {unbanning[ban.userId] ? '…' : 'Unban'}
                </button>
              </li>
            ))}
          </ul>
        )}

        <button
          type="button"
          onClick={onClose}
          className="mt-4 w-full text-sm bg-gray-700 hover:bg-gray-600 py-2 rounded transition-colors"
        >
          Close
        </button>
      </div>
    </div>
  )
}
