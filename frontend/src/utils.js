// Deterministic hue (0-359) so every club keeps the same colour everywhere.
export function hueFor(text = '') {
  let h = 7
  for (const ch of text) h = (h * 31 + ch.charCodeAt(0)) % 360
  return h
}

export function initials(name = '') {
  return name.split(/\s+/).filter(Boolean).slice(0, 2).map((w) => w[0]).join('').toUpperCase()
}

export function monogram(name = '') {
  const words = name.split(/\s+/).filter((w) => /[A-Za-z]/.test(w))
  if (words.length === 1) return words[0].slice(0, 2)
  return words.slice(0, 2).map((w) => w[0]).join('')
}

const toDate = (v) => (v instanceof Date ? v : new Date(v))

export const fmt = {
  mon: (v) => toDate(v).toLocaleString('en-IN', { month: 'short' }),
  day: (v) => toDate(v).getDate(),
  dow: (v) => toDate(v).toLocaleString('en-IN', { weekday: 'short' }),
  time: (v) => toDate(v).toLocaleString('en-IN', { hour: 'numeric', minute: '2-digit', hour12: true }),
  date: (v) => toDate(v).toLocaleString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }),
  long: (v) => toDate(v).toLocaleString('en-IN', { weekday: 'short', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit', hour12: true }),
  full: (v) => toDate(v).toLocaleString('en-IN', { day: 'numeric', month: 'long', year: 'numeric' }),
}

// "in 3 days", "in 5 hours", "2 days ago"
export function relative(v) {
  const diff = toDate(v).getTime() - Date.now()
  const abs = Math.abs(diff)
  const mins = Math.round(abs / 60000)
  const hours = Math.round(abs / 3600000)
  const days = Math.round(abs / 86400000)
  let s
  if (mins < 60) s = `${Math.max(mins, 1)} min`
  else if (hours < 24) s = `${hours} hour${hours > 1 ? 's' : ''}`
  else s = `${days} day${days > 1 ? 's' : ''}`
  return diff >= 0 ? `in ${s}` : `${s} ago`
}

// <input type="datetime-local"> value for "now + offset hours"
export function localInput(offsetHours = 0) {
  const d = new Date(Date.now() + offsetHours * 3600000)
  d.setMinutes(0, 0, 0)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

export const money = (n) => (n > 0 ? `₹${Number(n).toLocaleString('en-IN')}` : 'Free')

export function homePath() {
  return '/home'
}

export const roleLabel = { STUDENT: 'Student', COORDINATOR: 'Coordinator', ADMIN: 'Admin' }

export function downloadCsv(filename, rows) {
  const esc = (v) => `"${String(v ?? '').replace(/"/g, '""')}"`
  const csv = rows.map((r) => r.map(esc).join(',')).join('\n')
  const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8;' }))
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

export async function saveBlob(res, fallback) {
  const cd = res.headers['content-disposition'] || ''
  const m = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(cd)
  const url = URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = m ? decodeURIComponent(m[1]) : fallback
  a.click()
  URL.revokeObjectURL(url)
}
