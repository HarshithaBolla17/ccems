# Deploying CCEMS

## Docker (any VPS, one command)
1. Install Docker + Docker Compose on the server.
2. `cp .env.example .env` and fill in `DB_PASSWORD`, `DB_ROOT_PASSWORD`, `JWT_SECRET` (`openssl rand -base64 48`), `ADMIN_EMAIL`, `ADMIN_PASSWORD`.
3. `docker compose up -d --build`
4. Open `http://<server-ip>/` and sign in with the admin you set.

Nginx serves the React app and proxies `/api` to Spring Boot, so there is one origin and no CORS setup. Uploaded files live in the `uploads` volume and the database in `dbdata`, so both survive restarts and rebuilds.

The `prod` profile refuses to start with the built-in demo `JWT_SECRET`. Leave `SEED_DEMO_DATA=false` on a real server (set it to `true` only for a demo server).

## HTTPS
Put the app behind a TLS proxy. Easiest is Caddy on the host (`caddy reverse-proxy --from yourdomain.com --to localhost:80`), or a cloud load balancer / Cloudflare in front.

## Operations
- Logs: `docker compose logs -f backend`
- Health: `GET /actuator/health`
- Update: `git pull && docker compose up -d --build`
- Backup DB: `docker compose exec db sh -c 'mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" ccems' > backup.sql`
- Backup files: back up the `uploads` volume.

## Without Docker
Backend: `mvn clean package` then `java -jar target/*.jar` with the env vars from `.env.example` and `SPRING_PROFILES_ACTIVE=prod`. Frontend: `npm run build`, serve `dist/` with any static host and proxy `/api` to port 8080.

## CI
`.github/workflows/ci.yml` runs backend tests, builds the frontend and builds both Docker images on every push.

## GitHub + Render (backend) + Vercel (frontend)
See DEPLOY_RENDER_VERCEL.md.
