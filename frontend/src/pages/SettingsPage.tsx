import { useState, useEffect, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'
import { changePassword, deleteAccount } from '../api/auth'
import { getSessions, revokeSession, type SessionInfo } from '../api/sessions'

export default function SettingsPage() {
  const { setUser } = useAuthStore()
  const navigate = useNavigate()

  const [currentPw, setCurrentPw] = useState('')
  const [newPw, setNewPw] = useState('')
  const [pwMsg, setPwMsg] = useState('')
  const [pwError, setPwError] = useState('')

  const [sessions, setSessions] = useState<SessionInfo[]>([])
  const [revoking, setRevoking] = useState<Record<string, boolean>>({})

  useEffect(() => {
    getSessions().then(setSessions).catch(() => {})
  }, [])

  async function handleChangePassword(e: FormEvent) {
    e.preventDefault()
    setPwMsg('')
    setPwError('')
    try {
      await changePassword(currentPw, newPw)
      setPwMsg('Password changed successfully.')
      setCurrentPw('')
      setNewPw('')
    } catch (err: unknown) {
      setPwError((err as Error).message || 'Failed to change password')
    }
  }

  async function handleRevoke(sessionId: string) {
    if (!confirm('Revoke this session?')) return
    setRevoking((r) => ({ ...r, [sessionId]: true }))
    try {
      await revokeSession(sessionId)
      const current = sessions.find((s) => s.sessionId === sessionId)?.current
      setSessions((prev) => prev.filter((s) => s.sessionId !== sessionId))
      if (current) {
        setUser(null)
        navigate('/login')
      }
    } catch { /* ignore */ } finally {
      setRevoking((r) => { const n = { ...r }; delete n[sessionId]; return n })
    }
  }

  async function handleDeleteAccount() {
    if (!confirm('Permanently delete your account? This cannot be undone.')) return
    if (!confirm('Are you sure? All your rooms and data will be lost.')) return
    try {
      await deleteAccount()
      setUser(null)
      navigate('/login')
    } catch { alert('Failed to delete account') }
  }

  function formatDate(iso: string) {
    return new Date(iso).toLocaleString()
  }

  function browserHint(ua: string | null) {
    if (!ua) return 'Unknown browser'
    if (ua.includes('Chrome')) return 'Chrome'
    if (ua.includes('Firefox')) return 'Firefox'
    if (ua.includes('Safari')) return 'Safari'
    return ua.substring(0, 40)
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white p-8">
      <div className="max-w-xl mx-auto space-y-8">
        <div className="flex items-center gap-4">
          <button onClick={() => navigate(-1)} className="text-gray-400 hover:text-white text-sm">← Back</button>
          <h1 className="text-2xl font-bold">Account Settings</h1>
        </div>

        <section className="bg-gray-800 rounded-lg p-6">
          <h2 className="text-lg font-semibold mb-4">Change Password</h2>
          <form onSubmit={handleChangePassword} className="space-y-3">
            <input
              type="password" placeholder="Current password" value={currentPw}
              onChange={(e) => setCurrentPw(e.target.value)} required
              className="w-full bg-gray-700 rounded px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
            />
            <input
              type="password" placeholder="New password (min 8 chars)" value={newPw}
              onChange={(e) => setNewPw(e.target.value)} required minLength={8}
              className="w-full bg-gray-700 rounded px-3 py-2 text-sm outline-none focus:ring-2 focus:ring-indigo-500"
            />
            {pwError && <p className="text-red-400 text-sm">{pwError}</p>}
            {pwMsg && <p className="text-green-400 text-sm">{pwMsg}</p>}
            <button type="submit" className="bg-indigo-600 hover:bg-indigo-700 px-4 py-2 rounded text-sm font-medium">
              Change Password
            </button>
          </form>
        </section>

        <section className="bg-gray-800 rounded-lg p-6">
          <h2 className="text-lg font-semibold mb-4">Active Sessions</h2>
          {sessions.length === 0 ? (
            <p className="text-gray-400 text-sm">No sessions found.</p>
          ) : (
            <div className="space-y-2">
              {sessions.map((s) => (
                <div key={s.sessionId} className="flex items-start justify-between bg-gray-700 rounded p-3">
                  <div className="text-sm space-y-0.5">
                    <p className="text-gray-200">
                      {browserHint(s.userAgent)}
                      {s.current && <span className="ml-2 text-xs text-indigo-400">(this session)</span>}
                    </p>
                    <p className="text-gray-400 text-xs">{s.ipAddress ?? 'Unknown IP'}</p>
                    <p className="text-gray-500 text-xs">Last active: {formatDate(s.lastActiveAt)}</p>
                  </div>
                  <button
                    onClick={() => handleRevoke(s.sessionId)}
                    disabled={revoking[s.sessionId]}
                    className="text-red-400 hover:text-red-300 text-xs disabled:opacity-50 ml-4"
                  >
                    {revoking[s.sessionId] ? '…' : 'Revoke'}
                  </button>
                </div>
              ))}
            </div>
          )}
        </section>

        <section className="bg-gray-800 rounded-lg p-6 border border-red-900">
          <h2 className="text-lg font-semibold text-red-400 mb-2">Danger Zone</h2>
          <p className="text-gray-400 text-sm mb-4">
            Deleting your account permanently removes all your rooms, messages, and files. This cannot be undone.
          </p>
          <button
            onClick={handleDeleteAccount}
            className="bg-red-700 hover:bg-red-600 px-4 py-2 rounded text-sm font-medium"
          >
            Delete Account
          </button>
        </section>
      </div>
    </div>
  )
}
