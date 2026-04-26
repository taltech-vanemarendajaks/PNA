package routes.v1.auth

import com.pna.backend.config.CorsOrigin
import com.pna.backend.config.RootConfig
import com.pna.backend.dal.Database
import com.pna.backend.dal.repositories.RefreshTokenRepository
import com.pna.backend.dal.repositories.UserRepository
import com.pna.backend.plugins.configureSecurity
import com.pna.backend.routes.v1.auth.AUTH_ACCESS_COOKIE_NAME
import com.pna.backend.routes.v1.auth.REFRESH_TOKEN_COOKIE_NAME
import com.pna.backend.routes.v1.auth.googleAuthRoutes
import com.pna.backend.services.AppJwtService
import com.pna.backend.services.GoogleAuthCodeService
import com.pna.backend.services.GoogleTokenVerifierService
import com.pna.backend.services.RefreshTokenService
import domain.auth.GoogleUser
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.*
import support.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import testJwtService
import testRootConfig

class AuthRoutesTest {
    @Test
    fun `google redirect start route redirects to Google authorization and stores validated redirect context`() = testApplication {
        application {
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
        assertTrue(response.headers[HttpHeaders.Location]?.contains("client_id=google-client-id") == true)
        assertTrue(
            response.headers[HttpHeaders.Location]?.contains(
                "redirect_uri=http%3A%2F%2Flocalhost%3A8080%2Fapi%2Fv1%2Fauth%2Fgoogle%2Fredirect"
            ) == true
        )
        assertTrue(setCookies.any { it.contains("pna_google_oauth_state=") })
        assertTrue(setCookies.any { it.contains("pna_frontend_origin=") })
        assertTrue(setCookies.any { it.contains("pna_return_path=") })
    }

    @Test
    fun `google redirect callback exchanges code and redirects back to stored frontend context with auth and refresh cookies`() = testApplication {
        val database = TestDatabase.newDatabase("refresh-token-routes-test")

        try {
            database.migrate()

            application {
                installAuthRoutes(database = database)
            }

            val response = client.get("/api/v1/auth/google/redirect?code=auth-code&state=expected-state") {
                cookie("pna_google_oauth_state", "expected-state")
                cookie("pna_frontend_origin", "http%3A%2F%2Flocalhost%3A5173")
                cookie("pna_return_path", "%2Fnumbers%3Fq%3D123")
            }

            val setCookies = response.headers.getAll(HttpHeaders.SetCookie) ?: emptyList()

            assertEquals(HttpStatusCode.OK, response.status)

            val location = response.headers[HttpHeaders.Location] ?: error("Missing redirect location")

            assertEquals("http://localhost:5173/numbers?q=123", location)
            assertTrue(response.bodyAsText().contains("window.location.replace(\"http://localhost:5173/numbers?q=123\")"))
            assertTrue(setCookies.any { it.contains("$AUTH_ACCESS_COOKIE_NAME=") })
            assertEquals(1, setCookies.count { it.contains("$REFRESH_TOKEN_COOKIE_NAME=") })
        } finally {
            database.close()
        }
    }

    @Test
    fun `google redirect callback stores socket ip instead of spoofable forwarded headers`() {
        TestDatabase.newDatabase("auth_routes_ip_metadata").use { database ->
            database.migrate()

            testApplication {
                application {
                    installAuthRoutes(database = database)
                }

                val response = client.get("/api/v1/auth/google/redirect?code=auth-code&state=expected-state") {
                    cookie("pna_google_oauth_state", "expected-state")
                    cookie("pna_frontend_origin", "http%3A%2F%2Flocalhost%3A5173")
                    cookie("pna_return_path", "%2F")
                    header(HttpHeaders.XForwardedFor, "203.0.113.99")
                    header("X-Real-Ip", "198.51.100.77")
                }

                assertEquals(HttpStatusCode.OK, response.status)

                database.dataSource.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT ip_address FROM refresh_tokens").use { resultSet ->
                            assertTrue(resultSet.next())
                            val storedIp = resultSet.getString("ip_address")
                            assertTrue(storedIp.isNotBlank())
                            assertTrue(storedIp != "203.0.113.99")
                            assertTrue(storedIp != "198.51.100.77")
                            assertTrue(!resultSet.next())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `google redirect callback persists and reuses user row by subject`() {
        TestDatabase.newDatabase("auth_routes_number_search").use { database ->
            database.migrate()

            testApplication {
                application {
                    installAuthRoutes(
                        database = database,
                        verifyGoogleCredential = { idToken ->
                            when (idToken) {
                                "id-token-1" -> GoogleUser("subject-123", "first@example.com", "First Name", "First")
                                else -> GoogleUser("subject-123", "updated@example.com", "Updated Name", "Updated")
                            }
                        },
                        exchangeGoogleAuthCode = { code, _ ->
                            when (code) {
                                "auth-code-1" -> "id-token-1"
                                else -> "id-token-2"
                            }
                        }
                    )
                }

                val firstResponse = client.get("/api/v1/auth/google/redirect?code=auth-code-1&state=expected-state") {
                    cookie("pna_google_oauth_state", "expected-state")
                    cookie("pna_frontend_origin", "http%3A%2F%2Flocalhost%3A5173")
                    cookie("pna_return_path", "%2F")
                }
                assertEquals(HttpStatusCode.OK, firstResponse.status)

                val secondResponse = client.get("/api/v1/auth/google/redirect?code=auth-code-2&state=expected-state-2") {
                    cookie("pna_google_oauth_state", "expected-state-2")
                    cookie("pna_frontend_origin", "http%3A%2F%2Flocalhost%3A5173")
                    cookie("pna_return_path", "%2F")
                }
                assertEquals(HttpStatusCode.OK, secondResponse.status)

                database.dataSource.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery(
                            "SELECT subject, email, name, given_name FROM users"
                        ).use { resultSet ->
                            assertTrue(resultSet.next())
                            assertEquals("subject-123", resultSet.getString("subject"))
                            assertEquals("updated@example.com", resultSet.getString("email"))
                            assertEquals("Updated Name", resultSet.getString("name"))
                            assertEquals("Updated", resultSet.getString("given_name"))
                            assertTrue(!resultSet.next())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `google redirect callback rejects state mismatch`() = testApplication {
        application {
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
            installAuthRoutes(
                rootConfig = testRootConfig(
                    authCookieSecure = true,
                    authCookieSameSite = "Strict",
                    oauthFlowCookieSameSite = "Lax",
                    frontendBaseUrl = "https://app.example.com",
                    allowedOriginsMapped = listOf(CorsOrigin("app.example.com", listOf("https"))),
                    allowedOrigins = listOf("https://app.example.com")
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
    fun `android google login verifies id token and returns access and refresh tokens`() = testApplication {
        val refreshTokenService = newRefreshTokenService()
        val jwtService = newJwtService()

        application {
            install(ContentNegotiation) { json() }
            installAuthRoutes(
                accessTokenService = jwtService,
                refreshTokenService = refreshTokenService,
                verifyGoogleCredential = { idToken ->
                    if (idToken == "android-google-id-token") user() else null
                }
            )
        }

        val response = client.post("/api/v1/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "idToken": "android-google-id-token"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("private, no-store", response.headers[HttpHeaders.CacheControl])

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        val accessToken = body["token"]?.jsonPrimitive?.content
        val refreshToken = body["refreshToken"]?.jsonPrimitive?.content
        val displayName = body["displayName"]?.jsonPrimitive?.content

        assertNotNull(accessToken)
        assertNotNull(refreshToken)
        assertEquals("Jane", displayName)

        val verifiedUser = jwtService.verify(accessToken)
        assertNotNull(verifiedUser)
        assertEquals("subject", verifiedUser.subject)
        assertEquals("user@example.com", verifiedUser.email)

        val rotation = refreshTokenService.rotateRefreshToken(refreshToken)
        assertNotNull(rotation)
        assertEquals("subject", rotation.user.subject)
    }

    @Test
    fun `android google login rejects blank id token`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            installAuthRoutes()
        }

        val response = client.post("/api/v1/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "idToken": ""
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("idToken is required"))
    }

    @Test
    fun `android google login rejects invalid google id token`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            installAuthRoutes(
                verifyGoogleCredential = { null }
            )
        }

        val response = client.post("/api/v1/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "idToken": "invalid-token"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Invalid Google token"))
    }

    @Test
    fun `android refresh rotates refresh token and returns new access and refresh tokens`() = testApplication {
        val refreshTokenService = newRefreshTokenService()
        val jwtService = newJwtService()
        val originalRefreshToken = refreshTokenService.createRefreshToken(user())

        application {
            install(ContentNegotiation) { json() }
            installAuthRoutes(
                accessTokenService = jwtService,
                refreshTokenService = refreshTokenService
            )
        }

        val response = client.post("/api/v1/auth/android-refresh") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "refreshToken": "$originalRefreshToken"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("private, no-store", response.headers[HttpHeaders.CacheControl])

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        val newAccessToken = body["token"]?.jsonPrimitive?.content
        val newRefreshToken = body["refreshToken"]?.jsonPrimitive?.content

        assertNotNull(newAccessToken)
        assertNotNull(newRefreshToken)
        assertTrue(newRefreshToken != originalRefreshToken)

        val verifiedUser = jwtService.verify(newAccessToken)
        assertNotNull(verifiedUser)
        assertEquals("subject", verifiedUser.subject)

        val secondRotation = refreshTokenService.rotateRefreshToken(newRefreshToken)
        assertNotNull(secondRotation)
        assertEquals("subject", secondRotation.user.subject)
    }

    @Test
    fun `android refresh rejects blank refresh token`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            installAuthRoutes()
        }

        val response = client.post("/api/v1/auth/android-refresh") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "refreshToken": ""
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("refreshToken is required"))
    }

    @Test
    fun `android refresh rejects invalid refresh token`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            installAuthRoutes(
                refreshTokenService = newRefreshTokenService()
            )
        }

        val response = client.post("/api/v1/auth/android-refresh") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "refreshToken": "invalid-refresh-token"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(response.bodyAsText().contains("Refresh token is invalid or expired"))
    }

    @Test
    fun `session returns authenticated user for valid bearer token`() = testApplication {
        val jwtService = testJwtService()

        application {
            configureSecurity(
                jwtService,
                "test-issuer",
                "test-audience"
            )
            installAuthRoutes(accessTokenService = jwtService)
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.get("/api/v1/auth/session") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("private, no-store", response.headers[HttpHeaders.CacheControl])
        assertTrue(response.bodyAsText().contains("\"subject\":\"subject\""))
    }

    @Test
    fun `session returns authenticated user for valid auth cookie`() = testApplication {
        val jwtService = testJwtService()

        application {
            configureSecurity(
                jwtService,
                "test-issuer",
                "test-audience"
            )
            installAuthRoutes(accessTokenService = jwtService)
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.get("/api/v1/auth/session") {
            cookie(AUTH_ACCESS_COOKIE_NAME, token)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"subject\":\"subject\""))
    }

    @Test
    fun `session accepts lowercase bearer auth scheme`() = testApplication {
        val jwtService = testJwtService()

        application {
            configureSecurity(
                jwtService,
                "test-issuer",
                "test-audience"
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
        val jwtService = testJwtService()

        application {
            configureSecurity(
                jwtService,
                "test-issuer",
                "test-audience"
            )
            installAuthRoutes(accessTokenService = jwtService)
        }

        val response = client.get("/api/v1/auth/session")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `logout rejects disallowed origin`() = testApplication {
        val accessTokenService = testJwtService()

        application {
            installAuthRoutes(
                accessTokenService = accessTokenService
            )
        }

        val token = accessTokenService.issueAccessToken(user())

        val response = client.post("/api/v1/auth/logout") {
            header(HttpHeaders.Origin, "https://evil.example")
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("Origin is not allowed"))
    }

    @Test
    fun `refresh rotates the refresh cookie and reissues the auth access cookie`() {
        TestDatabase.newDatabase("refresh_token_routes_rotate").use { database ->
            database.migrate()

            testApplication {
                val refreshTokenService = newRefreshTokenService(database)

                application {
                    installAuthRoutes(
                        database = database,
                        refreshTokenService = refreshTokenService,
                        accessTokenService = testJwtService()
                    )
                }

                val refreshToken = refreshTokenService.createRefreshToken(user(), metadata())

                val response = client.post("/api/v1/auth/refresh") {
                    header(HttpHeaders.Origin, "http://localhost:5173")
                    cookie("pna_refresh_token", refreshToken)
                }

                val setCookies = response.headers.getAll(HttpHeaders.SetCookie) ?: emptyList()

                assertEquals(HttpStatusCode.NoContent, response.status)
                assertTrue(setCookies.any { it.contains("$AUTH_ACCESS_COOKIE_NAME=") })
                assertTrue(setCookies.any { it.contains("$REFRESH_TOKEN_COOKIE_NAME=") })
            }
        }
    }

    @Test
    fun `refresh clears auth cookies when refresh token is invalid`() {
        TestDatabase.newDatabase("refresh_token_routes_invalid").use { database ->
            database.migrate()

            testApplication {
                application {
                    installAuthRoutes(
                        database = database,
                        refreshTokenService = newRefreshTokenService(database)
                    )
                }

                val response = client.post("/api/v1/auth/refresh") {
                    header(HttpHeaders.Origin, "http://localhost:5173")
                    cookie(REFRESH_TOKEN_COOKIE_NAME, "invalid-refresh-token")
                }

                val setCookies = response.headers.getAll(HttpHeaders.SetCookie) ?: emptyList()

                assertEquals(HttpStatusCode.Unauthorized, response.status)
                assertTrue(
                    setCookies.any {
                        it.contains("$REFRESH_TOKEN_COOKIE_NAME=") && (it.contains("Max-Age=0") || it.contains("Expires=Thu, 01 Jan 1970"))
                    }
                )
                assertTrue(
                    setCookies.any {
                        it.contains("$AUTH_ACCESS_COOKIE_NAME=") && (it.contains("Max-Age=0") || it.contains("Expires=Thu, 01 Jan 1970"))
                    }
                )
            }
        }
    }

    @Test
    fun `logout clears auth and refresh cookies`() {
        TestDatabase.newDatabase("refresh_token_routes_logout").use { database ->
            database.migrate()

            testApplication {
                val refreshTokenService = newRefreshTokenService(database)
                val jwtService = testJwtService()

                application {
                    installAuthRoutes(
                        database = database,
                        refreshTokenService = refreshTokenService,
                        accessTokenService = jwtService
                    )
                }

                val refreshToken = refreshTokenService.createRefreshToken(user(), metadata())
                val token = jwtService.issueAccessToken(user())

                val response = client.post("/api/v1/auth/logout") {
                    header(HttpHeaders.Origin, "http://localhost:5173")
                    cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                    cookie(AUTH_ACCESS_COOKIE_NAME, token)
                }

                val setCookies = response.headers.getAll(HttpHeaders.SetCookie) ?: emptyList()

                assertEquals(HttpStatusCode.NoContent, response.status)
                assertTrue(
                    setCookies.any {
                        it.contains("$REFRESH_TOKEN_COOKIE_NAME=") && (it.contains("Max-Age=0") || it.contains("Expires=Thu, 01 Jan 1970"))
                    }
                )
                assertTrue(
                    setCookies.any {
                        it.contains("$AUTH_ACCESS_COOKIE_NAME=") && (it.contains("Max-Age=0") || it.contains("Expires=Thu, 01 Jan 1970"))
                    }
                )
            }
        }
    }

    private fun Application.installAuthRoutes(
        rootConfig: RootConfig = testRootConfig(),
        database: Database? = null,
        accessTokenService: AppJwtService = testJwtService(),
        refreshTokenService: RefreshTokenService = database?.let(::newRefreshTokenService) ?: newRefreshTokenService(),
        verifyGoogleCredential: (String) -> GoogleUser? = { user() },
        exchangeGoogleAuthCode: (String, String) -> String? = { _, _ -> "id-token" }
    ) {
        this.install(ContentNegotiation) {
            json()
        }

        if (pluginOrNull(Authentication) == null) {
            configureSecurity(
                accessTokenService,
                rootConfig.jwt.issuer,
                rootConfig.jwt.audience
            )
        }

        routing {
            googleAuthRoutes(
                rootConfig,
                accessTokenService,
                refreshTokenService,
                FakeGoogleTokenVerifierService(verifyGoogleCredential),
                FakeGoogleAuthCodeService(exchangeGoogleAuthCode)
            )
        }
    }

    private fun newRefreshTokenService(database: Database): RefreshTokenService {
        val userRepository = UserRepository(database)

        return RefreshTokenService(
            repository = RefreshTokenRepository(database, userRepository),
            ttlSeconds = 2592000
        )
    }

    private fun newRefreshTokenService(): RefreshTokenService {
        return TestDatabase.newDatabase("refresh-token-routes-test").use { database ->
            database.migrate()

            val userRepository = UserRepository(database)

            RefreshTokenService(
                repository = RefreshTokenRepository(database, userRepository),
                ttlSeconds = 2592000
            )
        }
    }

    private fun user(): GoogleUser = GoogleUser("subject", "user@example.com", "Jane", "Jane")

    private fun metadata() = com.pna.backend.services.SessionClientMetadata(
        userAgent = "JUnit",
        ipAddress = "127.0.0.1"
    )

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
