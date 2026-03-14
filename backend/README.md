# Backend

- `GET /health`
- `POST /api/v1/auth/google`
- `GET /openapi`
- `GET /swagger`

## Request body

```json
{
  "idToken": "<google id token from frontend>"
}
```

## Configuration

Create `backend/.env` (you can copy `backend/.env.example`) and set values there.

```env
APP_HOST=0.0.0.0
APP_PORT=8080
GOOGLE_CLIENT_ID=your-google-web-client-id.apps.googleusercontent.com
CORS_ALLOW_ANY_HOST=true
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Notes:
- Environment variables override `.env` values.
- If `CORS_ALLOW_ANY_HOST=false`, set `CORS_ALLOWED_ORIGINS` to a comma-separated list of origins.

## Run

```bash
./gradlew run
```

Swagger UI will be available at `http://localhost:8080/swagger`.
