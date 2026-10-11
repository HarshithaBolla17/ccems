import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth'
import { Avatar } from './ui'
import { BrandMark, LogoutIcon, MenuIcon, MoonIcon, SunIcon } from '../icons'
import NotificationBell from './NotificationBell'
import { homePath, roleLabel } from '../utils'
import api from '../api'

export function useTheme() {
  const [theme, setTheme] = useState(() => document.documentElement.getAttribute('data-theme') || 'light')
  const toggle = () => {
    const next = theme === 'dark' ? 'light' : 'dark'
    document.documentElement.setAttribute('data-theme', next)
    try { localStorage.setItem('ccems_theme', next) } catch { /* private mode */ }
    setTheme(next)
  }
  return [theme, toggle]
}

export default function Navbar() {
  const { user, logout } = useAuth()
  const [theme, toggleTheme] = useTheme()
  const [open, setOpen] = useState(false)
  const nav = useNavigate()
  const loc = useLocation()

  useEffect(() => setOpen(false), [loc.pathname])

  const [chatUnread, setChatUnread] = useState(0)
  useEffect(() => {
    if (!user) return undefined
    const f = () => api.get('/notifications/summary').then((r) => setChatUnread(r.data.unreadChats)).catch(() => {})
    f()
    const t = setInterval(f, 15000)
    return () => clearInterval(t)
  }, [user])

  const links = [{ to: '/verify', label: 'Verify certificate' }]
  if (user) {
    links.length = 0
    links.push({ to: '/home', label: 'Home' }, { to: '/clubs', label: 'Clubs' }, { to: '/events', label: 'Events' }, { to: '/chats', label: chatUnread ? `Chats (${chatUnread})` : 'Chats' })
    if (user.role === 'STUDENT') links.push({ to: '/profile', label: 'My profile' })
    if (user.role === 'COORDINATOR') links.push({ to: '/coordinator', label: 'Coordinator console' })
    if (user.role === 'ADMIN') links.push({ to: '/admin', label: 'Admin console' })
  }

  return (
    <header className="nav">
      <div className="container nav-inner">
        <Link to={user ? '/home' : '/'} className="brand" aria-label="CCEMS home">
          <BrandMark className="brand-mark" />
          CCEMS
        </Link>

        <nav className={`nav-links ${open ? 'open' : ''}`} aria-label="Main">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} className={({ isActive }) => (isActive ? 'active' : '')}>
              {l.label}
            </NavLink>
          ))}
        </nav>

        <div className="nav-right">
          <button className="icon-btn" onClick={toggleTheme} aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} theme`}>
            {theme === 'dark' ? <SunIcon /> : <MoonIcon />}
          </button>

          {user ? (
            <>
              <NotificationBell />
              <Link to={homePath(user.role)} className="user-chip" title="Go to your dashboard">
                <Avatar name={user.fullName} />
                <span className="who">
                  <b>{user.fullName}</b>
                  <span>{roleLabel[user.role]}</span>
                </span>
              </Link>
              <button
                className="icon-btn"
                aria-label="Sign out"
                title="Sign out"
                onClick={() => {
                  logout()
                  nav('/')
                }}
              >
                <LogoutIcon />
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-ghost btn-sm">Sign in</Link>
              <Link to="/register" className="btn btn-primary btn-sm">Create account</Link>
            </>
          )}

          {links.length > 0 && (
            <button className="icon-btn nav-burger" onClick={() => setOpen(!open)} aria-label="Menu" aria-expanded={open}>
              <MenuIcon />
            </button>
          )}
        </div>
      </div>
    </header>
  )
}
