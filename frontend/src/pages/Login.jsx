import { useState } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { errMsg } from '../api'
import { useAuth } from '../auth'
import { Avatar } from '../components/ui'
import { homePath } from '../utils'

const DEMOS = [
  { name: 'B. Harshitha', role: 'Student', email: 'student@ccems.edu', password: 'Student@123' },
  { name: 'Arjun Menon', role: 'Coordinator of Codestorm and Robotics Guild', email: 'coordinator@ccems.edu', password: 'Coord@123' },
  { name: 'Dr. Meera Iyer', role: 'Administrator', email: 'admin@ccems.edu', password: 'Admin@123' },
]

export default function Login() {
  const { user, login } = useAuth()
  const nav = useNavigate()
  const loc = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (user) return <Navigate to={homePath(user.role)} replace />

  async function submit(e, creds) {
    e?.preventDefault()
    const c = creds || { email, password }
    setBusy(true)
    setError('')
    try {
      const u = await login(c.email, c.password)
      nav(loc.state?.from || homePath(u.role), { replace: true })
    } catch (err) {
      setError(errMsg(err))
      setBusy(false)
    }
  }

  return (
    <div className="auth">
      <div className="auth-form">
        <div className="inner">
          <h1>Welcome back</h1>
          <p className="lede">Sign in to see your clubs, events and certificates.</p>

          {error && <div className="alert" role="alert">{error}</div>}

          <form className="stack" onSubmit={submit}>
            <div className="field">
              <label htmlFor="email">Email</label>
              <input id="email" className="input" type="email" autoComplete="username" required
                value={email} onChange={(e) => setEmail(e.target.value)} placeholder="you@college.edu" />
            </div>
            <div className="field">
              <label htmlFor="password">Password</label>
              <input id="password" className="input" type="password" autoComplete="current-password" required
                value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Your password" />
            </div>
            <button className="btn btn-primary btn-lg btn-block" disabled={busy}>
              {busy ? 'Signing in…' : 'Sign in'}
            </button>
          </form>

          <p className="muted small" style={{ marginTop: 22 }}>
            New to CCEMS? <Link to="/register" style={{ color: 'var(--primary)', fontWeight: 600 }}>Create an account</Link>
          </p>
        </div>
      </div>

      <aside className="auth-side">
        <div className="inner">
          <h2>Registered. Attended. Certified.</h2>
          <p>Try the demo with one of the accounts below. Each one opens a different side of the platform.</p>
          <div className="demo-list">
            {DEMOS.map((d) => (
              <button key={d.email} className="demo-btn" disabled={busy} onClick={() => submit(null, d)}>
                <Avatar name={d.name} />
                <span>
                  <b>{d.name}</b>
                  <span>{d.role}</span>
                </span>
              </button>
            ))}
          </div>
        </div>
      </aside>
    </div>
  )
}
