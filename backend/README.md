# Backend

Ktor backend API for authentication and phone-number lookup.

## Endpoints

- `GET /health`
- `GET /api/v1/auth/session`
- `GET /api/v1/auth/google/redirect`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/number/search`
- `GET /api/v1/number/all`
- `GET /openapi`
- `GET /swagger`

## Configuration

Copy the local backend env file:

```bash
cp backend/.env.example backend/.env
```

`backend/.env` exists so the backend can be run on its own with `cd backend && ./gradlew run` without depending on the repository-root `.env`. The root `.env` is mainly for full-stack Docker Compose usage.

Required values:

```env
GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-web-client-secret
JWT_SECRET=replace-me-with-a-long-random-secret
```

Important notes:
- Defaults live in `src/main/resources/application.yml` and `src/main/resources/application-dev.yml`.
- Config comes from YAML, environment variables, then `.env` fallback.
- Useful override names include `APP_PUBLIC_BACKEND_BASE_URL`, `APP_FRONTEND_BASE_URL`, and `JWT_TTL_SECONDS`.
- `GET /api/v1/auth/google/redirect` handles both Google OAuth start and callback.
- Login sets `HttpOnly` auth and refresh-token cookies; `/session`, `/refresh`, and `/logout` use the cookie-based session flow.
- `app.allowedOrigins` and `app.frontendBaseUrl` control runtime CORS.
- If `app.authCookieSameSite=None`, `app.authCookieSecure` must be `true`.
- `POST /api/v1/number/search` saves the user's searched number and reuses shared number results. `GET /api/v1/number/all` returns that user's saved searches, newest first.

## Run

For full-stack Docker Compose startup, see the repository root `README.md`.

```bash
cp backend/.env.example backend/.env
docker compose up -d postgres
cd backend && ./gradlew run
```

The backend runs on your host machine. Docker is only used here for local PostgreSQL.

Local PostgreSQL defaults:

- database: `pna`
- user: `postgres`
- password: `postgres-secret`

The backend bootstraps its PostgreSQL schema on startup, so no manual migration step is required.

Useful local database commands:

```bash
docker compose ps
docker compose logs -f postgres
docker compose down
docker compose down -v
```

- `docker compose down` keeps the local data volume.
- `docker compose down -v` removes the local Postgres data volume and resets the DB.

Swagger UI will be available at `http://localhost:8080/swagger`.
