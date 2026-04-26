# Backend

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

The backend loads tracked YAML config plus environment overrides.

```bash
cp backend/.env.example backend/.env
```

### Recommended local PostgreSQL setup

For local development, the simplest setup is to use the root-level `compose.yaml` Postgres
service. This avoids installing PostgreSQL directly on your machine and matches the
recommended local `DATABASE_*` values in `backend/.env`.

```env
GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-web-client-secret
JWT_SECRET=replace-me-with-a-long-random-secret
```

Notes:
- Defaults live in `backend/src/main/resources/application.yml` and `backend/src/main/resources/application-dev.yml`.
- On startup, the backend loads real process environment variables via Hoplite `addEnvironmentSource()`, then YAML config, then applies `.env` fallback.
- In production, values set in the server environment are picked up directly from process env.
- Locally, if those process env vars are missing, `.env` is a fallback for `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `DATABASE_JDBC_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`.
- Use nested config path names for overrides, for example `APP_PUBLIC_BACKEND_BASE_URL`, `APP_FRONTEND_BASE_URL`, and `JWT_TTL_SECONDS`.
- `app.publicBackendBaseUrl` is used for the Google OAuth callback URI, and `app.frontendBaseUrl` is used for the final browser redirect.
- `GET /api/v1/auth/google/redirect` handles both OAuth start and callback. The flow stores validated `frontendOrigin` and `returnPath` in temporary `HttpOnly` cookies and redirects back to the stored frontend URL after successful login.
- Successful login sets an `HttpOnly` auth cookie and refresh-token cookie. `GET /api/v1/auth/session`, `POST /api/v1/auth/refresh`, and `POST /api/v1/auth/logout` all work with the cookie-based session flow.
- `app.allowedOrigins` is used for the runtime CORS allowlist, and `app.frontendBaseUrl` is always included.
- If `app.authCookieSameSite=None`, `app.authCookieSecure` must be `true`. OAuth flow cookies use `app.oauthFlowCookieSameSite` (default `Lax`) so the Google callback can complete.
- `POST /api/v1/number/search` saves the user's searched number in PostgreSQL and reuses shared number results. `GET /api/v1/number/all` returns the authenticated user's searched numbers with all available results for that number, ordered newest first.

## Run

```bash
cp .env.example .env
cp backend/.env.example backend/.env
docker compose up -d postgres
cd backend && ./gradlew run
```

- Run `cp .env.example .env` from the repository root before using Compose; the root Compose file reads variables from the root `.env`.
- Run `docker compose up -d postgres` from the repository root.
- Run `./gradlew run` from `backend/`.
- The backend runs on your host machine; Docker is only used for the local PostgreSQL service.

### Full Docker Compose stack

You can also start the local database, backend, and frontend together from the repository root:

```bash
cp .env.example .env
docker compose up
```

This Compose setup reads all required variables from the repository-root `.env` file.
Start by copying `.env.example` to `.env`, then update values as needed for your machine.
Google login will not work until you replace the placeholder Google OAuth values with real credentials.

The checked-in Compose service starts PostgreSQL on `localhost:5432` with:

- database: `pna`
- user: `postgres`
- password: `postgres-secret`

If port `5432` is already in use on your machine, Docker will fail to start the local database
until that conflict is resolved.

The backend bootstraps its PostgreSQL schema on startup, so no manual migration step is required.

Useful local database commands:

```bash
docker compose ps
docker compose logs -f postgres
docker compose down
docker compose down -v
```

- `docker compose down` stops the database and keeps the local data volume.
- `docker compose down -v` also removes the local Postgres data volume and resets the DB.
- PostgreSQL data is stored in the named Compose volume `postgres_data`, which Docker creates
  with the project-prefixed name `pna_postgres_data` for this repo.
- Deleting containers alone, including from Docker Desktop, does not remove that volume, so the
  database data remains until you remove the volume explicitly.

Swagger UI will be available at `http://localhost:8080/swagger`.
