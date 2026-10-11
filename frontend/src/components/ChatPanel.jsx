import { useCallback, useEffect, useRef, useState } from 'react'
import api, { errMsg } from '../api'
import { useToast } from '../toast'
import { fmt } from '../utils'

/** Group chat for an event or a club. Short polling keeps it simple and reliable. */
export default function ChatPanel({ scope, id }) {
  const toast = useToast()
  const [chat, setChat] = useState(null)
  const [text, setText] = useState('')
  const [busy, setBusy] = useState(false)
  const box = useRef(null)
  const base = `/chat/${scope}/${id}`

  const load = useCallback(() => {
    api.get(`${base}/messages`).then((r) => setChat(r.data)).catch(() => {})
    api.post(`${base}/read`).catch(() => {})
  }, [base])

  useEffect(() => {
    setChat(null)
    load()
    const t = setInterval(load, 5000)
    return () => clearInterval(t)
  }, [load])

  useEffect(() => {
    if (box.current) box.current.scrollTop = box.current.scrollHeight
  }, [chat?.messages?.length])

  const send = async (e) => {
    e.preventDefault()
    if (!text.trim()) return
    setBusy(true)
    try {
      await api.post(`${base}/messages`, { body: text.trim() })
      setText('')
      load()
    } catch (err) { toast.error(errMsg(err)) } finally { setBusy(false) }
  }
  const remove = async (m) => {
    try { await api.delete(`/chat/messages/${m.messageId}`); load() } catch (err) { toast.error(errMsg(err)) }
  }
  const mute = async (m) => {
    try { await api.post(`${base}/mutes/${m.senderId}`); toast.success(`${m.senderName} is muted.`) } catch (err) { toast.error(errMsg(err)) }
  }

  if (!chat) return <p className="muted">Loading chat…</p>
  const blocked = chat.closed || chat.muted

  return (
    <div className="chat">
      <div className="chat-log" ref={box}>
        {chat.messages.length === 0 && <p className="muted center">No messages yet. Say hello 👋</p>}
        {chat.messages.map((m) => (
          <div key={m.messageId} className={`msg ${m.mine ? 'mine' : ''}`}>
            <div className="msg-head">
              <b>{m.senderName}</b>
              {m.senderRole !== 'STUDENT' && <span className="role-tag">{m.senderRole === 'ADMIN' ? 'Admin' : 'Organiser'}</span>}
              <span className="muted">{fmt.time(m.createdAt)}</span>
            </div>
            <p className={m.removed ? 'removed' : ''}>{m.removed ? 'Message removed' : m.body}</p>
            {!m.removed && (chat.canModerate || m.mine) && (
              <div className="msg-tools">
                <button onClick={() => remove(m)}>Delete</button>
                {chat.canModerate && !m.mine && <button onClick={() => mute(m)}>Mute</button>}
              </div>
            )}
          </div>
        ))}
      </div>
      {blocked ? (
        <p className="chat-note">{chat.closed ? chat.closedReason || 'This chat is closed.' : 'You have been muted by an organiser.'}</p>
      ) : (
        <form className="chat-form" onSubmit={send}>
          <input className="input" value={text} maxLength={1000} onChange={(e) => setText(e.target.value)}
            placeholder="Message everyone, e.g. venue changed to Hall 2" />
          <button className="btn btn-primary" disabled={busy || !text.trim()}>Send</button>
        </form>
      )}
    </div>
  )
}
