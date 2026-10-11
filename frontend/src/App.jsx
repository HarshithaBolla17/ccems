import { Link, Navigate, Outlet, Route, Routes, useLocation } from 'react-router-dom'
import { useAuth } from './auth'
import Navbar from './components/Navbar'
import { Empty, PageLoading } from './components/ui'
import { homePath } from './utils'
import Landing from './pages/Landing'
import Login from './pages/Login'
import Register from './pages/Register'
import Clubs from './pages/Clubs'
import ClubDetail from './pages/ClubDetail'
import Events from './pages/Events'
import Profile from './pages/Profile'
import CertificatePage from './pages/CertificatePage'
import Coordinator from './pages/Coordinator'
import Admin from './pages/Admin'
import VerifyPage from './pages/VerifyPage'
import Home from './pages/Home'
import EventHub from './pages/EventHub'
import Chats from './pages/Chats'
import ReceiptPage from './pages/ReceiptPage'

/** Route guard: signed-in users only, optionally limited to some roles. */
function Protected({ roles }) {
  const { user, loading } = useAuth()
  const loc = useLocation()
  if (loading) return <PageLoading />
  if (!user) return <Navigate to="/login" replace state={{ from: loc.pathname }} />
  if (roles && !roles.includes(user.role)) return <Navigate to={homePath(user.role)} replace />
  return <Outlet />
}

function NotFound() {
  return (
    <main className="page"><div className="container">
      <Empty title="This page doesn't exist" action={<Link to="/" className="btn btn-primary">Back to home</Link>}>
        The link may be old, or the page was moved.
      </Empty>
    </div></main>
  )
}

export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/verify" element={<VerifyPage />} />
        <Route path="/verify/:code" element={<VerifyPage />} />

        <Route element={<Protected />}>
          <Route path="/home" element={<Home />} />
          <Route path="/events/:id/hub" element={<EventHub />} />
          <Route path="/chats" element={<Chats />} />
          <Route path="/chats/:scope/:id" element={<Chats />} />
          <Route path="/clubs" element={<Clubs />} />
          <Route path="/clubs/:id" element={<ClubDetail />} />
          <Route path="/events" element={<Events />} />
          <Route path="/certificate/:id" element={<CertificatePage />} />
        </Route>

        <Route element={<Protected roles={['STUDENT']} />}>
          <Route path="/profile" element={<Profile />} />
          <Route path="/receipt/:eventId" element={<ReceiptPage />} />
        </Route>
        <Route element={<Protected roles={['COORDINATOR', 'ADMIN']} />}>
          <Route path="/coordinator" element={<Coordinator />} />
        </Route>
        <Route element={<Protected roles={['ADMIN']} />}>
          <Route path="/admin" element={<Admin />} />
        </Route>

        <Route path="*" element={<NotFound />} />
      </Routes>
    </>
  )
}
