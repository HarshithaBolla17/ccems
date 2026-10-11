import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg } from '../api'
import { useAuth } from '../auth'
import { useToast } from '../toast'
import { Avatar, Badge, Empty, Monogram, PageLoading, Stars, attendanceBadge } from '../components/ui'
import { Seal } from '../icons'
import { fmt, hueFor, money } from '../utils'

export default function Profile() {
  const { user } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)

  useEffect(() => {
    api.get(`/students/${user.userId}/profile`)
      .then((r) => setData(r.data))
      .catch((e) => toast.error(errMsg(e)))
  }, [user.userId]) // eslint-disable-line react-hooks/exhaustive-deps

  async function rate(reg, rating) {
    try {
      await api.put(`/events/${reg.eventId}/feedback`, { rating })
      setData((d) => ({
        ...d,
        registrations: d.registrations.map((r) => (r.registrationId === reg.registrationId ? { ...r, feedbackRating: rating } : r)),
      }))
      toast.success('Thanks for the feedback.')
    } catch (e) {
      toast.error(errMsg(e))
    }
  }

  if (!data) return <PageLoading />
  const { user: me, clubs, registrations, certificates } = data
  const attended = registrations.filter((r) => r.attendanceStatus === 'PRESENT').length

  return (
    <main className="page">
      <div className="container">
        <div className="profile-head">
          <Avatar name={me.fullName} size="lg" />
          <div>
            <h1>{me.fullName}</h1>
            <p className="muted">
              {me.rollNo} · {me.branch}{me.year ? ` · Year ${me.year}` : ''}
            </p>
          </div>
          <div className="stat-row">
            <div><b>{clubs.length}</b><span>Clubs</span></div>
            <div><b>{attended}</b><span>Events attended</span></div>
            <div><b>{certificates.length}</b><span>Certificates</span></div>
          </div>
        </div>

        <div className="two-col">
          <section>
            <h2 className="section-title">Event history <small>{registrations.length} registered</small></h2>
            {registrations.length ? (
              <div className="panel history">
                {registrations.map((r) => (
                  <div key={r.registrationId} className="history-item">
                    <div className="history-date">
                      <b>{fmt.day(r.eventDate)}</b>
                      <span>{fmt.mon(r.eventDate)}</span>
                    </div>
                    <div>
                      <h4>{r.eventTitle}</h4>
                      <span className="muted small">{r.clubName}</span>
                      {r.attendanceStatus === 'PRESENT' && (
                        <div style={{ marginTop: 4 }}>
                          <Stars value={r.feedbackRating || 0} onPick={(n) => rate(r, n)} />
                          {!r.feedbackRating && <span className="faint tiny" style={{ marginLeft: 8 }}>Rate this event</span>}
                        </div>
                      )}
                    </div>
                    <div className="row wrap">
                      {r.paymentStatus === 'PENDING'
                        ? <Badge tone="gold">Awaiting payment</Badge>
                        : attendanceBadge(r.attendanceStatus)}
                      {r.paymentStatus === 'PAID' && (
                        <Link className="btn btn-ghost btn-sm" to={`/receipt/${r.eventId}`}>Receipt · {money(r.amountPaid)}</Link>
                      )}
                      {r.certificateId && <Link className="btn btn-soft btn-sm" to={`/certificate/${r.certificateId}`}>Certificate</Link>}
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <Empty title="No events yet" action={<Link to="/events" className="btn btn-primary">Find an event</Link>}>
                Register for an event and it will show up here, along with your attendance.
              </Empty>
            )}
          </section>

          <aside className="stack" style={{ gap: 32 }}>
            <section>
              <h2 className="section-title">Certificates <small>{certificates.length}</small></h2>
              {certificates.length ? (
                <div className="stack">
                  {certificates.map((c) => (
                    <Link key={c.certificateId} to={`/certificate/${c.certificateId}`} className="cert-mini">
                      <Seal className="seal-sm" />
                      <div className="title" style={{ paddingRight: 40 }}>{c.eventTitle}</div>
                      <div className="muted small" style={{ marginTop: 6 }}>{c.clubName} · {fmt.date(c.issuedAt)}</div>
                      <div className="faint tiny" style={{ marginTop: 8 }}>{c.code}</div>
                    </Link>
                  ))}
                </div>
              ) : (
                <p className="muted">Certificates appear here automatically after a coordinator marks you present.</p>
              )}
            </section>

            <section>
              <h2 className="section-title">My clubs <small>{clubs.length}</small></h2>
              {clubs.length ? (
                <div className="panel mini-clubs">
                  {clubs.map((c) => (
                    <Link key={c.clubId} to={`/clubs/${c.clubId}`} className="mini-club" style={{ '--h': hueFor(c.name) }}>
                      <Monogram name={c.name} />
                      <span><b>{c.name}</b><span className="muted small">{c.upcomingEvents} upcoming</span></span>
                    </Link>
                  ))}
                </div>
              ) : (
                <p className="muted">You haven't joined a club yet. <Link to="/clubs" style={{ color: 'var(--primary)', fontWeight: 600 }}>Browse clubs</Link></p>
              )}
            </section>
          </aside>
        </div>
      </div>
    </main>
  )
}
