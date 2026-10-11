import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import api, { errMsg } from '../api'
import { useToast } from '../toast'
import Dropdown from '../components/Dropdown'
import EventTicket from '../components/EventTicket'
import { Empty, Pager, Skeletons } from '../components/ui'
import { SearchIcon } from '../icons'

const SORTS = {
  soonest: { label: 'Soonest first', sortBy: 'eventDate', dir: 'asc' },
  latest: { label: 'Latest first', sortBy: 'eventDate', dir: 'desc' },
  deadline: { label: 'Registration closing soon', sortBy: 'registrationDeadline', dir: 'asc' },
  title: { label: 'Title A to Z', sortBy: 'title', dir: 'asc' },
}

export default function Events() {
  const toast = useToast()
  const [sp] = useSearchParams()
  const [q, setQ] = useState(sp.get('q') || '')
  const [debounced, setDebounced] = useState(sp.get('q') || '')
  const [status, setStatus] = useState('UPCOMING')
  const [openOnly, setOpenOnly] = useState(false)
  const [sort, setSort] = useState('soonest')
  const [page, setPage] = useState(0)
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const t = setTimeout(() => { setDebounced(q); setPage(0) }, 300)
    return () => clearTimeout(t)
  }, [q])

  useEffect(() => {
    let live = true
    setLoading(true)
    const s = SORTS[sort]
    api.get('/events', {
      params: { page, size: 8, q: debounced || undefined, status: status || undefined, openOnly, sortBy: s.sortBy, dir: s.dir },
    })
      .then((r) => live && setData(r.data))
      .catch((e) => toast.error(errMsg(e)))
      .finally(() => live && setLoading(false))
    return () => { live = false }
  }, [page, debounced, status, openOnly, sort]) // eslint-disable-line react-hooks/exhaustive-deps

  const replace = (ev) =>
    setData((d) => ({ ...d, content: d.content.map((e) => (e.eventId === ev.eventId ? ev : e)) }))
  const reset = (fn) => (e) => { fn(e); setPage(0) }

  return (
    <main className="page">
      <div className="container">
        <div className="page-head">
          <div>
            <h1>What's on</h1>
            <p>Every event from every club. Register while seats last: capacity and deadlines are checked the moment you click.</p>
          </div>
        </div>

        <div className="toolbar">
          <div className="search">
            <SearchIcon />
            <input className="input" placeholder="Search events or clubs" value={q}
              onChange={(e) => setQ(e.target.value)} aria-label="Search events" />
          </div>
          <Dropdown ariaLabel="Status" style={{ width: 210 }} value={status} onChange={reset(setStatus)}
            options={[
              { value: '', label: 'All events' },
              { value: 'UPCOMING', label: 'Upcoming' },
              { value: 'CLOSED', label: 'Registration closed' },
              { value: 'COMPLETED', label: 'Completed' },
            ]} />
          <Dropdown ariaLabel="Sort by" style={{ width: 250 }} value={sort} onChange={reset(setSort)}
            options={Object.entries(SORTS).map(([k, s]) => ({ value: k, label: s.label }))} />
          <label className="check">
            <input type="checkbox" checked={openOnly} onChange={reset((e) => setOpenOnly(e.target.checked))} />
            Open for registration
          </label>
        </div>

        {loading && !data ? (
          <Skeletons className="ticket-grid" count={4} height={200} />
        ) : data?.content.length ? (
          <>
            <div className="ticket-grid" style={{ opacity: loading ? 0.6 : 1, transition: 'opacity .15s' }}>
              {data.content.map((e) => <EventTicket key={e.eventId} event={e} onChange={replace} />)}
            </div>
            <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />
          </>
        ) : (
          <Empty title="No events match those filters">Widen the status filter or clear the search to see more.</Empty>
        )}
      </div>
    </main>
  )
}
