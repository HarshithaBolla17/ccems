import { Link } from 'react-router-dom'
import { useAuth } from '../auth'
import { TicketShell } from '../components/EventTicket'
import { ClockIcon, PinIcon } from '../icons'
import { homePath } from '../utils'

const inDays = (n, hour) => {
  const d = new Date()
  d.setDate(d.getDate() + n)
  d.setHours(hour, 0, 0, 0)
  return d
}

function SampleTicket({ hue, date, club, title, venue, left, pct, className }) {
  return (
    <div className={className}>
      <TicketShell hue={hue} date={date}>
        <span className="ticket-club"><i style={{ background: `hsl(${hue} 72% 52%)` }} />{club}</span>
        <h3>{title}</h3>
        <div className="ticket-meta">
          <span><PinIcon width={15} height={15} />{venue}</span>
          <span><ClockIcon width={15} height={15} />Registration open</span>
        </div>
        <div className="ticket-foot">
          <div className="meter-wrap">
            <div className="meter" style={{ '--h': hue }}><span style={{ width: `${pct}%` }} /></div>
            <div className="meter-label">{left} seats left</div>
          </div>
        </div>
      </TicketShell>
    </div>
  )
}

export default function Landing() {
  const { user } = useAuth()

  return (
    <>
      <header className="hero">
        <div className="container hero-grid">
          <div>
            <h1>Join the club. Take the seat. Keep the certificate.</h1>
            <p className="lede">
              CCEMS puts every club, event sign-up, attendance sheet and certificate on one campus platform,
              so none of it lives in a WhatsApp group again.
            </p>
            <div className="cta">
              {user ? (
                <Link to={homePath(user.role)} className="btn btn-primary btn-lg">Open your dashboard</Link>
              ) : (
                <>
                  <Link to="/register" className="btn btn-primary btn-lg">Create your account</Link>
                  <Link to="/clubs" className="btn btn-ghost btn-lg">Browse clubs</Link>
                </>
              )}
            </div>
            <p className="hero-note">Seats and deadlines are enforced by the server. When it says full, it is full.</p>
          </div>

          <div className="hero-stack" aria-hidden="true">
            <SampleTicket className="t1" hue={252} date={inDays(9, 9)} club="Codestorm" title="Hack the Campus 2026" venue="Innovation Hub" left={31} pct={61} />
            <SampleTicket className="t2" hue={332} date={inDays(4, 17)} club="Sargam Music Club" title="Open Mic Evening" venue="Student Plaza" left={9} pct={82} />
            <SampleTicket className="t3" hue={28} date={inDays(6, 16)} club="Lens & Light" title="Golden Hour Photo Walk" venue="Main Gate" left={17} pct={43} />
          </div>
        </div>
      </header>

      <section className="section tint">
        <div className="container">
          <h2>From a sign-up to a certificate in three steps</h2>
          <p className="sub">The same record carries a student from the first click to the last page of their portfolio.</p>
          <div className="steps">
            <div className="step">
              <div className="n">1</div>
              <h3>Join the clubs you care about</h3>
              <p>Browse every active club in one directory and join with a click. Leave whenever you like.</p>
            </div>
            <div className="step">
              <div className="n">2</div>
              <h3>Register before the seats go</h3>
              <p>Each event shows live capacity and its deadline. Once either runs out, registration stops.</p>
            </div>
            <div className="step">
              <div className="n">3</div>
              <h3>Show up and get certified</h3>
              <p>Coordinators mark attendance after the event, and a certificate is issued for every student marked present.</p>
            </div>
          </div>
        </div>
      </section>

      <section className="section">
        <div className="container">
          <h2>One platform, three views of the same campus</h2>
          <dl className="roles">
            <dt>Students</dt>
            <dd>See what is on, register in seconds and collect certificates in a profile that keeps your clubs, event history and ratings together.</dd>
            <dt>Coordinators</dt>
            <dd>Publish events, watch registrations arrive, mark attendance from a single sheet and export the list when you need it elsewhere.</dd>
            <dt>Admins</dt>
            <dd>Create clubs, assign coordinators and see turnout across every department without asking anyone for a spreadsheet.</dd>
          </dl>
        </div>
      </section>

      <section className="section" style={{ paddingTop: 0 }}>
        <div className="container">
          <div className="cta-band">
            <h2>Your next event is already listed.</h2>
            <Link to={user ? '/events' : '/register'} className="btn btn-lg btn-light">
              {user ? 'See upcoming events' : 'Get started'}
            </Link>
          </div>
        </div>
      </section>

      <footer className="footer">
        <div className="container row">
          <span>CCEMS, the Campus Club &amp; Event Management System</span>
          <span>React · Spring Boot · MySQL · JWT</span>
        </div>
      </footer>
    </>
  )
}
