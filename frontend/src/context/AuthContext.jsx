import { createContext, useContext, useEffect, useState, useCallback } from 'react'
import { login as loginApi, register as registerApi } from '../api/authApi'
import { tokenStorage } from '../lib/tokenStorage'
import { eventBus } from '../lib/eventBus'

const USER_KEY = 'cryptopal.user'
const AuthContext = createContext(null)

function readStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function AuthProvider({ children }) {
  // Authenticated only if BOTH a token and matching user info exist -
  // guards against a corrupted/partial localStorage state forcing a
  // silent re-login instead of a broken session.
  const [user, setUser] = useState(() => (tokenStorage.get() ? readStoredUser() : null))

  useEffect(() => {
    // The axios interceptor fires this on any 401 - keeps AuthContext (and
    // therefore ProtectedRoute) in sync even when the token was rejected
    // outside of a user-initiated action.
    return eventBus.on('unauthorized', () => setUser(null))
  }, [])

  const login = useCallback(async (credentials) => {
    const data = await loginApi(credentials)
    tokenStorage.set(data.accessToken)
    const nextUser = { username: data.username, role: data.role, publicId: data.userPublicId }
    localStorage.setItem(USER_KEY, JSON.stringify(nextUser))
    setUser(nextUser)
    return nextUser
  }, [])

  const register = useCallback((payload) => registerApi(payload), [])

  const logout = useCallback(() => {
    tokenStorage.clear()
    localStorage.removeItem(USER_KEY)
    setUser(null)
  }, [])

  const value = { user, isAuthenticated: Boolean(user), login, register, logout }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}
