import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api from '../api'
import { Seal } from '../icons'
import { fmt } from '../utils'

// Accepts a bare code or a whole pasted link and returns just the code.
function extractCode(text = '') {
  const match = decodeURIComponent(text).match(/CCEMS-\d{4}-\d{1,9}/i)
  return (match ? match[0] : text).trim().toUpperCase()
}

/** Public page: anyone can check a certificate code without an account. */
export default function VerifyPage() {
  const { code: urlCode } = useParams()
  const nav = useNavigate()
  const [input, setInput] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [failed, setFailed] = useState('')

  useEffect(() => {
    setResult(null)
    setFailed('')
    if (!urlCode) { setInput(''); return }
    const code = extractCode(urlCode)
    setInput(code)
    setLoading(true)
    api.get(`/verify/${encodeURIComponent(code)}`)
      .then((r) => setResult(r.data))
      .catch((e) => setFailed(e.response
        ? 'That code is not in a valid format. It looks like CCEMS-2026-00001.'
        : 'The server could not be reached. Check that the backend is running, then try again.'))
      .finally(() => setLoading(false))
  }, [urlCode])

  const submit = (e) => {
    e.preventDefault()
    const c = extractCode(input)
    if (c) nav(`/verify/${encodeURIComponent(c)}`)
  }

  return (
    <main className="page">
      <div className="narrow">
        <div className="page-head" style={{ marginBottom: 22 }}>
          <div>
            <h1>Verify a certificate</h1>
            <p>Enter the code printed at the bottom of the certificate. You don't need an account.</p>
          </div>
        </div>

        <form className="row" onSubmit={submit} style={{ alignItems: 'stretch' }}>
          <input className="input" style={{ height: 52 }} placeholder="CCEMS-2026-00001" value={input}
            onChange={(e) => setInput(e.target.value)} aria-label="Certificate code" />
          <button className="btn btn-primary btn-lg" disabled={!input.trim() || loading}>
            {loading ? 'Checking…' : 'Verify'}
          </button>
        </form>

        {failed && <div className="alert" style={{ marginTop: 22 }} role="alert">{failed}</div>}

        {result?.valid && (
          <section className="verify-card is-valid" aria-live="polite">
            <Seal className="verify-seal" />
            <div>
              <h2>This certificate is genuine</h2>
              <p className="muted">It was issued by CCEMS and is recorded under this code.</p>
              <dl className="receipt-list" style={{ marginTop: 18 }}>
                <div><dt>Awarded to</dt><dd>{result.studentName}</dd></div>
                <div><dt>Event</dt><dd>{result.eventTitle}</dd></div>
                <div><dt>Club</dt><dd>{result.clubName}</dd></div>
                <div><dt>Event date</dt><dd>{fmt.full(result.eventDate)}</dd></div>
                <div><dt>Issued on</dt><dd>{fmt.full(result.issuedAt)}</dd></div>
                <div><dt>Code</dt><dd className="tabnum">{result.code}</dd></div>
              </dl>
            </div>
          </section>
        )}

        {result && !result.valid && (
          <section className="verify-card is-invalid" aria-live="polite">
            <div>
              <h2>No certificate found for this code</h2>
              <p className="muted">
                Check the code for typing mistakes. It looks like CCEMS-2026-00001. If it still fails, the certificate
                may not have been issued by CCEMS.
              </p>
            </div>
          </section>
        )}
      </div>
    </main>
  )
}
