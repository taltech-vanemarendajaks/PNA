package routes.v1.auth

import com.pna.backend.config.AppConfig
import com.pna.backend.config.CorsOrigin
import com.pna.backend.plugins.configureSecurity
import com.pna.backend.routes.v1.auth.googleAuthRoutes
import com.pna.backend.services.AppJwtService
import com.pna.backend.services.GoogleAuthCodeService
import com.pna.backend.services.GoogleTokenVerifierService
import domain.auth.GoogleUser
import io.ktor.client.plugins.cookies.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
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
            installAuthRoutes()
        }

        val noRedirectClient = createClient {
            followRedirects = false
        }

        val response = noRedirectClient.get(
            "/api/v1/auth/google/redirect?frontendOrigin=http%3A%2F%2Flocalhost%3A5173&returnPath=%2Fnumbers%3Fq%3D123"
        )

        val setCookies = response.headers.getAll(HttpHeaders.SetCookie) ?: emptyList()

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
    fun `google redirect callback exchanges code and redirects back to stored frontend context with access token fragment`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            installAuthRoutes()
        }

        val response = client.get("/api/v1/auth/google/redirect?code=auth-code&state=expected-state") {
            cookie("pna_google_oauth_state", "expected-state")
            cookie("pna_frontend_origin", "http%3A%2F%2Flocalhost%3A5173")
            cookie("pna_return_path", "%2Fnumbers%3Fq%3D123")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val location = response.headers[HttpHeaders.Location] ?: error("Missing redirect location")
        assertTrue(location.startsWith("http://localhost:5173/numbers?q=123#accessToken="))
        assertTrue(response.bodyAsText().contains("window.location.replace(\"http://localhost:5173/numbers?q=123#accessToken="))
    }

    @Test
    fun `google redirect callback rejects state mismatch`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            installAuthRoutes()
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
    fun `google redirect callback ignores query-based frontend context and uses stored backend context`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            installAuthRoutes()
        }

        val response = client.get("/api/v1/auth/google/redirect?code=auth-code&state=wrong-state&frontendOrigin=https%3A%2F%2Fevil.example&returnPath=%2Fnumbers") {
            cookie("pna_google_oauth_state", "expected-state")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            "http://localhost:5173/?authError=Google+login+state+mismatch",
            response.headers[HttpHeaders.Location]
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
            installAuthRoutes(
                appConfig = testAppConfig(
                    frontendBaseUrl = "https://app.example.com",
                    allowedOrigins = listOf(CorsOrigin(host = "app.example.com", schemes = listOf("https"))),
                    authCookieSecure = true,
                    authCookieSameSite = "Strict",
                    oauthFlowCookieSameSite = "Lax"
                )
            )
        }

        val noRedirectClient = createClient {
            followRedirects = false
        }

        val response = noRedirectClient.get("/api/v1/auth/google/redirect?frontendOrigin=https%3A%2F%2Fapp.example.com&returnPath=%2F")
        val setCookies = response.headers.getAll(HttpHeaders.SetCookie) ?: emptyList()

        assertTrue(setCookies.any { it.contains("pna_google_oauth_state=") && it.contains("SameSite=Lax") })
        assertTrue(setCookies.any { it.contains("pna_frontend_origin=") && it.contains("SameSite=Lax") })
        assertTrue(setCookies.any { it.contains("pna_return_path=") && it.contains("SameSite=Lax") })
    }

    @Test
    fun `session returns authenticated user for valid bearer token`() = testApplication {
        val jwtService = newJwtService()

        application {
            install(ContentNegotiation) { json() }
            configureSecurity(
                accessTokenService = jwtService,
                issuer = "test-issuer",
                audience = "test-audience"
            )
            installAuthRoutes(accessTokenService = jwtService)
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.get("/api/v1/auth/session") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"subject\":\"subject\""))
    }

    @Test
    fun `session accepts lowercase bearer auth scheme`() = testApplication {
        val jwtService = newJwtService()

        application {
            install(ContentNegotiation) { json() }
            configureSecurity(
                accessTokenService = jwtService,
                issuer = "test-issuer",
                audience = "test-audience"
            )
            installAuthRoutes(accessTokenService = jwtService)
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.get("/api/v1/auth/session") {
            header(HttpHeaders.Authorization, "bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `session returns unauthorized for missing bearer token`() = testApplication {
        val jwtService = newJwtService()

        application {
            install(ContentNegotiation) { json() }
            configureSecurity(
                accessTokenService = jwtService,
                issuer = "test-issuer",
                audience = "test-audience"
            )
            installAuthRoutes(accessTokenService = jwtService)
        }

        val response = client.get("/api/v1/auth/session")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    private fun Application.installAuthRoutes(
        appConfig: AppConfig = testAppConfig(),
        accessTokenService: AppJwtService = newJwtService(),
        verifyGoogleCredential: (String) -> GoogleUser? = { user() },
        exchangeGoogleAuthCode: (String, String) -> String? = { _, _ -> "id-token" }
    ) {
        if (pluginOrNull(Authentication) == null) {
            configureSecurity(
                accessTokenService = accessTokenService,
                issuer = appConfig.jwtIssuer,
                audience = appConfig.jwtAudience
            )
        }

        routing {
            googleAuthRoutes(
                appConfig = appConfig,
                accessTokenService = accessTokenService,
                googleTokenVerifierService = FakeGoogleTokenVerifierService(verifyGoogleCredential),
                googleAuthCodeService = FakeGoogleAuthCodeService(exchangeGoogleAuthCode)
            )
        }
    }

    private fun testAppConfig(
        googleClientId: String = "client-id",
        googleClientSecret: String = "client-secret",
        publicBackendBaseUrl: String = "https://api.example.com",
        frontendBaseUrl: String = "http://localhost:5173",
        allowedOrigins: List<CorsOrigin> = listOf(CorsOrigin(host = "localhost:5173", schemes = listOf("http"))),
        authCookieSecure: Boolean = false,
        authCookieSameSite: String = "Lax",
        googleOauthStateTtlSeconds: Int = 600,
        redirectContextTtlSeconds: Int = 600,
        oauthFlowCookieSameSite: String = "Lax"
    ): AppConfig {
        return AppConfig(
            port = 8080,
            host = "0.0.0.0",
            googleClientId = googleClientId,
            googleClientSecret = googleClientSecret,
            publicBackendBaseUrl = publicBackendBaseUrl,
            frontendBaseUrl = frontendBaseUrl,
            allowedOrigins = allowedOrigins,
            jwtSecret = "test-secret",
            jwtIssuer = "test-issuer",
            jwtAudience = "test-audience",
            jwtTtlSeconds = 900L,
            authCookieSecure = authCookieSecure,
            authCookieSameSite = authCookieSameSite,
            googleOauthStateTtlSeconds = googleOauthStateTtlSeconds,
            redirectContextTtlSeconds = redirectContextTtlSeconds,
            oauthFlowCookieSameSite = oauthFlowCookieSameSite
        )
    }

    private fun newJwtService(): AppJwtService {
        return AppJwtService(
            issuer = "test-issuer",
            audience = "test-audience",
            secret = "test-secret",
            ttlSeconds = 900L
        )
    }

    private fun user(): GoogleUser = GoogleUser("subject", "user@example.com", "Jane", "Jane")

    private class FakeGoogleTokenVerifierService(
        private val verifier: (String) -> GoogleUser?
    ) : GoogleTokenVerifierService("test-client-id") {
        override fun verify(idToken: String): GoogleUser? = verifier(idToken)
    }

    private class FakeGoogleAuthCodeService(
        private val exchanger: (String, String) -> String?
    ) : GoogleAuthCodeService("test-client-id", "test-client-secret") {
        override fun exchangeCodeForIdToken(code: String, redirectUri: String): String? = exchanger(code, redirectUri)
    }
}
