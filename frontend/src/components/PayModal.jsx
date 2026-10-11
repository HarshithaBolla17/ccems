import { useState } from 'react'
import api, { errMsg } from '../api'
import { useToast } from '../toast'
import { fmt, money } from '../utils'
import { clock, useCountdown } from './HoldTimer'
import { Modal } from './ui'

const METHODS = [
  { id: 'UPI', title: 'UPI', note: 'Pay with any UPI app' },
  { id: 'CARD', title: 'Card', note: 'Debit or credit card' },
  { id: 'NETBANKING', title: 'Net banking', note: "Through your bank's site" },
]

/** Payment step for a held seat. The gateway is a demo: no real money moves. */
export default function PayModal({ event: ev, onClose, onPaid }) {
  const toast = useToast()
  const [method, setMethod] = useState('UPI')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const left = useCountdown(ev.holdExpiresAt)
  const expired = left === 0

  async function pay() {
    setBusy(true)
    setError('')
    try {
      await new Promise((r) => setTimeout(r, 900)) // short pause so the payment feels real
      const { data } = await api.post(`/events/${ev.eventId}/pay`, { method })
      onPaid(data)
      toast.success(`Payment of ${money(ev.price)} received. You're registered for ${ev.title}.`)
      onClose()
    } catch (e) {
      setError(errMsg(e))
      setBusy(false)
    }
  }

  return (
    <Modal title="Complete your payment" onClose={onClose}>
      <div className="pay-summary">
        <div>
          <div className="muted small">{ev.clubName}</div>
          <b>{ev.title}</b>
          <div className="muted small">{fmt.long(ev.eventDate)}</div>
        </div>
        <div className="pay-amount">{money(ev.price)}</div>
      </div>

      <div className={`pay-hold ${expired ? 'is-expired' : ''}`} role="status">
        {expired
          ? 'Your seat hold ran out. Close this window and register again.'
          : <>Your seat is held for <b className="tabnum">{clock(left)}</b>. Pay before it runs out.</>}
      </div>

      {error && <div className="alert" role="alert" style={{ marginTop: 14 }}>{error}</div>}

      <fieldset className="pay-methods" disabled={busy || expired}>
        <legend className="label">Pay with</legend>
        {METHODS.map((m) => (
          <label key={m.id} className={`pay-method ${method === m.id ? 'is-on' : ''}`}>
            <input type="radio" name="method" value={m.id} checked={method === m.id} onChange={() => setMethod(m.id)} />
            <span><b>{m.title}</b><span className="muted small">{m.note}</span></span>
          </label>
        ))}
      </fieldset>

      <p className="faint small" style={{ marginTop: 14 }}>
        Demo payment: no real money moves. A full refund is given if you cancel before the registration deadline.
      </p>

      <div className="modal-foot">
        <button className="btn btn-ghost" onClick={onClose} disabled={busy}>Pay later</button>
        <button className="btn btn-primary" onClick={pay} disabled={busy || expired}>
          {busy ? 'Processing…' : `Pay ${money(ev.price)}`}
        </button>
      </div>
    </Modal>
  )
}
