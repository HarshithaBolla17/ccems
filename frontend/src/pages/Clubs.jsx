import { useEffect, useState } from 'react'
import api, { errMsg } from '../api'
import { useToast } from '../toast'
import ClubCard from '../components/ClubCard'
import { Empty, Pager, Skeletons } from '../components/ui'
import { SearchIcon } from '../icons'

export default function Clubs() {
  const toast = useToast()
  const [q, setQ] = useState('')
  const [debounced, setDebounced] = useState('')
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
    api.get('/clubs', { params: { page, size: 9, q: debounced || undefined } })
      .then((r) => live && setData(r.data))
      .catch((e) => toast.error(errMsg(e)))
      .finally(() => live && setLoading(false))
    return () => { live = false }
  }, [page, debounced]) // eslint-disable-line react-hooks/exhaustive-deps

  const replace = (club) =>
    setData((d) => ({ ...d, content: d.content.map((c) => (c.clubId === club.clubId ? club : c)) }))

  return (
    <main className="page">
      <div className="container">
        <div className="page-head">
          <div>
            <h1>Clubs on campus</h1>
            <p>Find the ones that fit, join with a click and their events will be waiting in your feed.</p>
          </div>
        </div>

        <div className="toolbar">
          <div className="search">
            <SearchIcon />
            <input className="input" placeholder="Search by name or interest" value={q}
              onChange={(e) => setQ(e.target.value)} aria-label="Search clubs" />
          </div>
          {data && <span className="muted small">{data.totalElements} club{data.totalElements === 1 ? '' : 's'}</span>}
        </div>

        {loading && !data ? (
          <Skeletons className="club-grid" count={6} height={270} />
        ) : data?.content.length ? (
          <>
            <div className="club-grid" style={{ opacity: loading ? 0.6 : 1, transition: 'opacity .15s' }}>
              {data.content.map((c) => <ClubCard key={c.clubId} club={c} onChange={replace} />)}
            </div>
            <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />
          </>
        ) : (
          <Empty title="No clubs match that search">Try a shorter keyword, or clear the search to see every club.</Empty>
        )}
      </div>
    </main>
  )
}
