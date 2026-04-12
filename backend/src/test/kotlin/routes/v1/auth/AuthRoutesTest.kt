package routes.v1.auth

import com.pna.backend.config.CorsOrigin
import com.pna.backend.routes.v1.auth.googleAuthRoutes
import com.pna.backend.services.AuthSessionService
import domain.auth.GoogleUser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthRoutesTest {
    @Test
    fun `google redirect start route redirects to Google authorization and stores validated redirect context`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing {
                googleAuthRoutes(
                    googleClientId = "client-id",
                    googleClientSecret = "client-secret",
                    publicBackendBaseUrl = "https://api.example.com",
                    frontendBaseUrl = "http://localhost:5173",
                    allowedOrigins = listOf(CorsOrigin(host = "localhost:5173", schemes = listOf("http"))),
                    authSessionService = AuthSessionService(),
                    sessionTtlSeconds = 3600,
                    authCookieSecure = false,
                    authCookieSameSite = "Lax",
                    verifyGoogleCredential = {
                        GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")
                    },
                    exchangeGoogleAuthCode = { _, _ -> "id-token" }
                )
            }
        }

        val noRedirectClient = createClient {
            followRedirects = false
        }

        val response = noRedirectClient.get(
            "/api/v1/auth/google/redirect?frontendOrigin=http%3A%2F%2Flocalhost%3A5173&returnPath=%2Fnumbers%3Fq%3D123"
        )

        val setCookies = response.headers.getAll("Set-Cookie") ?: emptyList()

        assertEquals(HttpStatusCode.Found, response.status)
        assertTrue(response.headers[HttpHeaders.Location]?.startsWith("https://accounts.google.com/o/oauth2/v2/auth?") == true)
        assertTrue(response.headers[HttpHeaders.Location]?.contains("client_id=client-id") == true)
        assertTrue(
            response.headers[HttpHeaders.Location]?.contains(
                "redirect_uri=https%3A%2F%2Fapi.example.com%2Fapi%2Fv1%2Fauth%2Fgoogle%2Fredirect"
            ) == true
        )
        assertTrue(setCookies.any { it.contains("pna_google_oauth_state=") })
        assertTrue(setCookies.any { it.contains("pna_frontend_origin=") })
        assertTrue(setCookies.any { it.contains("pna_return_path=") })
    }

    @Test
    fun `google redirect callback exchanges code and redirects back to stored frontend context`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing {
                googleAuthRoutes(
                    googleClientId = "client-id",
                    googleClientSecret = "client-secret",
                    publicBackendBaseUrl = "https://api.example.com",
                    frontendBaseUrl = "http://localhost:5173",
                    allowedOrigins = listOf(CorsOrigin(host = "localhost:5173", schemes = listOf("http"))),
                    authSessionService = AuthSessionService(),
                    sessionTtlSeconds = 3600,
                    authCookieSecure = false,
                    authCookieSameSite = "Lax",
                    verifyGoogleCredential = {
                        GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")
                    },
                    exchangeGoogleAuthCode = { code, redirectUri ->
                        if (code == "auth-code" && redirectUri == "https://api.example.com/api/v1/auth/google/redirect") {
                            "id-token"
                        } else {
                            null
                        }
                    }
                )
            }
        }

        val response = client.get("/api/v1/auth/google/redirect?code=auth-code&state=expected-state") {
            cookie("pna_google_oauth_state", "expected-state")
            cookie("pna_frontend_origin", "http%3A%2F%2Flocalhost%3A5173")
            cookie("pna_return_path", "%2Fnumbers%3Fq%3D123")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("http://localhost:5173/numbers?q=123", response.headers[HttpHeaders.Location])
        assertTrue(response.headers.getAll("Set-Cookie")?.any { it.contains("pna_session=") } == true)
        assertTrue(response.bodyAsText().contains("window.location.replace(\"http://localhost:5173/numbers?q=123\")"))
    }

    @Test
    fun `google redirect callback rejects state mismatch`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing {
                googleAuthRoutes(
                    googleClientId = "client-id",
                    googleClientSecret = "client-secret",
                    publicBackendBaseUrl = "https://api.example.com",
                    frontendBaseUrl = "http://localhost:5173",
                    allowedOrigins = listOf(CorsOrigin(host = "localhost:5173", schemes = listOf("http"))),
                    authSessionService = AuthSessionService(),
                    sessionTtlSeconds = 3600,
                    authCookieSecure = false,
                    authCookieSameSite = "Lax",
                    verifyGoogleCredential = {
                        GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")
                    },
                    exchangeGoogleAuthCode = { _, _ -> "id-token" }
                )
            }
        }

        val response = client.get("/api/v1/auth/google/redirect?code=auth-code&state=wrong-state") {
            cookie("pna_google_oauth_state", "expected-state")
            cookie("pna_frontend_origin", "http%3A%2F%2Flocalhost%3A5173")
            cookie("pna_return_path", "%2F")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("http://localhost:5173/?authError=Google+login+state+mismatch", response.headers[HttpHeaders.Location])
    }

    @Test
    fun `google redirect falls back to configured frontend when requested origin is not allowed`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing {
                googleAuthRoutes(
                    googleClientId = null,
                    googleClientSecret = null,
                    publicBackendBaseUrl = "https://api.example.com",
                    frontendBaseUrl = "http://localhost:5173",
                    allowedOrigins = listOf(CorsOrigin(host = "localhost:5173", schemes = listOf("http"))),
                    authSessionService = AuthSessionService(),
                    sessionTtlSeconds = 3600,
                    authCookieSecure = false,
                    authCookieSameSite = "Lax"
                )
            }
        }

        val response = client.get("/api/v1/auth/google/redirect?frontendOrigin=https%3A%2F%2Fevil.example&returnPath=%2F")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            "http://localhost:5173/?authError=GOOGLE_CLIENT_ID+is+not+configured",
            response.headers["Location"]
        )
        assertTrue(
            response.bodyAsText().contains(
                "window.location.replace(\"http://localhost:5173/?authError=GOOGLE_CLIENT_ID+is+not+configured\")"
            )
        )
    }

    @Test
    fun `google redirect callback ignores query-based frontend context and uses stored backend context`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing {
                googleAuthRoutes(
                    googleClientId = null,
                    googleClientSecret = null,
                    publicBackendBaseUrl = "https://api.example.com",
                    frontendBaseUrl = "http://localhost:5173",
                    allowedOrigins = listOf(CorsOrigin(host = "localhost:5173", schemes = listOf("http"))),
                    authSessionService = AuthSessionService(),
                    sessionTtlSeconds = 3600,
                    authCookieSecure = false,
                    authCookieSameSite = "Lax"
                )
            }
        }

        val response = client.get("/api/v1/auth/google/redirect?code=auth-code&state=wrong-state&frontendOrigin=https%3A%2F%2Fevil.example&returnPath=%2Fnumbers") {
            cookie("pna_google_oauth_state", "expected-state")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            "http://localhost:5173/?authError=Google+login+state+mismatch",
            response.headers["Location"]
        )
        assertTrue(
            response.bodyAsText().contains(
                "window.location.replace(\"http://localhost:5173/?authError=Google+login+state+mismatch\")"
            )
        )
    }

    @Test
    fun `google redirect start route uses lax cookies for oauth flow even when auth cookie is strict`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing {
                googleAuthRoutes(
                    googleClientId = "client-id",
                    googleClientSecret = "client-secret",
                    publicBackendBaseUrl = "https://api.example.com",
                    frontendBaseUrl = "https://app.example.com",
                    allowedOrigins = listOf(CorsOrigin(host = "app.example.com", schemes = listOf("https"))),
                    authSessionService = AuthSessionService(),
                    sessionTtlSeconds = 3600,
                    authCookieSecure = true,
                    authCookieSameSite = "Strict",
                    verifyGoogleCredential = {
                        GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")
                    },
                    exchangeGoogleAuthCode = { _, _ -> "id-token" }
                )
            }
        }

        val noRedirectClient = createClient {
            followRedirects = false
        }

        val response = noRedirectClient.get("/api/v1/auth/google/redirect?frontendOrigin=https%3A%2F%2Fapp.example.com&returnPath=%2F")
        val setCookies = response.headers.getAll("Set-Cookie") ?: emptyList()

        assertTrue(setCookies.any { it.contains("pna_google_oauth_state=") && it.contains("SameSite=Lax") })
        assertTrue(setCookies.any { it.contains("pna_frontend_origin=") && it.contains("SameSite=Lax") })
        assertTrue(setCookies.any { it.contains("pna_return_path=") && it.contains("SameSite=Lax") })
    }
}
