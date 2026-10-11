import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg } from '../api'
import { useToast } from '../toast'
import { Avatar, Badge, Empty, Modal, Monogram, PageLoading, Skeletons, Tabs, attendanceBadge, eventStatusBadge } from '../components/ui'
import Dropdown from '../components/Dropdown'
import { DownloadIcon, PlusIcon } from '../icons'
import { downloadCsv, fmt, hueFor, localInput, money } from '../utils'

export default function Coordinator() {
  const toast = useToast()
  const [clubs, setClubs] = useState(null)
  const [clubId, setClubId] = useState(null)
  const [events, setEvents] = useState(null)
  const [tab, setTab] = useState('events')
  const [selected, setSelected] = useState(null)
  const [creating, setCreating] = useState(false)

  useEffect(() => {
    api.get('/clubs/managed')
      .then((r) => { setClubs(r.data); if (r.data.length) setClubId(r.data[0].clubId) })
      .catch((e) => { toast.error(errMsg(e)); setClubs([]) })
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  const loadEvents = useCallback(() => {
    if (!clubId) return
    api.get(`/clubs/${clubId}/events`, { params: { size: 50, sortBy: 'eventDate', dir: 'desc' } })
      .then((r) => {
        setEvents(r.data.content)
        setSelected((cur) => {
          if (cur && r.data.content.some((e) => e.eventId === cur)) return cur
          const started = r.data.content.find((e) => new Date(e.eventDate) <= new Date() || e.status === 'COMPLETED')
          return (started || r.data.content[0])?.eventId ?? null
        })
      })
      .catch((e) => toast.error(errMsg(e)))
  }, [clubId]) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => { setEvents(null); setSelected(null); loadEvents() }, [loadEvents])

  const club = clubs?.find((c) => c.clubId === clubId)
  const selectedEvent = useMemo(() => events?.find((e) => e.eventId === selected), [events, selected])

  if (!clubs) return <PageLoading />

  if (!clubs.length) {
    return (
      <main className="page"><div className="container">
        <Empty title="No clubs assigned to you yet">
          An admin assigns clubs to coordinators. Once one is assigned, you'll be able to publish events and take attendance here.
        </Empty>
      </div></main>
    )
  }

  const goTo = (t, eventId) => { setSelected(eventId); setTab(t) }

  return (
    <main className="page">
      <div className="container">
        <div className="page-head">
          <div>
            <h1>Coordinator console</h1>
            <p>Publish events, see who registered and mark attendance. Certificates are issued the moment you mark someone present.</p>
          </div>
          <button className="btn btn-primary" onClick={() => setCreating(true)}><PlusIcon /> New event</button>
        </div>

        {clubs.length > 1 && (
          <div className="club-pills" role="group" aria-label="Choose a club">
            {clubs.map((c) => (
              <button key={c.clubId} className="pill" aria-pressed={c.clubId === clubId}
                style={{ '--h': hueFor(c.name) }} onClick={() => setClubId(c.clubId)}>
                <Monogram name={c.name} /> {c.name}
              </button>
            ))}
          </div>
        )}

        <Tabs value={tab} onChange={setTab} tabs={[
          { id: 'events', label: 'Manage events', count: events?.length },
          { id: 'attendance', label: 'Attendance sheet' },
          { id: 'registrations', label: 'Registrations' },
        ]} />

        {!events ? <Skeletons count={3} height={90} className="stack" /> : (
          <>
            {tab === 'events' && (
              <ManageEvents events={events} clubName={club?.name} reload={loadEvents} onCreate={() => setCreating(true)} goTo={goTo} />
            )}
            {tab !== 'events' && (
              events.length === 0 ? (
                <Empty title="No events to show yet" action={<button className="btn btn-primary" onClick={() => setCreating(true)}>Create an event</button>}>
                  Publish your first event and students will be able to register for it.
                </Empty>
              ) : (
                <>
                  <div className="row wrap" style={{ marginBottom: 20 }}>
                    <span className="label">Event</span>
                    <Dropdown ariaLabel="Choose an event" searchable style={{ width: 'min(420px, 100%)' }} menuMinWidth={380}
                      value={selected} onChange={setSelected}
                      options={events.map((e) => ({ value: e.eventId, label: e.title, hint: fmt.date(e.eventDate), hue: hueFor(e.clubName) }))} />
                    {selectedEvent && eventStatusBadge(selectedEvent.status)}
                  </div>
                  {selectedEvent && tab === 'attendance' && <AttendanceSheet event={selectedEvent} onSaved={loadEvents} />}
                  {selectedEvent && tab === 'registrations' && <Registrations event={selectedEvent} />}
                </>
              )
            )}
          </>
        )}
      </div>

      {creating && (
        <EventForm clubId={clubId} clubName={club?.name} onClose={() => setCreating(false)}
          onCreated={() => { setCreating(false); setTab('events'); loadEvents() }} />
      )}
    </main>
  )
}

/* ------------------------------------------------------------------ manage */

function ManageEvents({ events, clubName, reload, onCreate, goTo }) {
  const toast = useToast()
  const [busy, setBusy] = useState(null)

  async function setStatus(ev, status, message) {
    setBusy(ev.eventId)
    try {
      await api.put(`/events/${ev.eventId}/status`, { status })
      toast.success(message)
      reload()
    } catch (e) {
      toast.error(errMsg(e))
    } finally {
      setBusy(null)
    }
  }

  if (!events.length) {
    return (
      <Empty title={`${clubName} has no events yet`} action={<button className="btn btn-primary" onClick={onCreate}>Create the first event</button>}>
        Add a date, a venue and a capacity. Registration closes automatically at the deadline you set.
      </Empty>
    )
  }

  return (
    <div className="panel">
      {events.map((ev) => (
        <div key={ev.eventId} className="manage-event" style={{ '--h': hueFor(ev.clubName) }}>
          <div className="when"><b>{fmt.day(ev.eventDate)}</b><span>{fmt.mon(ev.eventDate)}</span></div>
          <div style={{ minWidth: 0 }}>
            <div className="row wrap" style={{ gap: 10 }}>
              <h4>{ev.title}</h4>
              {eventStatusBadge(ev.status)}
              {ev.price > 0 && <Badge tone="gold" dot={false}>{money(ev.price)}</Badge>}
              {ev.status === 'UPCOMING' && !ev.registrationOpen && <Badge tone="bad">{ev.closedReason}</Badge>}
            </div>
            <p className="muted small" style={{ marginTop: 4 }}>
              {fmt.long(ev.eventDate)}{ev.venue ? ` · ${ev.venue}` : ''} · <b>{ev.registeredCount}</b> of {ev.capacity} registered · deadline {fmt.long(ev.registrationDeadline)}
            </p>
          </div>
          <div className="row wrap">
            <Link className="btn btn-primary btn-sm" to={`/events/${ev.eventId}/hub`}>Event hub</Link>
            <button className="btn btn-ghost btn-sm" onClick={() => goTo('registrations', ev.eventId)}>Registrations</button>
            <button className="btn btn-soft btn-sm" onClick={() => goTo('attendance', ev.eventId)}>Attendance</button>
            {ev.status === 'UPCOMING' && (
              <button className="btn btn-ghost btn-sm" disabled={busy === ev.eventId}
                onClick={() => setStatus(ev, 'CLOSED', 'Registration closed.')}>Close registration</button>
            )}
            {ev.status === 'CLOSED' && (
              <button className="btn btn-ghost btn-sm" disabled={busy === ev.eventId}
                onClick={() => setStatus(ev, 'UPCOMING', 'Registration reopened.')}>Reopen</button>
            )}
            {ev.status !== 'COMPLETED' && (
              <button className="btn btn-ghost btn-sm" disabled={busy === ev.eventId}
                onClick={() => setStatus(ev, 'COMPLETED', `${ev.title} marked as completed.`)}>Mark completed</button>
            )}
          </div>
        </div>
      ))}
    </div>
  )
}

/* -------------------------------------------------------------- new event */

function EventForm({ clubId, clubName, onClose, onCreated }) {
  const toast = useToast()
  const [f, setF] = useState({
    title: '', description: '', venue: '', capacity: 50, price: 0,
    eventDate: localInput(24 * 7 + 10), registrationDeadline: localInput(24 * 5),
  })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api.post(`/clubs/${clubId}/events`, { ...f, capacity: Number(f.capacity), price: Number(f.price) || 0 })
      toast.success(`${f.title} is published.`)
      onCreated()
    } catch (err) {
      setError(errMsg(err))
      setBusy(false)
    }
  }

  return (
    <Modal title={`New event for ${clubName}`} onClose={onClose}>
      {error && <div className="alert" role="alert">{error}</div>}
      <form onSubmit={submit}>
        <div className="form-grid">
          <div className="field full">
            <label htmlFor="t">Title</label>
            <input id="t" className="input" required value={f.title} onChange={set('title')} placeholder="Hack the Campus 2026" />
          </div>
          <div className="field full">
            <label htmlFor="d">Description</label>
            <textarea id="d" className="textarea" value={f.description} onChange={set('description')} placeholder="What will happen, and who is it for?" />
          </div>
          <div className="field">
            <label htmlFor="v">Venue</label>
            <input id="v" className="input" value={f.venue} onChange={set('venue')} placeholder="Seminar Hall B" />
          </div>
          <div className="field">
            <label htmlFor="c">Capacity</label>
            <input id="c" className="input" type="number" min={1} required value={f.capacity} onChange={set('capacity')} />
          </div>
          <div className="field full">
            <label htmlFor="pr">Ticket price in rupees</label>
            <input id="pr" className="input" type="number" min={0} step={10} value={f.price} onChange={set('price')} />
            <span className="faint small">
              {Number(f.price) > 0
                ? `Students pay ${money(Number(f.price))} within 10 minutes of registering, or the seat is released.`
                : 'Leave at 0 for a free event.'}
            </span>
          </div>
          <div className="field">
            <label htmlFor="ed">Event starts</label>
            <input id="ed" className="input" type="datetime-local" required value={f.eventDate} onChange={set('eventDate')} />
          </div>
          <div className="field">
            <label htmlFor="rd">Registration closes</label>
            <input id="rd" className="input" type="datetime-local" required value={f.registrationDeadline} onChange={set('registrationDeadline')} />
          </div>
        </div>
        <div className="modal-foot">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Publishing…' : 'Publish event'}</button>
        </div>
      </form>
    </Modal>
  )
}

/* ------------------------------------------------------------- attendance */

function AttendanceSheet({ event, onSaved }) {
  const toast = useToast()
  const [regs, setRegs] = useState(null)
  const [marks, setMarks] = useState({})
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setRegs(null)
    api.get(`/events/${event.eventId}/registrations`)
      .then((r) => {
        setRegs(r.data)
        setMarks(Object.fromEntries(r.data.map((x) => [x.registrationId, x.attendanceStatus])))
      })
      .catch((e) => toast.error(errMsg(e)))
  }, [event.eventId]) // eslint-disable-line react-hooks/exhaustive-deps

  const started = new Date(event.eventDate) <= new Date() || event.status === 'COMPLETED'
  // Students who haven't paid yet can't be marked, so they stay off the sheet.
  const unpaid = regs ? regs.filter((r) => r.paymentStatus === 'PENDING').length : 0
  const counts = useMemo(() => {
    const v = Object.values(marks)
    return { present: v.filter((s) => s === 'PRESENT').length, absent: v.filter((s) => s === 'ABSENT').length, pending: v.filter((s) => s === 'REGISTERED').length }
  }, [marks])
  const changes = regs ? regs.filter((r) => marks[r.registrationId] !== r.attendanceStatus && marks[r.registrationId] !== 'REGISTERED') : []

  const mark = (id, status) => setMarks((m) => ({ ...m, [id]: status }))
  const markAll = () => setMarks(Object.fromEntries(regs.map((r) => [r.registrationId, r.paymentStatus === 'PENDING' ? 'REGISTERED' : 'PRESENT'])))

  async function save() {
    setSaving(true)
    try {
      const records = changes.map((r) => ({ registrationId: r.registrationId, status: marks[r.registrationId] }))
      const { data } = await api.put(`/events/${event.eventId}/attendance`, { records })
      setRegs(data.registrations)
      setMarks(Object.fromEntries(data.registrations.map((x) => [x.registrationId, x.attendanceStatus])))
      toast.success(data.certificatesIssued
        ? `Attendance saved. ${data.certificatesIssued} certificate${data.certificatesIssued > 1 ? 's' : ''} issued.`
        : 'Attendance saved.')
      onSaved()
    } catch (e) {
      toast.error(errMsg(e))
    } finally {
      setSaving(false)
    }
  }

  if (!regs) return <Skeletons count={3} height={64} className="stack" />
  if (!regs.length) return <Empty title="Nobody has registered yet">Attendance can be marked once students sign up for this event.</Empty>
  const sheet = regs.filter((r) => r.paymentStatus !== 'PENDING')

  return (
    <>
      {unpaid > 0 && (
        <div className="notice">{unpaid} student{unpaid > 1 ? 's have' : ' has'} a held seat but hasn't paid yet, so {unpaid > 1 ? 'they are' : 'they are'} not on this sheet.</div>
      )}
      {!started && (
        <div className="notice">Attendance opens when the event starts on {fmt.long(event.eventDate)}. You can also mark the event completed from Manage events.</div>
      )}
      <div className="panel">
        <div className="panel-head">
          <div className="sheet-summary">
            <span className="pillstat"><b>{counts.present}</b> present</span>
            <span className="pillstat"><b>{counts.absent}</b> absent</span>
            <span className="pillstat"><b>{Math.max(0, counts.pending - unpaid)}</b> not marked</span>
          </div>
          <button className="btn btn-ghost btn-sm" onClick={markAll} disabled={!started}>Mark everyone present</button>
        </div>
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Student</th><th>Registered</th><th>Attendance</th><th>Certificate</th></tr></thead>
            <tbody>
              {sheet.map((r) => {
                const m = marks[r.registrationId]
                const locked = r.attendanceStatus === 'PRESENT'
                return (
                  <tr key={r.registrationId}>
                    <td>
                      <div className="person">
                        <Avatar name={r.studentName} />
                        <div><b>{r.studentName}</b><span>{r.rollNo} · {r.branch}</span></div>
                      </div>
                    </td>
                    <td className="muted">{fmt.long(r.registeredAt)}</td>
                    <td>
                      <div className="segmented" role="group" aria-label={`Attendance for ${r.studentName}`}>
                        <button className="is-ok" aria-pressed={m === 'PRESENT'} disabled={!started} onClick={() => mark(r.registrationId, 'PRESENT')}>Present</button>
                        <button className="is-bad" aria-pressed={m === 'ABSENT'} disabled={!started || locked} onClick={() => mark(r.registrationId, 'ABSENT')}>Absent</button>
                      </div>
                    </td>
                    <td>
                      {r.certificateId
                        ? <Link to={`/certificate/${r.certificateId}`} className="badge badge-ok">Issued · view</Link>
                        : m === 'PRESENT' ? <span className="faint small">Issued on save</span> : <span className="faint small">None</span>}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>

      {changes.length > 0 && (
        <div className="sticky-save">
          <span>{changes.length} unsaved change{changes.length > 1 ? 's' : ''}. Marking someone present issues their certificate.</span>
          <button className="btn btn-primary" onClick={save} disabled={saving}>{saving ? 'Saving…' : 'Save attendance'}</button>
        </div>
      )}
    </>
  )
}

/* ---------------------------------------------------------- registrations */

function Registrations({ event }) {
  const toast = useToast()
  const [regs, setRegs] = useState(null)

  useEffect(() => {
    setRegs(null)
    api.get(`/events/${event.eventId}/registrations`).then((r) => setRegs(r.data)).catch((e) => toast.error(errMsg(e)))
  }, [event.eventId]) // eslint-disable-line react-hooks/exhaustive-deps

  if (!regs) return <Skeletons count={3} height={64} className="stack" />

  const collected = regs.reduce((sum, r) => sum + (r.amountPaid || 0), 0)
  const payLabel = { PAID: 'Paid', PENDING: 'Awaiting payment', NOT_REQUIRED: 'Free', REFUNDED: 'Refunded' }

  const exportCsv = () =>
    downloadCsv(`${event.title.replace(/\W+/g, '-').toLowerCase()}-registrations.csv`, [
      ['Name', 'Roll number', 'Branch', 'Year', 'Email', 'Registered at', 'Attendance', 'Payment', 'Amount paid (INR)'],
      ...regs.map((r) => [r.studentName, r.rollNo, r.branch, r.year, r.studentEmail, r.registeredAt, r.attendanceStatus, payLabel[r.paymentStatus], r.amountPaid]),
    ])

  return (
    <div className="panel">
      <div className="panel-head">
        <div>
          <h3>{regs.length} of {event.capacity} seats taken</h3>
          {event.price > 0 && <p className="muted small">{money(collected)} collected at {money(event.price)} per ticket</p>}
        </div>
        <button className="btn btn-ghost btn-sm" onClick={exportCsv} disabled={!regs.length}><DownloadIcon /> Export CSV</button>
      </div>
      {regs.length ? (
        <div className="table-wrap">
          <table className="table">
            <thead><tr><th>Student</th><th>Roll number</th><th>Year</th><th>Registered</th><th>Payment</th><th>Status</th></tr></thead>
            <tbody>
              {regs.map((r) => (
                <tr key={r.registrationId}>
                  <td><div className="person"><Avatar name={r.studentName} /><div><b>{r.studentName}</b><span>{r.studentEmail}</span></div></div></td>
                  <td>{r.rollNo}<div className="faint tiny">{r.branch}</div></td>
                  <td>{r.year ? `Year ${r.year}` : '-'}</td>
                  <td className="muted">{fmt.long(r.registeredAt)}</td>
                  <td>
                    {r.paymentStatus === 'PAID' && <Badge tone="ok">Paid {money(r.amountPaid)}</Badge>}
                    {r.paymentStatus === 'PENDING' && <Badge tone="gold">Awaiting payment</Badge>}
                    {r.paymentStatus === 'NOT_REQUIRED' && <span className="faint small">Free</span>}
                  </td>
                  <td>{r.paymentStatus === 'PENDING' ? <span className="faint small">Not confirmed</span> : attendanceBadge(r.attendanceStatus)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : (
        <div className="panel-pad muted">No registrations yet. Students can sign up until {fmt.long(event.registrationDeadline)}.</div>
      )}
    </div>
  )
}
