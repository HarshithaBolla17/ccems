import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api from '../api'
import ChatPanel from '../components/ChatPanel'
import { Empty } from '../components/ui'
import { relative } from '../utils'

export default function Chats() {
  const { scope, id } = useParams()
  const nav = useNavigate()
  const [rooms, setRooms] = useState(null)
  useEffect(() => {
    const f = () => api.get('/chat/rooms').then((r) => setRooms(r.data)).catch(() => setRooms([]))
    f()
    const t = setInterval(f, 8000)
    return () => clearInterval(t)
  }, [])

  const current = rooms?.find((r) => r.scope === scope && String(r.id) === id)
  return (
    <main className="page"><div className="container">
      <h1 style={{ marginBottom: 16 }}>Chats</h1>
      <div className="chats">
        <aside className="rooms">
          {rooms && rooms.length === 0 && <p className="muted pad">Join a club or register for an event to start chatting.</p>}
          {rooms?.map((r) => (
            <button key={`${r.scope}${r.id}`} className={`room ${current === r ? 'active' : ''}`} onClick={() => nav(`/chats/${r.scope}/${r.id}`)}>
              <div className="row"><b>{r.title}</b>{r.unread > 0 && <span className="dot-badge static">{r.unread}</span>}</div>
              <span className="muted small">{r.lastMessage ? `${r.lastSender}: ${r.lastMessage}` : r.subtitle}</span>
              {r.lastAt && <small className="muted">{relative(r.lastAt)}</small>}
            </button>
          ))}
        </aside>
        <section>
          {scope && id ? <ChatPanel key={`${scope}${id}`} scope={scope} id={id} /> : <Empty title="Pick a chat">Event and club chats are listed on the left.</Empty>}
        </section>
      </div>
    </div></main>
  )
}
