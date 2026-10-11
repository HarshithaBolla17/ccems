# CCEMS: Campus Club & Event Management System

Join clubs, register for events, get attendance marked and receive certificates automatically.

**Stack:** React.js (Vite) · Spring Boot 3 (Web, Data JPA, Security) · MySQL · stateless JWT + BCrypt

```
ccems/
├── backend/    Spring Boot REST API
└── frontend/   React (Vite) single-page app
```

## Run it

### 1. Backend (port 8080)

You need Java 17+ and Maven.

**With MySQL (the real setup)**

1. Start MySQL. The database `ccems` is created automatically.
2. Set your credentials in `backend/src/main/resources/application.properties`, or pass `DB_USER` and `DB_PASSWORD` as environment variables (defaults: `root` / `root`).
3. Run:

```bash
cd backend
mvn spring-boot:run
```

**Without MySQL (quick demo, in-memory H2)**

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=h2
```

On first start, the database is filled with demo clubs, events, registrations and certificates. Turn this off with `ccems.seed.enabled=false`.

### 2. Frontend (port 5173)

You need Node 18+.

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. The dev server proxies `/api` to the backend, so there is no CORS setup to do.

## Demo accounts

| Role | Email | Password |
|------|-------|----------|
| Student | student@ccems.edu | Student@123 |
| Coordinator (Codestorm, Robotics Guild) | coordinator@ccems.edu | Coord@123 |
| Coordinator (Drama, Music) | coordinator2@ccems.edu | Coord@123 |
| Admin | admin@ccems.edu | Admin@123 |

The sign-in page has one-click buttons for the student, coordinator and admin accounts.

## What to try

- **Student:** join a club, register for an event, cancel, rate an attended event, open a certificate and print it.
- **Student, rules in action:** "Drone Basics Bootcamp" is full and "Git & GitHub in an Afternoon" has a passed deadline. Both are refused by the server.
- **Coordinator:** create an event, close registration, open the Attendance sheet for a past event, mark students present and save. Certificates are issued on save.
- **Admin:** create a club, assign a coordinator, deactivate a club, read the campus-wide activity chart.

## How the spec maps to the code

| Spec | Where |
|------|-------|
| JWT with role claim, BCrypt | `security/JwtUtil`, `SecurityConfig`, `AuthService` |
| `@PreAuthorize` on role endpoints | `controller/*` |
| Capacity + deadline enforcement | `EventService.register` (row-locked, so two clicks can't take the last seat) |
| Attendance triggers certificate | `EventService.markAttendance` → `CertificateService.issueIfMissing` |
| DTO-only responses | `dto/Dtos`, `service/DtoMapper` |
| Global error handling | `exception/GlobalExceptionHandler` (`@RestControllerAdvice`) |
| Axios interceptor + role-guarded routes | `frontend/src/api.js`, `App.jsx` |

### API

| Method & path | Role | Purpose |
|---|---|---|
| POST `/api/auth/register` | Public | Student/coordinator signup |
| POST `/api/auth/login` | Public | Login, returns JWT |
| GET `/api/me` | Any | Current user |
| GET `/api/clubs?page=0&size=10&q=` | Any | Paginated club directory |
| POST `/api/clubs` | ADMIN | Create a club |
| GET `/api/clubs/managed` | COORDINATOR, ADMIN | Clubs the caller can manage |
| PUT `/api/clubs/{id}/coordinator` | ADMIN | Assign a coordinator |
| PUT `/api/clubs/{id}/status` | ADMIN | Activate or deactivate |
| POST `/api/clubs/{id}/join` · `/leave` | STUDENT | Membership |
| GET `/api/clubs/{id}/events` | Any | A club's events (paginated, sortable) |
| POST `/api/clubs/{id}/events` | COORDINATOR, ADMIN | Create an event |
| GET `/api/events` | Any | Event feed: `q`, `status`, `openOnly`, `sortBy`, `dir`, paging |
| POST / DELETE `/api/events/{id}/register` | STUDENT | Register / cancel |
| PUT `/api/events/{id}/attendance` | COORDINATOR, ADMIN | Mark attendance, issues certificates |
| PUT `/api/events/{id}/status` | COORDINATOR, ADMIN | Close, reopen or complete |
| PUT `/api/events/{id}/feedback` | STUDENT | 1 to 5 rating after attending |
| GET `/api/events/{id}/registrations` | COORDINATOR, ADMIN | Registrants |
| GET `/api/students/{id}/certificates` | STUDENT, ADMIN | Earned certificates |
| GET `/api/students/{id}/profile` | STUDENT, ADMIN | Clubs, history, certificates |
| GET `/api/certificates/{id}` | Owner, club coordinator, ADMIN | One certificate |
| GET `/api/admin/stats` | ADMIN | Campus-wide overview |

## Paid events

- A coordinator sets a ticket price (₹0 = free) when creating an event.
- A student registering for a paid event gets a **10-minute seat hold**. The hold counts toward capacity, then the seat is released automatically if unpaid (checked every minute and before each registration).
- Paying records a `payments` row and confirms the registration. The student gets a printable receipt at `/receipt/:eventId`.
- Cancelling a paid registration **before the registration deadline** refunds it. After the deadline, cancelling is blocked.
- Unpaid students can't be marked present, so they never receive a certificate.
- Admins see total ticket revenue and revenue per club. Coordinators see collected amounts per event, and the registrations CSV includes payment columns.
- The gateway is a **demo**: `PaymentService.pay()` records a successful payment and moves no money. To use Razorpay, create the order before `pay()`, verify its signature inside `pay()`, and store the Razorpay payment id in `Payment.reference`.

## Certificate verification

Every certificate has a code like `CCEMS-2026-00001`. Anyone can check it at `/verify` (no account needed), which calls the public endpoint `GET /api/verify/{code}`. The certificate page has a "Copy verification link" button.

| Method & path | Role | Purpose |
|---|---|---|
| POST `/api/events/{id}/pay` | STUDENT | Pay for a held seat (`method`: UPI, CARD or NETBANKING) |
| GET `/api/events/{id}/receipt` | STUDENT | The student's receipt for an event |
| GET `/api/verify/{code}` | Public | Check a certificate code |

## Small additions beyond the problem statement

- `clubs.category`, `events.description` and `events.venue` columns, so the UI has something meaningful to show.
- `student_profiles.year` is stored in the column `year_of_study` (`year` is a reserved word in some databases).
- A student must be a club member to register for its events. The UI offers "Join club & register" in one click.
- Once someone is marked present and has a certificate, they can't be switched to absent.

## Production notes

- Set `JWT_SECRET` to a long random value.
- Set `CORS_ORIGINS` to your frontend's URL, and `VITE_API_URL` when building the frontend (`npm run build`).
- Change `spring.jpa.hibernate.ddl-auto` from `update` to `validate` once your schema is stable.


## New in this version
- **Home page after sign-in**, tailored to Student / Coordinator / Admin, with a "Needs your attention" list.
- **Event Hub** (`/events/:id/hub`): announcements (pin / important), resource links and file uploads, submission tasks with deadlines, and an event chat.
- **Chats** (`/chats`): event and club group chats with unread counts; organisers can delete messages and mute people.
- **Notification bell** for announcements, new tasks, payments and certificates.
- **Deployment**: `docker-compose.yml`, Dockerfiles, nginx, `.env.example`, GitHub Actions CI. See `DEPLOYMENT.md`.
- Tests: `cd backend && mvn test`.
