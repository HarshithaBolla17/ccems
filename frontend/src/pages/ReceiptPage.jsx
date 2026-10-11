import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import api from '../api'
import { Badge, Empty, PageLoading } from '../components/ui'
import { BrandMark, PrintIcon } from '../icons'
import { fmt, money } from '../utils'

const METHOD = { UPI: 'UPI', CARD: 'Card', NETBANKING: 'Net banking' }

export default function ReceiptPage() {
  const { eventId } = useParams()
  const [r, setR] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    api.get(`/events/${eventId}/receipt`)
      .then((res) => setR(res.data))
      .catch((e) => setError(e.response?.data?.message || 'This receipt could not be loaded.'))
  }, [eventId])

  if (error) {
    return (
      <main className="page"><div className="container">
        <Empty title="Receipt unavailable" action={<Link to="/profile" className="btn btn-primary">Back to profile</Link>}>{error}</Empty>
      </div></main>
    )
  }
  if (!r) return <PageLoading />

  return (
    <main className="cert-stage">
      <div className="cert-actions">
        <button className="btn btn-primary" onClick={() => window.print()}><PrintIcon /> Print or save as PDF</button>
        <Link to="/profile" className="btn btn-ghost">Back to profile</Link>
      </div>

      <section className="receipt" aria-label={`Receipt ${r.receiptNo}`}>
        <header className="receipt-head">
          <div className="row"><BrandMark className="brand-mark" style={{ color: '#3d3fe8' }} /><b className="receipt-brand">CCEMS</b></div>
          <div style={{ textAlign: 'right' }}>
            <div className="receipt-no">{r.receiptNo}</div>
            {r.status === 'REFUNDED' ? <Badge tone="bad">Refunded</Badge> : <Badge tone="ok">Paid</Badge>}
          </div>
        </header>

        <div className="receipt-amount">
          <span>{r.status === 'REFUNDED' ? 'Amount refunded' : 'Amount paid'}</span>
          <b>{money(r.amount)}</b>
        </div>

        <dl className="receipt-list">
          <div><dt>Event</dt><dd>{r.eventTitle}</dd></div>
          <div><dt>Club</dt><dd>{r.clubName}</dd></div>
          <div><dt>Event date</dt><dd>{fmt.long(r.eventDate)}</dd></div>
          <div><dt>Paid by</dt><dd>{r.studentName}{r.rollNo ? ` (${r.rollNo})` : ''}</dd></div>
          <div><dt>Method</dt><dd>{METHOD[r.method] || r.method}</dd></div>
          <div><dt>Paid on</dt><dd>{r.paidAt ? fmt.long(r.paidAt) : '-'}</dd></div>
          {r.refundedAt && <div><dt>Refunded on</dt><dd>{fmt.long(r.refundedAt)}</dd></div>}
          <div><dt>Reference</dt><dd className="tabnum">{r.reference}</dd></div>
        </dl>

        <p className="faint small receipt-note">
          This is a demo payment, so no real money moved. This receipt is a record of the registration only.
        </p>
      </section>
    </main>
  )
}
