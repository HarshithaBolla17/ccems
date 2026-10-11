import { useState } from 'react'
import { Link } from 'react-router-dom'
import api, { errMsg } from '../api'
import { useAuth } from '../auth'
import { useToast } from '../toast'
import { hueFor } from '../utils'
import { Badge, Monogram } from './ui'

/** Join / leave button shared by the directory card and the club page. */
export function MembershipButton({ club, onChange, size = 'sm', light }) {
  const { user } = useAuth()
  const toast = useToast()
  const [busy, setBusy] = useState(false)
  if (user?.role !== 'STUDENT' || club.status !== 'ACTIVE') return null

  async function toggle() {
    setBusy(true)
    try {
      const { data } = await api.post(`/clubs/${club.clubId}/${club.joined ? 'leave' : 'join'}`)
      onChange?.(data)
      toast.success(club.joined ? `You left ${club.name}.` : `Welcome to ${club.name}.`)
    } catch (e) {
      toast.error(errMsg(e))
    } finally {
      setBusy(false)
    }
  }

  const cls = club.joined ? 'btn-ghost' : light ? 'btn-light' : 'btn-primary'
  return (
    <button className={`btn ${cls} ${size === 'sm' ? 'btn-sm' : ''}`} onClick={toggle} disabled={busy}>
      {busy ? 'Working…' : club.joined ? 'Leave club' : 'Join club'}
    </button>
  )
}

export default function ClubCard({ club, onChange }) {
  return (
    <article className="club-card" style={{ '--h': hueFor(club.name) }}>
      <div className="club-top">
        <Monogram name={club.name} />
        <div className="grow">
          <h3><Link to={`/clubs/${club.clubId}`}>{club.name}</Link></h3>
          <span className="cat">{club.category || 'Open to all'}</span>
        </div>
        {club.joined && <Badge tone="ok">Joined</Badge>}
        {club.status !== 'ACTIVE' && <Badge>Inactive</Badge>}
      </div>
      <p className="desc">{club.description}</p>
      <div className="club-stats">
        <span><b>{club.memberCount}</b> members</span>
        <span><b>{club.upcomingEvents}</b> upcoming</span>
      </div>
      <div className="club-foot">
        <div className="coord">
          Coordinator
          <b>{club.coordinatorName || 'Not assigned yet'}</b>
        </div>
        <div className="row">
          <Link to={`/clubs/${club.clubId}`} className="btn btn-ghost btn-sm">View</Link>
          <MembershipButton club={club} onChange={onChange} />
        </div>
      </div>
    </article>
  )
}
