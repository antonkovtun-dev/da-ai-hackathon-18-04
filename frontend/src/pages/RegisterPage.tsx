// frontend/src/pages/RegisterPage.tsx
import { useState, type FormEvent } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { register, type ValidationError } from '../api/auth'
import { useAuthStore } from '../store/authStore'

export default function RegisterPage() {
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [errors, setErrors] = useState<ValidationError[]>([])
  const navigate = useNavigate()
  const setUser = useAuthStore(s => s.setUser)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setErrors([])
    try {
      const user = await register(email, username, password)
      setUser(user)
      navigate('/')
    } catch (errs) {
      if (Array.isArray(errs)) setErrors(errs)
      else setErrors([{ field: 'general', message: 'Registration failed. Please try again.' }])
    }
  }

  const fieldError = (field: string) => errors.find(e => e.field === field)?.message

  return (
    <div className="min-h-screen bg-gray-900 text-white flex items-center justify-center">
      <div className="bg-gray-800 p-8 rounded-lg w-full max-w-sm">
        <h1 className="text-2xl font-bold mb-6">Create Account</h1>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <div>
            <input
              type="email"
              placeholder="Email"
              value={email}
              onChange={e => setEmail(e.target.value)}
              required
              className="w-full bg-gray-700 rounded px-3 py-2 outline-none focus:ring-2 focus:ring-indigo-500"
            />
            {fieldError('email') && (
              <p className="text-red-400 text-xs mt-1">{fieldError('email')}</p>
            )}
          </div>
          <div>
            <input
              type="text"
              placeholder="Username (3–30 chars, letters/digits/_)"
              value={username}
              onChange={e => setUsername(e.target.value)}
              required
              className="w-full bg-gray-700 rounded px-3 py-2 outline-none focus:ring-2 focus:ring-indigo-500"
            />
            {fieldError('username') && (
              <p className="text-red-400 text-xs mt-1">{fieldError('username')}</p>
            )}
          </div>
          <div>
            <input
              type="password"
              placeholder="Password (min 8 characters)"
              value={password}
              onChange={e => setPassword(e.target.value)}
              required
              className="w-full bg-gray-700 rounded px-3 py-2 outline-none focus:ring-2 focus:ring-indigo-500"
            />
            {fieldError('password') && (
              <p className="text-red-400 text-xs mt-1">{fieldError('password')}</p>
            )}
          </div>
          {fieldError('general') && (
            <p className="text-red-400 text-sm">{fieldError('general')}</p>
          )}
          <button
            type="submit"
            className="bg-indigo-600 hover:bg-indigo-700 rounded py-2 font-semibold transition-colors"
          >
            Create Account
          </button>
        </form>
        <p className="mt-4 text-gray-400 text-sm text-center">
          Have an account?{' '}
          <Link to="/login" className="text-indigo-400 hover:underline">
            Sign In
          </Link>
        </p>
      </div>
    </div>
  )
}
