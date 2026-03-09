# PNA
Phone Number Analyser

## Backend Auth API

- `GET /health`
- `POST /api/v1/auth/google`

### Request body

```json
{
  "idToken": "<google id token from frontend>"
}
```

### Environment variable

Set `GOOGLE_CLIENT_ID` to your Google OAuth Web Client ID before starting the server.

### Run

```bash
cd backend
./gradlew run
```
