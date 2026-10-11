import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import api, { errMsg } from '../api'
import { useAuth } from '../auth'
import Dropdown from '../components/Dropdown'
import { homePath } from '../utils'

const BRANCHES = ['CSE-P', 'CSE-A', 'CSE-B', 'ECE', 'EEE', 'ME', 'CE', 'IT', 'AIML']

export default function Register() {
  const { user, register } = useAuth()
  const nav = useNavigate()
  const [role, setRole] = useState('STUDENT')
  const [f, setF] = useState({ fullName: '', email: '', password: '', rollNo: '', branch: 'CSE-P', year: 1 })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={homePath(user.role)} replace />
  const set = (k) => (e) => setF({ ...f, [k]: e.target.value })

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const payload = { fullName: f.fullName, email: f.email, password: f.password, role }
      if (role === 'STUDENT') Object.assign(payload, { rollNo: f.rollNo, branch: f.branch, year: Number(f.year) })
      const u = await register(payload)
      nav(homePath(u.role), { replace: true })
    } catch (err) {
      setError(errMsg(err))
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <div className="auth-form">
        <div className="inner">
          <h1>Create your account</h1>
          <p className="lede">It takes a minute. Students can join clubs straight away.</p>

          <div className="segmented" style={{ marginBottom: 22 }} role="group" aria-label="Account type">
            <button type="button" aria-pressed={role === 'STUDENT'} onClick={() => setRole('STUDENT')}>I'm a student</button>
            <button type="button" aria-pressed={role === 'COORDINATOR'} onClick={() => setRole('COORDINATOR')}>I'm a coordinator</button>
          </div>

          {error && <div className="alert" role="alert">{error}</div>}

          <form className="stack" onSubmit={submit}>
            <div className="field">
              <label htmlFor="fullName">Full name</label>
              <input id="fullName" className="input" required value={f.fullName} onChange={set('fullName')} />
            </div>
            <div className="field">
              <label htmlFor="email">College email</label>
              <input id="email" className="input" type="email" required value={f.email} onChange={set('email')} placeholder="you@college.edu" />
            </div>
            <div className="field">
              <label htmlFor="password">Password</label>
              <input id="password" className="input" type="password" required minLength={6} autoComplete="new-password"
                value={f.password} onChange={set('password')} placeholder="At least 6 characters" />
            </div>

            {role === 'STUDENT' && (
              <div className="form-grid">
                <div className="field full">
                  <label htmlFor="rollNo">Roll number</label>
                  <input id="rollNo" className="input" required value={f.rollNo} onChange={set('rollNo')} placeholder="24EG105P55" />
                </div>
                <div className="field">
                  <span className="label">Branch</span>
                  <Dropdown ariaLabel="Branch" value={f.branch} onChange={(v) => setF({ ...f, branch: v })}
                    options={BRANCHES.map((b) => ({ value: b, label: b }))} />
                </div>
                <div className="field">
                  <span className="label">Year</span>
                  <Dropdown ariaLabel="Year" value={Number(f.year)} onChange={(v) => setF({ ...f, year: v })}
                    options={[1, 2, 3, 4].map((y) => ({ value: y, label: `Year ${y}` }))} />
                </div>
              </div>
            )}

            {role === 'COORDINATOR' && (
              <p className="muted small">An admin assigns clubs to coordinators. You'll see yours in the console once they do.</p>
            )}

            <button className="btn btn-primary btn-lg btn-block" disabled={busy}>
              {busy ? 'Creating account…' : 'Create account'}
            </button>
          </form>

          <p className="muted small" style={{ marginTop: 22 }}>
            Already have an account? <Link to="/login" style={{ color: 'var(--primary)', fontWeight: 600 }}>Sign in</Link>
          </p>
        </div>
      </div>

      <aside className="auth-side">
        <div className="inner">
          <h2>Every certificate starts with a sign-up.</h2>
          <p>Join clubs, register for events and watch your portfolio fill up as you attend.</p>
        </div>
      </aside>
    </div>
  )
}
