import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useEffect } from 'react'
import { useAuthStore } from './store/authStore'

function PlaceholderPage({ name }: { name: string }) {
  return (
    <div className="min-h-screen bg-gray-900 text-white flex items-center justify-center">
      <p className="text-gray-400">{name} — coming soon</p>
    </div>
  )
}

function ProtectedPlaceholder() {
  const { user, loading } = useAuthStore()
  if (loading) return null
  if (!user) return <Navigate to="/login" replace />
  return <PlaceholderPage name="Home" />
}

export default function App() {
  const init = useAuthStore(s => s.init)

  useEffect(() => { init() }, [init])

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<PlaceholderPage name="Login" />} />
        <Route path="/register" element={<PlaceholderPage name="Register" />} />
        <Route path="/" element={<ProtectedPlaceholder />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
