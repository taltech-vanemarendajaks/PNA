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

```env
GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-google-web-client-secret
JWT_SECRET=replace-me-with-a-long-random-secret
```

Notes:
- Defaults live in `backend/src/main/resources/application.yml` and `backend/src/main/resources/application-dev.yml`.
- On startup, the backend loads real process environment variables via Hoplite `addEnvironmentSource()`, then YAML config, then applies `.env` fallback.
- In production, values set in the server environment are picked up directly from process env.
- Locally, if those process env vars are missing, `.env` is only a fallback for `JWT_SECRET`, `GOOGLE_CLIENT_ID`, and `GOOGLE_CLIENT_SECRET`.
- Use nested config path names for overrides, for example `APP_PUBLIC_BACKEND_BASE_URL`, `APP_FRONTEND_BASE_URL`, and `JWT_TTL_SECONDS`.
- `app.publicBackendBaseUrl` is used for the Google OAuth callback URI, and `app.frontendBaseUrl` is used for the final browser redirect.
- `GET /api/v1/auth/google/redirect` handles both OAuth start and callback. The flow stores validated `frontendOrigin` and `returnPath` in temporary `HttpOnly` cookies and redirects back to the stored frontend URL after successful login.
- Successful login sets an `HttpOnly` auth cookie and refresh-token cookie. `GET /api/v1/auth/session`, `POST /api/v1/auth/refresh`, and `POST /api/v1/auth/logout` all work with the cookie-based session flow.
- `app.allowedOrigins` is used for the runtime CORS allowlist, and `app.frontendBaseUrl` is always included.
- If `app.authCookieSameSite=None`, `app.authCookieSecure` must be `true`. OAuth flow cookies use `app.oauthFlowCookieSameSite` (default `Lax`) so the Google callback can complete.
- `POST /api/v1/number/search` returns a cached SQLite result when available and saves new lookups. `GET /api/v1/number/all` returns saved searches.
- `database.numberSearchPath` and `database.refreshTokenPath` control the SQLite file locations.

## Run

```bash
./gradlew run
```

Swagger UI will be available at `http://localhost:8080/swagger`.
