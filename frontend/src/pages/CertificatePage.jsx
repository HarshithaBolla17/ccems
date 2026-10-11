import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api from '../api'
import { useToast } from '../toast'
import { CornerOrnament, PrintIcon, Seal } from '../icons'
import { Empty, PageLoading } from '../components/ui'
import { fmt } from '../utils'

export default function CertificatePage() {
  const { id } = useParams()
  const toast = useToast()
  const [c, setC] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get(`/certificates/${id}`)
      .then((r) => setC(r.data))
      .catch((e) => setError(e.response?.data?.message || 'This certificate could not be loaded.'))
  }, [id])

  if (error) {
    return (
      <main className="page"><div className="container">
        <Empty title="Certificate unavailable" action={<Link to="/" className="btn btn-primary">Go home</Link>}>{error}</Empty>
      </div></main>
    )
  }
  if (!c) return <PageLoading />

  return (
    <main className="cert-stage">
      <div className="cert-actions">
        <button className="btn btn-primary" onClick={() => window.print()}>
          <PrintIcon /> Print or save as PDF
        </button>
        <button
          className="btn btn-ghost"
          onClick={() => {
            navigator.clipboard?.writeText(`${window.location.origin}/verify/${c.code}`)
            toast.success('Verification link copied.')
          }}
          title="Copy a public link anyone can use to check this certificate"
        >
          Copy verification link
        </button>
        <Link to="/profile" className="btn btn-ghost">Back to profile</Link>
      </div>

      <section className="certificate" aria-label={`Certificate for ${c.studentName}`}>
        <div className="frame" />
        <CornerOrnament className="corner tl" />
        <CornerOrnament className="corner tr" />
        <CornerOrnament className="corner bl" />
        <CornerOrnament className="corner br" />

        <div className="content">
          <div className="org">{c.clubName}</div>
          <h1>Certificate of Participation</h1>
          <p className="line">This certifies that</p>
          <div className="name">{c.studentName}</div>
          {c.rollNo && <div className="rollno">{c.rollNo}{c.branch ? ` · ${c.branch}` : ''}</div>}
          <p className="line">attended and took part in</p>
          <div className="event">{c.eventTitle}</div>
          <div className="meta">held on {fmt.full(c.eventDate)}</div>

          <div className="foot">
            <div className="sig">
              <div className="scrawl">{c.clubName}</div>
              <small>Issuing club</small>
            </div>
            <div style={{ textAlign: 'center' }}>
              <Seal className="seal" />
              <div className="code">{c.code}</div>
            </div>
            <div className="sig">
              <div className="scrawl">{fmt.date(c.issuedAt)}</div>
              <small>Date of issue</small>
            </div>
          </div>
        </div>
      </section>
    </main>
  )
}
