import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { useEffect } from 'react'
import { useAuthStore } from './store/authStore'
import ProtectedRoute from './components/ProtectedRoute'
import AppLayout from './components/AppLayout'
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
        <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
          <Route path="/rooms" element={<RoomCatalogPage />} />
          <Route path="/rooms/new" element={<CreateRoomPage />} />
          <Route path="/rooms/:id" element={<ChatPage />} />
          <Route path="/friends" element={<FriendsPage />} />
          <Route path="/dm/:id" element={<DmChatPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>
        <Route path="/" element={<Navigate to="/rooms" replace />} />
        <Route path="*" element={<Navigate to="/rooms" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
