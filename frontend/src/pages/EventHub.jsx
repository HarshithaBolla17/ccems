import { useCallback, useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import api, { errMsg } from '../api'
import { useToast } from '../toast'
import ChatPanel from '../components/ChatPanel'
import { Badge, Empty, Modal, PageLoading, Tabs } from '../components/ui'
import { fmt, localInput, relative, saveBlob } from '../utils'

const size = (n) => (n > 1048576 ? `${(n / 1048576).toFixed(1)} MB` : `${Math.max(1, Math.round(n / 1024))} KB`)

export default function EventHub() {
  const { id } = useParams()
  const [sp, setSp] = useSearchParams()
  const toast = useToast()
  const [hub, setHub] = useState(null)
  const [err, setErr] = useState('')
  const [modal, setModal] = useState(null) // 'announce' | 'link' | 'file' | 'task'
  const tab = sp.get('tab') || 'updates'

  const load = useCallback(() => {
    api.get(`/events/${id}/hub`).then((r) => setHub(r.data)).catch((e) => setErr(errMsg(e)))
  }, [id])
  useEffect(() => { load() }, [load])

  const act = async (fn, ok) => {
    try { await fn(); if (ok) toast.success(ok); load() } catch (e) { toast.error(errMsg(e)) }
  }
  const download = async (url, name) => {
    try { saveBlob(await api.get(url, { responseType: 'blob' }), name) } catch (e) { toast.error('Could not download the file.') }
  }

  if (err) return <main className="page"><div className="container"><Empty title="Event hub unavailable" action={<Link to="/events" className="btn btn-primary">Back to events</Link>}>{err}</Empty></div></main>
  if (!hub) return <PageLoading />

  const { event: ev, canManage, announcements, resources, tasks } = hub
  const tabs = [
    { id: 'updates', label: 'Updates', count: announcements.length },
    { id: 'resources', label: 'Resources', count: resources.length },
    { id: 'tasks', label: 'Tasks', count: tasks.length },
    { id: 'chat', label: 'Chat' },
  ]

  return (
    <main className="page"><div className="container stack" style={{ gap: 20 }}>
      <header className="hub-head">
        <Link to="/events" className="muted small">← All events</Link>
        <h1>{ev.title}</h1>
        <p className="muted">{ev.clubName} · {fmt.long(ev.eventDate)}{ev.venue ? ` · ${ev.venue}` : ''}</p>
      </header>

      <Tabs tabs={tabs} value={tab} onChange={(t) => setSp({ tab: t }, { replace: true })} />

      {tab === 'updates' && (
        <section className="stack">
          {canManage && <div><button className="btn btn-primary btn-sm" onClick={() => setModal('announce')}>Post announcement</button></div>}
          {announcements.length === 0 && <Empty title="No announcements yet">Updates from the organisers will show up here.</Empty>}
          {announcements.map((a) => (
            <article key={a.announcementId} className={`note ${a.urgent ? 'urgent' : ''}`}>
              <div className="row wrap">
                {a.pinned && <Badge>Pinned</Badge>}
                {a.urgent && <Badge tone="bad">Important</Badge>}
                <b>{a.title}</b>
              </div>
              {a.body && <p>{a.body}</p>}
              <small className="muted">{a.authorName} · {relative(a.createdAt)}</small>
              {canManage && (
                <div className="row">
                  <button className="link-btn" onClick={() => act(() => api.put(`/announcements/${a.announcementId}/pin`), a.pinned ? 'Unpinned.' : 'Pinned.')}>{a.pinned ? 'Unpin' : 'Pin'}</button>
                  <button className="link-btn" onClick={() => act(() => api.delete(`/announcements/${a.announcementId}`), 'Deleted.')}>Delete</button>
                </div>
              )}
            </article>
          ))}
        </section>
      )}

      {tab === 'resources' && (
        <section className="stack">
          {canManage && (
            <div className="row wrap">
              <button className="btn btn-primary btn-sm" onClick={() => setModal('link')}>Add link</button>
              <button className="btn btn-soft btn-sm" onClick={() => setModal('file')}>Upload file</button>
            </div>
          )}
          {resources.length === 0 && <Empty title="No resources yet">Links, slides and PDFs shared by the organisers appear here.</Empty>}
          {resources.map((r) => (
            <div key={r.resourceId} className="res">
              <div>
                <b>{r.title}</b>
                {r.description && <span>{r.description}</span>}
                <small className="muted">{r.type === 'FILE' ? `${r.fileName} · ${size(r.fileSize)}` : r.url} · {r.uploadedBy}</small>
              </div>
              <div className="row">
                {r.type === 'LINK'
                  ? <a className="btn btn-soft btn-sm" href={r.url} target="_blank" rel="noopener noreferrer">Open</a>
                  : <button className="btn btn-soft btn-sm" onClick={() => download(`/resources/${r.resourceId}/download`, r.fileName)}>Download</button>}
                {canManage && <button className="link-btn" onClick={() => act(() => api.delete(`/resources/${r.resourceId}`), 'Removed.')}>Remove</button>}
              </div>
            </div>
          ))}
        </section>
      )}

      {tab === 'tasks' && (
        <section className="stack">
          {canManage && <div><button className="btn btn-primary btn-sm" onClick={() => setModal('task')}>New submission task</button></div>}
          {tasks.length === 0 && <Empty title="No tasks">Submission tasks with deadlines show up here.</Empty>}
          {tasks.map((t) => (
            <TaskCard key={t.taskId} t={t} canManage={canManage} onDone={load} download={download}
              onDelete={() => act(() => api.delete(`/tasks/${t.taskId}`), 'Task deleted.')} />
          ))}
        </section>
      )}

      {tab === 'chat' && <ChatPanel scope="event" id={id} />}

      {modal === 'announce' && <AnnounceForm id={id} onClose={() => setModal(null)} onDone={load} />}
      {modal === 'link' && <LinkForm id={id} onClose={() => setModal(null)} onDone={load} />}
      {modal === 'file' && <FileForm id={id} onClose={() => setModal(null)} onDone={load} />}
      {modal === 'task' && <TaskForm id={id} onClose={() => setModal(null)} onDone={load} />}
    </div></main>
  )
}

function TaskCard({ t, canManage, onDone, onDelete, download }) {
  const toast = useToast()
  const [open, setOpen] = useState(false)
  const [subs, setSubs] = useState(null)
  const [f, setF] = useState({ link: '', note: '', file: null })
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    const fd = new FormData()
    if (f.link) fd.append('link', f.link)
    if (f.note) fd.append('note', f.note)
    if (f.file) fd.append('file', f.file)
    setBusy(true)
    try { await api.post(`/tasks/${t.taskId}/submit`, fd); toast.success('Submitted.'); setOpen(false); onDone() }
    catch (err) { toast.error(errMsg(err)) } finally { setBusy(false) }
  }
  const viewSubs = async () => {
    try { setSubs((await api.get(`/tasks/${t.taskId}/submissions`)).data) } catch (e) { toast.error(errMsg(e)) }
  }

  return (
    <article className="note">
      <div className="row wrap"><b>{t.title}</b><Badge tone={t.open ? undefined : 'bad'}>{t.open ? `Due ${relative(t.deadline)}` : 'Closed'}</Badge></div>
      {t.description && <p>{t.description}</p>}
      <small className="muted">Deadline {fmt.long(t.deadline)}{canManage ? ` · ${t.submittedCount} of ${t.expectedCount} submitted` : ''}</small>

      {!canManage && t.mine && (
        <div className="submitted">✓ Submitted {relative(t.mine.submittedAt)}{t.mine.fileName ? ` · ${t.mine.fileName}` : ''}{t.mine.linkUrl ? ` · ${t.mine.linkUrl}` : ''}</div>
      )}
      {!canManage && t.open && <div><button className="btn btn-primary btn-sm" onClick={() => setOpen(true)}>{t.mine ? 'Resubmit' : 'Submit work'}</button></div>}
      {canManage && (
        <div className="row">
          <button className="link-btn" onClick={viewSubs}>View submissions</button>
          <button className="link-btn" onClick={onDelete}>Delete task</button>
        </div>
      )}
      {subs && (
        <div className="subs">
          {subs.length === 0 && <span className="muted">Nothing submitted yet.</span>}
          {subs.map((s) => (
            <div key={s.submissionId} className="row wrap">
              <b>{s.studentName}</b><span className="muted">{s.rollNo}</span>
              {s.linkUrl && <a href={s.linkUrl} target="_blank" rel="noopener noreferrer">Link</a>}
              {s.fileName && <button className="link-btn" onClick={() => download(`/submissions/${s.submissionId}/download`, s.fileName)}>{s.fileName}</button>}
              {s.note && <span className="muted">“{s.note}”</span>}
            </div>
          ))}
        </div>
      )}
      {open && (
        <Modal title={`Submit: ${t.title}`} onClose={() => setOpen(false)}>
          <form className="stack" onSubmit={submit}>
            <div className="field"><label>Link (optional)</label><input className="input" value={f.link} onChange={(e) => setF({ ...f, link: e.target.value })} placeholder="https://" /></div>
            <div className="field"><label>File (optional, max 10 MB)</label><input type="file" onChange={(e) => setF({ ...f, file: e.target.files[0] })} /></div>
            <div className="field"><label>Note (optional)</label><textarea className="textarea" rows={3} value={f.note} onChange={(e) => setF({ ...f, note: e.target.value })} /></div>
            <button className="btn btn-primary" disabled={busy}>{busy ? 'Submitting…' : 'Submit'}</button>
          </form>
        </Modal>
      )}
    </article>
  )
}

function useForm(onClose, onDone, okMsg) {
  const toast = useToast()
  const [busy, setBusy] = useState(false)
  const run = async (fn) => {
    setBusy(true)
    try { await fn(); toast.success(okMsg); onClose(); onDone() } catch (e) { toast.error(errMsg(e)) } finally { setBusy(false) }
  }
  return [busy, run]
}

function AnnounceForm({ id, onClose, onDone }) {
  const [f, setF] = useState({ title: '', body: '', pinned: false, urgent: false })
  const [busy, run] = useForm(onClose, onDone, 'Announcement posted. Participants were notified.')
  return (
    <Modal title="Post announcement" onClose={onClose}>
      <form className="stack" onSubmit={(e) => { e.preventDefault(); run(() => api.post(`/events/${id}/announcements`, f)) }}>
        <div className="field"><label>Title</label><input className="input" required value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} placeholder="Venue changed to Hall 2" /></div>
        <div className="field"><label>Details (optional)</label><textarea className="textarea" rows={4} value={f.body} onChange={(e) => setF({ ...f, body: e.target.value })} /></div>
        <label className="check"><input type="checkbox" checked={f.urgent} onChange={(e) => setF({ ...f, urgent: e.target.checked })} /> Mark as important</label>
        <label className="check"><input type="checkbox" checked={f.pinned} onChange={(e) => setF({ ...f, pinned: e.target.checked })} /> Pin to top</label>
        <button className="btn btn-primary" disabled={busy}>Post</button>
      </form>
    </Modal>
  )
}

function LinkForm({ id, onClose, onDone }) {
  const [f, setF] = useState({ title: '', url: '', description: '' })
  const [busy, run] = useForm(onClose, onDone, 'Link added.')
  return (
    <Modal title="Add a link" onClose={onClose}>
      <form className="stack" onSubmit={(e) => { e.preventDefault(); run(() => api.post(`/events/${id}/resources/link`, f)) }}>
        <div className="field"><label>Title</label><input className="input" required value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} /></div>
        <div className="field"><label>URL</label><input className="input" required value={f.url} onChange={(e) => setF({ ...f, url: e.target.value })} placeholder="https://" /></div>
        <div className="field"><label>Description (optional)</label><input className="input" value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></div>
        <button className="btn btn-primary" disabled={busy}>Add</button>
      </form>
    </Modal>
  )
}

function FileForm({ id, onClose, onDone }) {
  const [f, setF] = useState({ title: '', description: '', file: null })
  const [busy, run] = useForm(onClose, onDone, 'File uploaded.')
  const submit = (e) => {
    e.preventDefault()
    const fd = new FormData()
    fd.append('title', f.title)
    if (f.description) fd.append('description', f.description)
    fd.append('file', f.file)
    run(() => api.post(`/events/${id}/resources/file`, fd))
  }
  return (
    <Modal title="Upload a file" onClose={onClose}>
      <form className="stack" onSubmit={submit}>
        <div className="field"><label>Title</label><input className="input" required value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} /></div>
        <div className="field"><label>File (PDF, slides, docs, images, zip · max 10 MB)</label><input type="file" required onChange={(e) => setF({ ...f, file: e.target.files[0] })} /></div>
        <div className="field"><label>Description (optional)</label><input className="input" value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></div>
        <button className="btn btn-primary" disabled={busy || !f.file}>Upload</button>
      </form>
    </Modal>
  )
}

function TaskForm({ id, onClose, onDone }) {
  const [f, setF] = useState({ title: '', description: '', deadline: localInput(48) })
  const [busy, run] = useForm(onClose, onDone, 'Task created. Participants were notified.')
  return (
    <Modal title="New submission task" onClose={onClose}>
      <form className="stack" onSubmit={(e) => { e.preventDefault(); run(() => api.post(`/events/${id}/tasks`, f)) }}>
        <div className="field"><label>Title</label><input className="input" required value={f.title} onChange={(e) => setF({ ...f, title: e.target.value })} placeholder="Submit your project report" /></div>
        <div className="field"><label>Instructions (optional)</label><textarea className="textarea" rows={3} value={f.description} onChange={(e) => setF({ ...f, description: e.target.value })} /></div>
        <div className="field"><label>Deadline</label><input className="input" type="datetime-local" required value={f.deadline} onChange={(e) => setF({ ...f, deadline: e.target.value })} /></div>
        <button className="btn btn-primary" disabled={busy}>Create task</button>
      </form>
    </Modal>
  )
}
