# Backend

- `GET /health`
- `GET /api/v1/auth/session`
- `GET /api/v1/auth/google/redirect`
- `POST /api/v1/auth/logout`
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
AUTH_SESSION_TTL_SECONDS=3600
AUTH_COOKIE_SECURE=false
AUTH_COOKIE_SAME_SITE=Lax
```

Notes:
- Environment variables override `.env` values.
- Cookie auth requires explicit origins in `CORS_ALLOWED_ORIGINS`; `CORS_ALLOW_ANY_HOST=true` is not supported.
- `PUBLIC_BACKEND_BASE_URL` is the public backend origin used for the Google OAuth callback URI. Set this to the externally reachable backend URL in proxied deployments.
- `FRONTEND_BASE_URL` is used by the backend-owned Google redirect flow to send the browser back to the frontend page.
- `GET /api/v1/auth/google/redirect` now starts the Google OAuth authorization-code flow and also handles the callback from Google on the same route.
- `GOOGLE_CLIENT_SECRET` is required for the backend to exchange the Google authorization code for tokens.
- `GET /api/v1/auth/session` returns the authenticated user from the backend session cookie.
- `POST /api/v1/auth/logout` clears the backend session cookie.
- `AUTH_COOKIE_SAME_SITE=Strict` is allowed for the long-lived auth session cookie, but the temporary OAuth state/redirect-context cookies always use `Lax` so the Google callback can complete.
- `AUTH_COOKIE_SAME_SITE=None` requires `AUTH_COOKIE_SECURE=true`.

## Run

```bash
./gradlew run
```

Swagger UI will be available at `http://localhost:8080/swagger`.
