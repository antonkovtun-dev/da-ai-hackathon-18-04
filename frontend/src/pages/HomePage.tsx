import { useNavigate } from 'react-router-dom'
import { logout } from '../api/auth'
import { useAuthStore } from '../store/authStore'

export default function HomePage() {
  const { user, setUser } = useAuthStore()
  const navigate = useNavigate()

  async function handleLogout() {
    await logout()
    setUser(null)
    navigate('/login')
  }

  return (
    <div className="min-h-screen bg-gray-900 text-white">
      <nav className="flex items-center justify-between px-6 py-4 bg-gray-800 border-b border-gray-700">
        <h1 className="text-xl font-bold">Chat App</h1>
        <div className="flex items-center gap-4">
          <span className="text-gray-400 text-sm">@{user?.username}</span>
          <button
            onClick={handleLogout}
            className="text-sm bg-gray-700 hover:bg-gray-600 px-3 py-1 rounded transition-colors"
          >
            Logout
          </button>
        </div>
      </nav>
      <main className="flex items-center justify-center h-[calc(100vh-64px)]">
        <p className="text-gray-400">Rooms coming in Phase 2.</p>
      </main>
    </div>
  )
}
