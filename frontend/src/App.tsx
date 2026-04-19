import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useEffect } from 'react'
import { useAuthStore } from './store/authStore'
import ProtectedRoute from './components/ProtectedRoute'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import RoomCatalogPage from './pages/RoomCatalogPage'
import CreateRoomPage from './pages/CreateRoomPage'
import ChatPage from './pages/ChatPage'
import FriendsPage from './pages/FriendsPage'
import DmChatPage from './pages/DmChatPage'
import SettingsPage from './pages/SettingsPage'

export default function App() {
  const init = useAuthStore(s => s.init)

  useEffect(() => { init() }, [init])

  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/rooms" element={
          <ProtectedRoute><RoomCatalogPage /></ProtectedRoute>
        } />
        <Route path="/rooms/new" element={
          <ProtectedRoute><CreateRoomPage /></ProtectedRoute>
        } />
        <Route path="/rooms/:id" element={
          <ProtectedRoute><ChatPage /></ProtectedRoute>
        } />
        <Route path="/friends" element={
          <ProtectedRoute><FriendsPage /></ProtectedRoute>
        } />
        <Route path="/dm/:id" element={
          <ProtectedRoute><DmChatPage /></ProtectedRoute>
        } />
        <Route path="/settings" element={
          <ProtectedRoute><SettingsPage /></ProtectedRoute>
        } />
        <Route path="/" element={<Navigate to="/rooms" replace />} />
        <Route path="*" element={<Navigate to="/rooms" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
