import { useState, useEffect, useCallback } from 'react'
import { useAuthStore } from '../store/authStore'
import { getMembers, setMemberRole, banUser } from '../api/rooms'
import type { Member } from '../api/rooms'

interface Props {
  roomId: string
  onOpenBanList: () => void
  onDeleteRoom: () => void
}

const ROLE_BADGE: Record<string, string> = {
  OWNER: 'bg-yellow-600 text-yellow-100',
  ADMIN: 'bg-purple-700 text-purple-100',
  MEMBER: 'bg-gray-600 text-gray-200',
}

export default function RoomMembersPanel({ roomId, onOpenBanList, onDeleteRoom }: Props) {
  const { user } = useAuthStore()
  const [members, setMembers] = useState<Member[]>([])
  const [loading, setLoading] = useState(true)
  const [acting, setActing] = useState<Record<string, boolean>>({})

  const myRole = members.find((m) => m.userId === user?.id)?.role ?? null

  const fetchMembers = useCallback(async () => {
    try {
      setMembers(await getMembers(roomId))
    } catch {
      // ignore — user may not be a member yet
    } finally {
      setLoading(false)
    }
  }, [roomId])

  useEffect(() => {
    setLoading(true)
    fetchMembers()
  }, [fetchMembers])

  async function handleKick(m: Member) {
    if (!confirm(`Kick @${m.username} from this room?`)) return
    setActing((a) => ({ ...a, [m.userId]: true }))
    try {
      await banUser(roomId, m.userId)
      await fetchMembers()
    } catch (e: unknown) {
      alert((e as Error).message || 'Failed to kick')
    } finally {
      setActing((a) => { const n = { ...a }; delete n[m.userId]; return n })
    }
  }

  async function handleSetRole(m: Member, role: 'ADMIN' | 'MEMBER') {
    const verb = role === 'ADMIN' ? 'Promote' : 'Demote'
    if (!confirm(`${verb} @${m.username}?`)) return
    setActing((a) => ({ ...a, [m.userId]: true }))
    try {
      await setMemberRole(roomId, m.userId, role)
      await fetchMembers()
    } catch (e: unknown) {
      alert((e as Error).message || 'Failed to change role')
    } finally {
      setActing((a) => { const n = { ...a }; delete n[m.userId]; return n })
    }
  }

  // OWNER can kick any non-owner; ADMIN can kick members only
  const canKick = (target: Member) => {
    if (!myRole || myRole === 'MEMBER') return false
    if (target.role === 'OWNER') return false
    if (myRole === 'ADMIN' && target.role !== 'MEMBER') return false
    return target.userId !== user?.id
  }

  // OWNER only can promote MEMBER to ADMIN
  const canPromote = (target: Member) =>
    myRole === 'OWNER' && target.role === 'MEMBER' && target.userId !== user?.id

  // OWNER only can demote ADMIN to MEMBER
  const canDemote = (target: Member) =>
    myRole === 'OWNER' &&
    target.role === 'ADMIN' &&
    target.userId !== user?.id

  return (
    <aside className="w-48 flex-shrink-0 bg-gray-800 border-l border-gray-700 flex flex-col text-sm">
      <div className="px-3 py-2 border-b border-gray-700 flex items-center justify-between">
        <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider">Members</span>
        <span className="text-gray-500 text-xs">{members.length}</span>
      </div>

      <div className="flex-1 overflow-y-auto py-1">
        {loading ? (
          <p className="px-3 py-2 text-gray-500 text-xs">Loading…</p>
        ) : (
          members.map((m) => (
            <div key={m.userId} className="px-3 py-1.5 hover:bg-gray-700">
              <div className="flex items-center gap-1.5 min-w-0">
                <span className="truncate text-gray-200 text-xs">@{m.username}</span>
                <span className={`flex-shrink-0 text-[10px] px-1 rounded ${ROLE_BADGE[m.role]}`}>
                  {m.role}
                </span>
              </div>
              {acting[m.userId] ? (
                <span className="text-[10px] text-gray-500">working…</span>
              ) : (
                (canKick(m) || canPromote(m) || canDemote(m)) && (
                  <div className="flex gap-1.5 mt-0.5 flex-wrap">
                    {canKick(m) && (
                      <button
                        onClick={() => handleKick(m)}
                        className="text-[10px] text-red-400 hover:text-red-300"
                      >
                        kick
                      </button>
                    )}
                    {canPromote(m) && (
                      <button
                        onClick={() => handleSetRole(m, 'ADMIN')}
                        className="text-[10px] text-purple-400 hover:text-purple-300"
                      >
                        +admin
                      </button>
                    )}
                    {canDemote(m) && (
                      <button
                        onClick={() => handleSetRole(m, 'MEMBER')}
                        className="text-[10px] text-gray-400 hover:text-gray-200"
                      >
                        demote
                      </button>
                    )}
                  </div>
                )
              )}
            </div>
          ))
        )}
      </div>

      {(myRole === 'OWNER' || myRole === 'ADMIN') && (
        <div className="p-2 border-t border-gray-700 space-y-1">
          <button
            onClick={onOpenBanList}
            className="w-full text-left text-xs text-gray-400 hover:text-white px-2 py-1 rounded hover:bg-gray-700 transition-colors"
          >
            Ban list
          </button>
          {myRole === 'OWNER' && (
            <button
              onClick={onDeleteRoom}
              className="w-full text-left text-xs text-red-500 hover:text-red-400 px-2 py-1 rounded hover:bg-gray-700 transition-colors"
            >
              Delete room
            </button>
          )}
        </div>
      )}
    </aside>
  )
}
