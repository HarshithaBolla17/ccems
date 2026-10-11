import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api'
import { relative } from '../utils'

export default function NotificationBell() {
  const nav = useNavigate()
  const [sum, setSum] = useState({ unreadNotifications: 0, unreadChats: 0 })
  const [open, setOpen] = useState(false)
  const [list, setList] = useState(null)
  const ref = useRef(null)

  const poll = useCallback(() => {
    api.get('/notifications/summary').then((r) => setSum(r.data)).catch(() => {})
  }, [])
  useEffect(() => {
    poll()
    const t = setInterval(poll, 15000)
    return () => clearInterval(t)
  }, [poll])
  useEffect(() => {
    const close = (e) => ref.current && !ref.current.contains(e.target) && setOpen(false)
    document.addEventListener('mousedown', close)
    return () => document.removeEventListener('mousedown', close)
  }, [])

  const toggle = () => {
    const next = !open
    setOpen(next)
    if (next) api.get('/notifications').then((r) => setList(r.data)).catch(() => {})
  }
  const go = async (n) => {
    setOpen(false)
    if (!n.read) await api.post(`/notifications/${n.notificationId}/read`).catch(() => {})
    poll()
    if (n.link) nav(n.link)
  }
  const readAll = async () => {
    await api.post('/notifications/read-all').catch(() => {})
    setList((l) => l && { ...l, unread: 0, items: l.items.map((i) => ({ ...i, read: true })) })
    poll()
  }

  return (
    <div className="bell-wrap" ref={ref}>
      <button className="icon-btn bell" onClick={toggle} aria-label="Notifications">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
          <path d="M6 8a6 6 0 1 1 12 0c0 7 3 9 3 9H3s3-2 3-9" /><path d="M10.3 21a1.94 1.94 0 0 0 3.4 0" />
        </svg>
        {sum.unreadNotifications > 0 && <span className="dot-badge">{sum.unreadNotifications > 9 ? '9+' : sum.unreadNotifications}</span>}
      </button>
      {open && (
        <div className="bell-pop">
          <div className="bell-head"><b>Notifications</b>
            {list?.unread > 0 && <button onClick={readAll}>Mark all read</button>}
          </div>
          {!list && <p className="muted pad">Loading…</p>}
          {list && list.items.length === 0 && <p className="muted pad">You're all caught up.</p>}
          {list?.items.map((n) => (
            <button key={n.notificationId} className={`bell-item ${n.read ? '' : 'unread'}`} onClick={() => go(n)}>
              <b>{n.title}</b>
              {n.body && <span>{n.body}</span>}
              <small>{relative(n.createdAt)}</small>
            </button>
          ))}
        </div>
      )}
    </div>
  )
}
