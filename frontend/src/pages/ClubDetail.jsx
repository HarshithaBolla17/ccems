import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api, { errMsg } from '../api'
import { useAuth } from '../auth'
import { useToast } from '../toast'
import EventTicket from '../components/EventTicket'
import { MembershipButton } from '../components/ClubCard'
import Dropdown from '../components/Dropdown'
import { Empty, Monogram, PageLoading, Pager, Skeletons } from '../components/ui'
import { hueFor } from '../utils'

export default function ClubDetail() {
  const { id } = useParams()
  const { user } = useAuth()
  const toast = useToast()
  const [club, setClub] = useState(null)
  const [events, setEvents] = useState(null)
  const [page, setPage] = useState(0)
  const [dir, setDir] = useState('desc')
  const [missing, setMissing] = useState(false)

  useEffect(() => {
    setClub(null)
    setMissing(false)
    api.get(`/clubs/${id}`).then((r) => setClub(r.data)).catch(() => setMissing(true))
  }, [id])

  useEffect(() => {
    setEvents(null)
    api.get(`/clubs/${id}/events`, { params: { page, size: 6, sortBy: 'eventDate', dir } })
      .then((r) => setEvents(r.data))
      .catch((e) => toast.error(errMsg(e)))
  }, [id, page, dir]) // eslint-disable-line react-hooks/exhaustive-deps

  const replaceEvent = (ev) =>
    setEvents((d) => ({ ...d, content: d.content.map((e) => (e.eventId === ev.eventId ? ev : e)) }))

  if (missing) {
    return (
      <main className="page"><div className="container">
        <Empty title="That club doesn't exist" action={<Link to="/clubs" className="btn btn-primary">Back to clubs</Link>}>
          It may have been removed, or the link is wrong.
        </Empty>
      </div></main>
    )
  }
  if (!club) return <PageLoading />

  return (
    <main className="page">
      <div className="container">
        <section className="club-hero" style={{ '--h': hueFor(club.name) }}>
          <div className="row wrap spread" style={{ alignItems: 'flex-start', gap: 24 }}>
            <div className="row" style={{ gap: 22, alignItems: 'flex-start' }}>
              <Monogram name={club.name} size="xl" />
              <div>
                <span className="small" style={{ opacity: 0.85 }}>{club.category || 'Open to all'}{club.status !== 'ACTIVE' && ' · Inactive'}</span>
                <h1>{club.name}</h1>
                <p>{club.description}</p>
              </div>
            </div>
            <MembershipButton club={club} onChange={setClub} size="lg" light />
          </div>
          <div className="facts">
            <span><b>{club.memberCount}</b>members</span>
            <span><b>{club.upcomingEvents}</b>upcoming events</span>
            <span>Coordinated by <b style={{ fontSize: '1rem' }}>{club.coordinatorName || 'no one yet'}</b></span>
          </div>
        </section>

        <div className="page-head" style={{ marginBottom: 22 }}>
          <h2 style={{ fontSize: '1.7rem' }}>Events</h2>
          <Dropdown ariaLabel="Sort events" style={{ width: 190 }} value={dir}
            onChange={(v) => { setDir(v); setPage(0) }}
            options={[{ value: 'desc', label: 'Newest first' }, { value: 'asc', label: 'Oldest first' }]} />
        </div>

        {!events ? (
          <Skeletons className="ticket-grid" count={4} height={190} />
        ) : events.content.length ? (
          <>
            <div className="ticket-grid">
              {events.content.map((e) => <EventTicket key={e.eventId} event={e} onChange={replaceEvent} />)}
            </div>
            <Pager page={events.page} totalPages={events.totalPages} onChange={setPage} />
          </>
        ) : (
          <Empty title="No events yet">
            {user?.role === 'STUDENT' ? 'Join the club and you will see its events here as soon as they are announced.' : 'This club has not announced any events.'}
          </Empty>
        )}
      </div>
    </main>
  )
}
