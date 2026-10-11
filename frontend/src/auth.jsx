import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import api from './api'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('ccems_user') || 'null')
    } catch {
      return null
    }
  })
  const [loading, setLoading] = useState(!!localStorage.getItem('ccems_token'))

  const store = (data) => {
    localStorage.setItem('ccems_token', data.token)
    localStorage.setItem('ccems_user', JSON.stringify(data.user))
    setUser(data.user)
  }

  const logout = useCallback(() => {
    localStorage.removeItem('ccems_token')
    localStorage.removeItem('ccems_user')
    setUser(null)
  }, [])

  // Restore the session: confirm the saved token is still valid.
  useEffect(() => {
    if (!localStorage.getItem('ccems_token')) return
    api
      .get('/me')
      .then((res) => {
        localStorage.setItem('ccems_user', JSON.stringify(res.data))
        setUser(res.data)
      })
      .catch(() => {})
      .finally(() => setLoading(false))
  }, [])

  // Any 401 from the API signs the user out.
  useEffect(() => {
    window.addEventListener('ccems:logout', logout)
    return () => window.removeEventListener('ccems:logout', logout)
  }, [logout])

  const login = async (email, password) => {
    const { data } = await api.post('/auth/login', { email, password })
    store(data)
    return data.user
  }

  const register = async (payload) => {
    const { data } = await api.post('/auth/register', payload)
    store(data)
    return data.user
  }

  const value = useMemo(() => ({ user, loading, login, register, logout }), [user, loading, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => useContext(AuthContext)
