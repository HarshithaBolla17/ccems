# Deploy: GitHub -> Render (backend) + Vercel (frontend) + MySQL

1. Push this folder to a GitHub repo.
2. Create a free MySQL database (Aiven or TiDB Cloud). Note host, port, user, password, db name.
3. Render: New Web Service -> your repo -> Root Directory `backend` -> Runtime `Docker` -> Health check path `/actuator/health`.
   Env vars: SPRING_PROFILES_ACTIVE=prod, DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET, ADMIN_EMAIL, ADMIN_PASSWORD, SEED_DEMO_DATA, CORS_ORIGINS (your Vercel URL, no trailing slash).
4. Vercel: Add New Project -> same repo -> Root Directory `frontend` -> Framework Vite.
   Env var: VITE_API_URL = https://YOUR-RENDER-URL.onrender.com/api
5. Put the Vercel URL into Render's CORS_ORIGINS and redeploy the backend.
