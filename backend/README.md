# Backend

- `GET /health`
- `GET /api/v1/auth/session`
- `GET /api/v1/auth/google/redirect`
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
AUTH_COOKIE_SECURE=false
AUTH_COOKIE_SAME_SITE=Lax
NUMBER_SEARCH_DB_PATH=number-searches.db
```

Notes:
- Environment variables override `.env` values.
- `PUBLIC_BACKEND_BASE_URL` is the public backend origin used for the Google OAuth callback URI. Set this to the externally reachable backend URL in proxied deployments.
- `FRONTEND_BASE_URL` is used by the backend-owned Google redirect flow to send the browser back to the frontend page.
- `GET /api/v1/auth/google/redirect` starts the Google OAuth authorization-code flow and also handles the callback from Google on the same route.
- `GOOGLE_CLIENT_SECRET` is required for the backend to exchange the Google authorization code for tokens.
- After Google login succeeds, the backend issues a short-lived app access JWT and redirects the browser back to the frontend with the token in the URL fragment.
- `JWT_TTL_SECONDS` controls the access JWT lifetime.
- `GET /api/v1/auth/session` returns the authenticated user for a valid bearer access JWT.
- The temporary OAuth state/redirect-context cookies always use `Lax` so the Google callback can complete.
- `AUTH_COOKIE_SAME_SITE=None` requires `AUTH_COOKIE_SECURE=true`.
- `POST /api/v1/number/search` checks SQLite first; if the number already exists, the stored lookup result is returned. If not, a new lookup is performed and saved.
- `GET /api/v1/number/all` returns all saved number searches.
- `NUMBER_SEARCH_DB_PATH` controls where the SQLite file is stored (relative or absolute path).

## Run

```bash
./gradlew run
```

Swagger UI will be available at `http://localhost:8080/swagger`.
