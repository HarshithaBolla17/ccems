import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg } from '../api'
import { useToast } from '../toast'
import { Avatar, Badge, Empty, Modal, Monogram, PageLoading, Pager, Skeletons, Tabs, attendanceBadge, eventStatusBadge } from '../components/ui'
import Dropdown from '../components/Dropdown'
import { PlusIcon, SearchIcon } from '../icons'
import { fmt, hueFor, money } from '../utils'

export default function Admin() {
  const [tab, setTab] = useState('overview')
  return (
    <main className="page">
      <div className="container">
        <div className="page-head">
          <div>
            <h1>Admin console</h1>
            <p>One view of every club, event and registration across campus, and the controls to set clubs up.</p>
          </div>
        </div>
        <Tabs value={tab} onChange={setTab} tabs={[
          { id: 'overview', label: 'Overview' },
          { id: 'clubs', label: 'Clubs and coordinators' },
          { id: 'events', label: 'All events' },
        ]} />
        {tab === 'overview' && <Overview />}
        {tab === 'clubs' && <ClubsAdmin />}
        {tab === 'events' && <EventsAdmin />}
      </div>
    </main>
  )
}

/* ---------------------------------------------------------------- overview */

function Overview() {
  const toast = useToast()
  const [s, setS] = useState(null)

  useEffect(() => {
    api.get('/admin/stats').then((r) => setS(r.data)).catch((e) => toast.error(errMsg(e)))
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  if (!s) return <PageLoading />
  const max = Math.max(1, ...s.clubActivity.map((c) => c.registrations))
  const rate = s.registrations ? Math.round((s.attended / s.registrations) * 100) : 0

  return (
    <>
      <div className="tile-row">
        <div className="tile"><div className="k">Students</div><div className="v">{s.students}</div><div className="d">{s.coordinators} coordinators</div></div>
        <div className="tile"><div className="k">Active clubs</div><div className="v">{s.activeClubs}</div><div className="d">of {s.totalClubs} in total</div></div>
        <div className="tile"><div className="k">Upcoming events</div><div className="v">{s.upcomingEvents}</div><div className="d">{s.completedEvents} completed</div></div>
        <div className="tile"><div className="k">Registrations</div><div className="v">{s.registrations}</div><div className="d">{s.attended} attended · {s.certificates} certificates</div></div>
        <div className="tile"><div className="k">Ticket revenue</div><div className="v">{s.revenue > 0 ? money(s.revenue) : '₹0'}</div><div className="d">from paid events, after refunds</div></div>
      </div>

      <div className="panel" style={{ marginBottom: 28 }}>
        <div className="panel-head">
          <div>
            <h3>Club activity</h3>
            <p className="muted small">Overall turnout so far: {rate}% of registered students attended.</p>
          </div>
          <div className="legend">
            <span><i style={{ background: 'color-mix(in srgb, var(--primary) 35%, var(--surface-2))' }} />Registered</span>
            <span><i style={{ background: 'var(--primary)' }} />Attended</span>
          </div>
        </div>
        <div className="bars">
          {s.clubActivity.map((c) => (
            <div key={c.clubId} className="bar-row">
              <div className="nm" title={c.clubName}>{c.clubName}</div>
              <div className="bar-track" role="img" aria-label={`${c.registrations} registered, ${c.attended} attended`}>
                <div className="reg" style={{ width: `${(c.registrations / max) * 100}%` }} />
                <div className="att" style={{ width: `${(c.attended / max) * 100}%` }} />
              </div>
              <div className="val">{c.attended} / {c.registrations} · {c.events} event{c.events === 1 ? '' : 's'}{c.revenue > 0 ? ` · ${money(c.revenue)}` : ''}</div>
            </div>
          ))}
        </div>
      </div>

      <div className="panel">
        <div className="panel-head"><h3>Latest registrations</h3></div>
        {s.recentRegistrations.length ? (
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Student</th><th>Event</th><th>Club</th><th>Registered</th><th>Status</th></tr></thead>
              <tbody>
                {s.recentRegistrations.map((r) => (
                  <tr key={r.registrationId}>
                    <td><div className="person"><Avatar name={r.studentName} /><div><b>{r.studentName}</b><span>{r.rollNo}</span></div></div></td>
                    <td>{r.eventTitle}</td>
                    <td className="muted">{r.clubName}</td>
                    <td className="muted">{fmt.long(r.registeredAt)}</td>
                    <td>{attendanceBadge(r.attendanceStatus)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <div className="panel-pad muted">No registrations yet.</div>}
      </div>
    </>
  )
}

/* ------------------------------------------------------------------- clubs */

function ClubsAdmin() {
  const toast = useToast()
  const [clubs, setClubs] = useState(null)
  const [coordinators, setCoordinators] = useState([])
  const [creating, setCreating] = useState(false)

  const load = useCallback(() => {
    api.get('/clubs', { params: { size: 50, includeInactive: true } })
      .then((r) => setClubs(r.data.content))
      .catch((e) => toast.error(errMsg(e)))
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    load()
    api.get('/admin/coordinators').then((r) => setCoordinators(r.data)).catch(() => {})
  }, [load])

  const replace = (c) => setClubs((list) => list.map((x) => (x.clubId === c.clubId ? c : x)))

  async function assign(club, coordinatorId) {
    if (!coordinatorId) return
    try {
      const { data } = await api.put(`/clubs/${club.clubId}/coordinator`, { coordinatorId: Number(coordinatorId) })
      replace(data)
      toast.success(`${data.coordinatorName} now coordinates ${club.name}.`)
    } catch (e) { toast.error(errMsg(e)) }
  }

  async function toggle(club) {
    const status = club.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      const { data } = await api.put(`/clubs/${club.clubId}/status`, { status })
      replace(data)
      toast.success(status === 'ACTIVE' ? `${club.name} is active again.` : `${club.name} is now inactive.`)
    } catch (e) { toast.error(errMsg(e)) }
  }

  return (
    <>
      <div className="row spread wrap" style={{ marginBottom: 18 }}>
        <p className="muted">{clubs ? `${clubs.length} clubs` : ''}</p>
        <button className="btn btn-primary" onClick={() => setCreating(true)}><PlusIcon /> Create club</button>
      </div>

      {!clubs ? <Skeletons count={4} height={64} className="stack" /> : (
        <div className="panel">
          <div className="table-wrap">
            <table className="table">
              <thead><tr><th>Club</th><th>Coordinator</th><th className="num">Members</th><th className="num">Upcoming</th><th>Status</th></tr></thead>
              <tbody>
                {clubs.map((c) => (
                  <tr key={c.clubId} style={{ '--h': hueFor(c.name) }}>
                    <td>
                      <div className="person">
                        <Monogram name={c.name} className="" />
                        <div>
                          <b><Link to={`/clubs/${c.clubId}`}>{c.name}</Link></b>
                          <span>{c.category || 'No category'}</span>
                        </div>
                      </div>
                    </td>
                    <td>
                      <Dropdown ariaLabel={`Coordinator for ${c.name}`} searchable style={{ height: 38, minWidth: 210 }}
                        placeholder="Choose a coordinator" value={c.coordinatorId ?? null}
                        onChange={(v) => assign(c, v)}
                        options={coordinators.map((u) => ({ value: u.userId, label: u.fullName, hint: u.email }))} />
                    </td>
                    <td className="num">{c.memberCount}</td>
                    <td className="num">{c.upcomingEvents}</td>
                    <td>
                      <div className="row">
                        <Badge tone={c.status === 'ACTIVE' ? 'ok' : undefined}>{c.status === 'ACTIVE' ? 'Active' : 'Inactive'}</Badge>
                        <button className="btn btn-ghost btn-sm" onClick={() => toggle(c)}>
                          {c.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {creating && (
        <ClubForm coordinators={coordinators} onClose={() => setCreating(false)}
          onCreated={(c) => { setCreating(false); load(); toast.success(`${c.name} created.`) }} />
      )}
    </>
  )
}

function ClubForm({ coordinators, onClose, onCreated }) {
  const [f, setF] = useState({ name: '', category: '', description: '', coordinatorId: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const { data } = await api.post('/clubs', { ...f, coordinatorId: f.coordinatorId ? Number(f.coordinatorId) : null })
      onCreated(data)
    } catch (err) {
      setError(errMsg(err))
      setBusy(false)
    }
  }

  return (
    <Modal title="Create a club" onClose={onClose}>
      {error && <div className="alert" role="alert">{error}</div>}
      <form onSubmit={submit}>
        <div className="form-grid">
          <div className="field full">
            <label htmlFor="cn">Club name</label>
            <input id="cn" className="input" required value={f.name} onChange={set('name')} placeholder="Chess Circle" />
          </div>
          <div className="field">
            <label htmlFor="cc">Category</label>
            <input id="cc" className="input" value={f.category} onChange={set('category')} placeholder="Technology, Arts, Sports…" />
          </div>
          <div className="field">
            <span className="label">Coordinator</span>
            <Dropdown ariaLabel="Coordinator" value={f.coordinatorId} onChange={(v) => setF({ ...f, coordinatorId: v })}
              options={[{ value: '', label: 'Assign later' }, ...coordinators.map((u) => ({ value: u.userId, label: u.fullName, hint: u.email }))]} />
          </div>
          <div className="field full">
            <label htmlFor="cd">Description</label>
            <textarea id="cd" className="textarea" value={f.description} onChange={set('description')} placeholder="What does the club do, and who should join?" />
          </div>
        </div>
        <div className="modal-foot">
          <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
          <button className="btn btn-primary" disabled={busy}>{busy ? 'Creating…' : 'Create club'}</button>
        </div>
      </form>
    </Modal>
  )
}

/* ------------------------------------------------------------------ events */

function EventsAdmin() {
  const toast = useToast()
  const [q, setQ] = useState('')
  const [debounced, setDebounced] = useState('')
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)

  useEffect(() => {
    const t = setTimeout(() => { setDebounced(q); setPage(0) }, 300)
    return () => clearTimeout(t)
  }, [q])

  useEffect(() => {
    api.get('/events', { params: { page, size: 10, q: debounced || undefined, status: status || undefined, sortBy: 'eventDate', dir: 'desc' } })
      .then((r) => setData(r.data))
      .catch((e) => toast.error(errMsg(e)))
  }, [page, debounced, status]) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <>
      <div className="toolbar">
        <div className="search">
          <SearchIcon />
          <input className="input" placeholder="Search events or clubs" value={q} onChange={(e) => setQ(e.target.value)} aria-label="Search events" />
        </div>
        <Dropdown ariaLabel="Status" style={{ width: 220 }} value={status} onChange={(v) => { setStatus(v); setPage(0) }}
          options={[
            { value: '', label: 'All statuses' },
            { value: 'UPCOMING', label: 'Upcoming' },
            { value: 'CLOSED', label: 'Registration closed' },
            { value: 'COMPLETED', label: 'Completed' },
          ]} />
      </div>

      {!data ? <Skeletons count={5} height={56} className="stack" /> : data.content.length ? (
        <>
          <div className="panel">
            <div className="table-wrap">
              <table className="table">
                <thead><tr><th>Event</th><th>When</th><th>Price</th><th>Registration</th><th>Seats</th><th>Status</th></tr></thead>
                <tbody>
                  {data.content.map((e) => (
                    <tr key={e.eventId}>
                      <td><b>{e.title}</b><div className="faint small">{e.clubName}</div></td>
                      <td className="muted">{fmt.long(e.eventDate)}</td>
                      <td>{money(e.price)}</td>
                      <td>{e.registrationOpen ? <Badge tone="ok">Open</Badge> : <span className="faint small">{e.closedReason}</span>}</td>
                      <td className="tabnum">{e.registeredCount} / {e.capacity}</td>
                      <td>{eventStatusBadge(e.status)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
          <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      ) : <Empty title="No events match">Try a different search or status.</Empty>}
    </>
  )
}
