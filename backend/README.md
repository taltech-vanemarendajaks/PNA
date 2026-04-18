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

Create `backend/.env` (you can copy `backend/.env.example`) and set values there.

```env
APP_HOST=0.0.0.0
APP_PORT=8080
GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-web-client-secret
PUBLIC_BACKEND_BASE_URL=http://localhost:8080
FRONTEND_BASE_URL=http://localhost:5173
CORS_ALLOW_ANY_HOST=false
CORS_ALLOWED_ORIGINS=http://localhost:5173
JWT_SECRET=replace-me-with-a-long-random-secret
JWT_ISSUER=http://localhost:8080
JWT_AUDIENCE=pna-clients
JWT_TTL_SECONDS=900
REFRESH_TOKEN_TTL_SECONDS=2592000
AUTH_COOKIE_SECURE=false
AUTH_COOKIE_SAME_SITE=Lax
NUMBER_SEARCH_DB_PATH=number-searches.db
```

Notes:
- Environment variables override `.env` values.
- Cookie-backed refresh/logout endpoints require explicit origins in `CORS_ALLOWED_ORIGINS`; `CORS_ALLOW_ANY_HOST=true` is not supported.
- `PUBLIC_BACKEND_BASE_URL` is the public backend origin used for the Google OAuth callback URI. Set this to the externally reachable backend URL in proxied deployments.
- `FRONTEND_BASE_URL` is used by the backend-owned Google redirect flow to send the browser back to the frontend page.
- `GET /api/v1/auth/google/redirect` starts the Google OAuth authorization-code flow and also handles the callback from Google on the same route.
- `GOOGLE_CLIENT_SECRET` is required for the backend to exchange the Google authorization code for tokens.
- After Google login succeeds, the backend issues a short-lived app access JWT in an `HttpOnly` auth cookie and also sets an `HttpOnly` refresh-token cookie.
- `JWT_TTL_SECONDS` controls the access JWT lifetime.
- `REFRESH_TOKEN_TTL_SECONDS` controls the refresh-token cookie/session lifetime.
- `GET /api/v1/auth/session` returns the authenticated user for a valid auth cookie or bearer access JWT.
- `POST /api/v1/auth/refresh` rotates the refresh token cookie and reissues a fresh auth access cookie.
- `POST /api/v1/auth/logout` revokes the current refresh-token session and clears both the refresh cookie and the auth cookie.
- Refresh-token storage keeps only the fields needed for family revocation, refresh replay detection, and the current session payload; older rows are migrated automatically on startup.
- `AUTH_COOKIE_SAME_SITE=Strict` is allowed for the auth and refresh cookies, but the temporary OAuth state/redirect-context cookies always use `Lax` so the Google callback can complete.
- `AUTH_COOKIE_SAME_SITE=None` requires `AUTH_COOKIE_SECURE=true`.
- `POST /api/v1/number/search` checks SQLite first; if the number already exists, the stored lookup result is returned. If not, a new lookup is performed and saved. Requests may authenticate with the auth cookie or a bearer token header.
- `GET /api/v1/number/all` returns all saved number searches and accepts the auth cookie or a bearer token header.
- `NUMBER_SEARCH_DB_PATH` controls where the SQLite file is stored (relative or absolute path).

## Run

```bash
./gradlew run
```

Swagger UI will be available at `http://localhost:8080/swagger`.
