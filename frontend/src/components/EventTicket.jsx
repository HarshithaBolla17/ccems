import { useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg } from '../api'
import { useAuth } from '../auth'
import { useToast } from '../toast'
import { fmt, hueFor, money, relative } from '../utils'
import { Badge, attendanceBadge } from './ui'
import HoldTimer from './HoldTimer'
import PayModal from './PayModal'
import { ClockIcon, PinIcon } from '../icons'

/** The ticket silhouette: a coloured date stub, a perforation, and a body. */
export function TicketShell({ hue, date, className = '', children }) {
  return (
    <div className="ticket-wrap">
      <article className={`ticket ${className}`} style={{ '--h': hue }}>
        <div className="ticket-stub">
          <span className="dow">{fmt.dow(date)}</span>
          <span className="day">{fmt.day(date)}</span>
          <span className="mon">{fmt.mon(date)}</span>
          <span className="time">{fmt.time(date)}</span>
        </div>
        <div className="ticket-body">{children}</div>
      </article>
    </div>
  )
}

export function CapacityMeter({ registered, capacity }) {
  const pct = Math.min(100, Math.round((registered / capacity) * 100))
  const full = registered >= capacity
  const cls = full ? 'full' : pct >= 85 ? 'hot' : ''
  return (
    <div className="meter-wrap">
      <div className={`meter ${cls}`} role="img" aria-label={`${registered} of ${capacity} seats taken`}>
        <span style={{ width: `${pct}%` }} />
      </div>
      <div className="meter-label">
        {full ? 'Full' : `${capacity - registered} of ${capacity} seats left`}
      </div>
    </div>
  )
}

export function PriceTag({ price }) {
  return <span className={`price ${price > 0 ? '' : 'is-free'}`}>{money(price)}</span>
}

export default function EventTicket({ event: ev, onChange }) {
  const { user } = useAuth()
  const toast = useToast()
  const [busy, setBusy] = useState(false)
  const [paying, setPaying] = useState(false)
  const isStudent = user?.role === 'STUDENT'
  const past = ev.status === 'COMPLETED'
  const paid = ev.price > 0
  const beforeDeadline = new Date(ev.registrationDeadline) > new Date()

  async function run(action, message, after) {
    setBusy(true)
    try {
      const { data } = await action()
      onChange?.(data)
      toast.success(typeof message === 'function' ? message(data) : message)
      after?.(data)
    } catch (e) {
      toast.error(errMsg(e))
    } finally {
      setBusy(false)
    }
  }

  const register = () =>
    run(
      async () => {
        // Students must follow a club to register; do both in one click.
        if (!ev.memberOfClub) await api.post(`/clubs/${ev.clubId}/join`)
        return api.post(`/events/${ev.eventId}/register`)
      },
      (d) => (d.myPayment === 'PENDING'
        ? 'Seat held for 10 minutes. Complete your payment to confirm it.'
        : ev.memberOfClub ? `You're registered for ${ev.title}.` : `Joined ${ev.clubName} and registered for ${ev.title}.`),
      (d) => { if (d.myPayment === 'PENDING') setPaying(true) },
    )

  const cancel = () =>
    run(() => api.delete(`/events/${ev.eventId}/register`),
      ev.myPayment === 'PAID' ? `Registration cancelled. ${money(ev.price)} will be refunded.` : 'Registration cancelled.')

  // The unpaid hold ran out: reload the event so the seat shows as free again.
  const holdExpired = () => api.get(`/events/${ev.eventId}`).then((r) => onChange?.(r.data)).catch(() => {})

  let action = null
  if (isStudent) {
    if (ev.registered && ev.myPayment === 'PENDING') {
      action = (
        <div className="row wrap">
          <Badge tone="gold">Pay within <HoldTimer until={ev.holdExpiresAt} onExpire={holdExpired} /></Badge>
          <button className="btn btn-primary btn-sm" onClick={() => setPaying(true)}>Pay {money(ev.price)}</button>
          <button className="btn btn-ghost btn-sm" onClick={cancel} disabled={busy}>Cancel</button>
        </div>
      )
    } else if (ev.registered) {
      const canCancel = ev.myAttendance === 'REGISTERED' && !past && (ev.myPayment !== 'PAID' || beforeDeadline)
      action = (
        <div className="row wrap">
          {ev.myAttendance === 'REGISTERED'
            ? <Badge tone="ok">{ev.myPayment === 'PAID' ? `Paid ${money(ev.price)}` : "You're registered"}</Badge>
            : attendanceBadge(ev.myAttendance)}
          {ev.myPayment !== 'PENDING' && <Link className="btn btn-primary btn-sm" to={`/events/${ev.eventId}/hub`}>Event hub</Link>}
          {ev.myPayment === 'PAID' && <Link className="btn btn-soft btn-sm" to={`/receipt/${ev.eventId}`}>Receipt</Link>}
          {canCancel && (
            <button className="btn btn-ghost btn-sm" onClick={cancel} disabled={busy}>
              {ev.myPayment === 'PAID' ? 'Cancel and refund' : 'Cancel'}
            </button>
          )}
        </div>
      )
    } else if (ev.registrationOpen) {
      action = (
        <button className="btn btn-primary btn-sm" onClick={register} disabled={busy}>
          {busy ? 'Working…' : ev.memberOfClub ? (paid ? `Register · ${money(ev.price)}` : 'Register') : 'Join club & register'}
        </button>
      )
    } else {
      action = <Badge tone={ev.status === 'UPCOMING' ? 'bad' : undefined}>{ev.closedReason}</Badge>
    }
  } else if (!ev.registrationOpen) {
    action = <Badge>{ev.closedReason}</Badge>
  }

  return (
    <>
      <TicketShell hue={hueFor(ev.clubName)} date={ev.eventDate} className={past ? 'is-past' : ''}>
        <div className="ticket-top">
          <Link to={`/clubs/${ev.clubId}`} className="ticket-club">
            <i />
            {ev.clubName}
          </Link>
          <PriceTag price={ev.price} />
        </div>
        <h3>{ev.title}</h3>
        <div className="ticket-meta">
          {ev.venue && <span><PinIcon width={15} height={15} />{ev.venue}</span>}
          {ev.registrationOpen && (
            <span><ClockIcon width={15} height={15} />Registration closes {relative(ev.registrationDeadline)}</span>
          )}
        </div>
        {ev.description && <p className="ticket-desc">{ev.description}</p>}
        <div className="ticket-foot">
          <CapacityMeter registered={ev.registeredCount} capacity={ev.capacity} />
          {action}
        </div>
      </TicketShell>
      {paying && ev.myPayment === 'PENDING' && (
        <PayModal event={ev} onClose={() => setPaying(false)} onPaid={(d) => onChange?.(d)} />
      )}
    </>
  )
}
