import { useEffect } from 'react'
import { hueFor, initials, monogram as mono } from '../utils'
import { XIcon, TicketIcon } from '../icons'

export function Avatar({ name, size, className = '' }) {
  const h = hueFor(name)
  return (
    <span
      className={`avatar ${size === 'lg' ? 'lg' : ''} ${className}`}
      style={{ background: `linear-gradient(145deg, hsl(${h} 70% 52%), hsl(${(h + 30) % 360} 65% 40%))` }}
    >
      {initials(name)}
    </span>
  )
}

export function Monogram({ name, size, className = '' }) {
  return (
    <span className={`monogram ${size === 'xl' ? 'xl' : ''} ${className}`} style={{ '--h': hueFor(name) }}>
      {mono(name)}
    </span>
  )
}

export function Badge({ tone, children, dot = true }) {
  return <span className={`badge ${tone ? `badge-${tone}` : ''} ${dot ? '' : 'no-dot'}`}>{children}</span>
}

export const eventStatusBadge = (status) => {
  if (status === 'UPCOMING') return <Badge tone="primary">Upcoming</Badge>
  if (status === 'CLOSED') return <Badge tone="gold">Registration closed</Badge>
  return <Badge>Completed</Badge>
}

export const attendanceBadge = (status) => {
  if (status === 'PRESENT') return <Badge tone="ok">Present</Badge>
  if (status === 'ABSENT') return <Badge tone="bad">Absent</Badge>
  return <Badge tone="primary">Registered</Badge>
}

export function Spinner() {
  return <span className="spinner" role="progressbar" aria-label="Loading" />
}

export function PageLoading() {
  return (
    <div className="page-loading">
      <Spinner />
    </div>
  )
}

export function Skeletons({ count = 4, height = 190, className = '' }) {
  return (
    <div className={className}>
      {Array.from({ length: count }).map((_, i) => (
        <div key={i} className="skeleton" style={{ height }} />
      ))}
    </div>
  )
}

export function Empty({ title, children, action, icon }) {
  return (
    <div className="empty">
      <div className="glyph">{icon || <TicketIcon width={56} height={56} strokeWidth={1.3} />}</div>
      <h3>{title}</h3>
      <p>{children}</p>
      {action}
    </div>
  )
}

export function Pager({ page, totalPages, onChange }) {
  if (totalPages <= 1) return null
  // Show a compact window of page numbers around the current page.
  const pages = []
  const start = Math.max(0, Math.min(page - 2, totalPages - 5))
  const end = Math.min(totalPages, start + 5)
  for (let i = start; i < end; i++) pages.push(i)
  return (
    <nav className="pager" aria-label="Pagination">
      <button onClick={() => onChange(page - 1)} disabled={page === 0}>Previous</button>
      {pages.map((p) => (
        <button key={p} aria-current={p === page ? 'page' : undefined} onClick={() => onChange(p)}>
          {p + 1}
        </button>
      ))}
      <button onClick={() => onChange(page + 1)} disabled={page >= totalPages - 1}>Next</button>
    </nav>
  )
}

export function Modal({ title, onClose, children }) {
  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose()
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [onClose])

  return (
    <div className="modal-back" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true" aria-label={title}>
        <div className="modal-head">
          <h3>{title}</h3>
          <button className="icon-btn" onClick={onClose} aria-label="Close">
            <XIcon />
          </button>
        </div>
        <div className="modal-body">{children}</div>
      </div>
    </div>
  )
}

export function Tabs({ tabs, value, onChange }) {
  return (
    <div className="tabs" role="tablist">
      {tabs.map((t) => (
        <button key={t.id} role="tab" aria-selected={value === t.id} onClick={() => onChange(t.id)}>
          {t.label}
          {t.count != null && <span className="count">{t.count}</span>}
        </button>
      ))}
    </div>
  )
}

export function Stars({ value = 0, onPick, readOnly }) {
  return (
    <span className="stars" aria-label={`${value} out of 5`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          className={n <= value ? 'on' : ''}
          disabled={readOnly}
          style={readOnly ? { cursor: 'default' } : undefined}
          onClick={() => onPick?.(n)}
          aria-label={`${n} star${n > 1 ? 's' : ''}`}
        >
          ★
        </button>
      ))}
    </span>
  )
}
