import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg } from '../api'
import { useAuth } from '../auth'
import EventTicket from '../components/EventTicket'
import ClubCard from '../components/ClubCard'
import { Badge, Empty, Skeletons } from '../components/ui'
import { relative } from '../utils'

const greeting = () => {
  const h = new Date().getHours()
  return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'
}

export default function Home() {
  const { user } = useAuth()
  const [d, setD] = useState(null)
  const [err, setErr] = useState('')

  const load = () => api.get('/home').then((r) => setD(r.data)).catch((e) => setErr(errMsg(e)))
  useEffect(() => { load() }, [])

  const role = user.role
  const consoleLink = role === 'ADMIN' ? '/admin' : role === 'COORDINATOR' ? '/coordinator' : '/profile'
  const consoleLabel = role === 'ADMIN' ? 'Open admin console' : role === 'COORDINATOR' ? 'Open coordinator console' : 'My profile & certificates'

  return (
    <main className="page"><div className="container stack" style={{ gap: 28 }}>
      <header className="home-hero">
        <div>
          <p className="eyebrow">{greeting()}</p>
          <h1>{user.fullName.split(' ')[0]}, here is your day</h1>
        </div>
        <div className="row wrap">
          <Link to="/events" className="btn btn-primary">Browse events</Link>
          <Link to={consoleLink} className="btn btn-ghost">{consoleLabel}</Link>
        </div>
      </header>

      {err && <p className="field-error">{err}</p>}
      {!d && !err && <Skeletons count={3} height={110} />}

      {d && (
        <>
          {d.stats?.length > 0 && (
            <section className="stat-row">
              {d.stats.map((s) => (
                <div key={s.label} className="stat"><b>{s.value}</b><span>{s.label}</span>{s.hint && <small>{s.hint}</small>}</div>
              ))}
            </section>
          )}

          <section>
            <h2 className="h-sm">Needs your attention</h2>
            {d.attention.length === 0 ? (
              <Empty title="Nothing pending">You're all caught up.</Empty>
            ) : (
              <div className="attn-list">
                {d.attention.map((a, i) => (
                  <Link key={i} to={a.link || '/home'} className={`attn ${a.tone || ''}`}>
                    <div><b>{a.title}</b><span>{a.detail}</span></div>
                    {a.until && <Badge tone={a.tone === 'bad' ? 'bad' : undefined}>{relative(a.until)}</Badge>}
                  </Link>
                ))}
              </div>
            )}
          </section>

          {d.nextEvent && (
            <section>
              <h2 className="h-sm">{role === 'STUDENT' ? 'Your next event' : 'Next event'}</h2>
              <EventTicket event={d.nextEvent} onChange={load} />
            </section>
          )}

          {d.latestCertificate && (
            <section className="cert-strip">
              <div><b>Latest certificate</b><span>{d.latestCertificate.eventTitle} · {d.latestCertificate.clubName}</span></div>
              <Link className="btn btn-soft btn-sm" to={`/certificate/${d.latestCertificate.certificateId}`}>View</Link>
            </section>
          )}

          {d.managedClubs?.length > 0 && (
            <section>
              <h2 className="h-sm">Your clubs</h2>
              <div className="club-grid">{d.managedClubs.map((c) => <ClubCard key={c.clubId} club={c} />)}</div>
            </section>
          )}

          {d.suggestedEvents?.length > 0 && (
            <section>
              <h2 className="h-sm">Open for registration</h2>
              <div className="stack">{d.suggestedEvents.map((e) => <EventTicket key={e.eventId} event={e} onChange={load} />)}</div>
            </section>
          )}

          {d.suggestedClubs?.length > 0 && (
            <section>
              <h2 className="h-sm">Clubs you might like</h2>
              <div className="club-grid">{d.suggestedClubs.map((c) => <ClubCard key={c.clubId} club={c} />)}</div>
            </section>
          )}
        </>
      )}
    </div></main>
  )
}
