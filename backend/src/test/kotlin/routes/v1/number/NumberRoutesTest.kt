package routes.v1.number

import com.pna.backend.config.AppConfig
import com.pna.backend.config.CorsOrigin
import com.pna.backend.dal.repositories.NumberSearchRepository
import com.pna.backend.plugins.configureSecurity
import com.pna.backend.routes.v1.auth.AUTH_ACCESS_COOKIE_NAME
import com.pna.backend.routes.v1.number.numberRoutes
import com.pna.backend.services.AppJwtService
import com.pna.backend.services.NumberSearchService
import com.pna.backend.services.PhoneLookupService
import domain.auth.GoogleUser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumberRoutesTest {
    @Test
    fun `search returns unauthorized when bearer token is missing`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            installNumberRoutes()
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `search returns unauthorized when bearer token is invalid`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            installNumberRoutes()
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer invalid-token")
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `search returns bad request when number is blank`() = testApplication {
        val jwtService = newJwtService()

        application {
            install(ContentNegotiation) { json() }
            installNumberRoutes()
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "bearer $token")
            setBody("""{"number":""}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `search returns success message when request is valid`() = testApplication {
        val jwtService = newJwtService()

        application {
            install(ContentNegotiation) { json() }
            installNumberRoutes()
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"result\""))
        assertTrue(body.contains("\"country\""))
        assertTrue(body.contains("\"countryCode\""))
        assertTrue(body.contains("\"regionCode\""))
        assertTrue(body.contains("\"numberType\""))
        assertTrue(body.contains("\"internationalFormat\""))
        assertTrue(body.contains("\"carrier\""))
        assertTrue(body.contains("\"timeZones\""))
    }

    @Test
    fun `search accepts lowercase bearer auth scheme`() = testApplication {
        val jwtService = newJwtService()

        application {
            install(ContentNegotiation) { json() }
            installNumberRoutes()
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "bearer $token")
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `search accepts auth cookie`() = testApplication {
        val jwtService = newJwtService()

        application {
            install(ContentNegotiation) { json() }
            installNumberRoutes()
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            cookie(AUTH_ACCESS_COOKIE_NAME, "$token")
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `search persists and searches endpoint returns saved numbers`() = testApplication {
        val jwtService = newJwtService()

        application {
            install(ContentNegotiation) { json() }
            installNumberRoutes()
        }

        val token = jwtService.issueAccessToken(user())

        val searchedNumber = "1234567890"

        val searchResponse = client.post("/api/v1/number/search") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"number":"$searchedNumber"}""")
        }
        assertEquals(HttpStatusCode.OK, searchResponse.status)

        val secondSearchResponse = client.post("/api/v1/number/search") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"number":"$searchedNumber"}""")
        }
        assertEquals(HttpStatusCode.OK, secondSearchResponse.status)

        val searchesResponse = client.get("/api/v1/number/all") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, searchesResponse.status)
        assertEquals("private, no-store", searchesResponse.headers[HttpHeaders.CacheControl])
        val body = searchesResponse.bodyAsText()
        assertTrue(body.contains("\"number\":\"$searchedNumber\""))
        assertTrue(body.contains("\"result\""))
        val occurrences = "\"number\":\"$searchedNumber\"".toRegex().findAll(body).count()
        assertEquals(1, occurrences)
    }

    @Test
    fun `searches returns unauthorized when bearer token is missing`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            installNumberRoutes()
        }

        val response = client.get("/api/v1/number/all")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `search rejects disallowed origin`() = testApplication {
        val jwtService = newJwtService()

        application {
            install(ContentNegotiation) { json() }
            installNumberRoutes()
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.Origin, "https://evil.example")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("Invalid origin"))
    }

    private fun Application.installNumberRoutes(
        appConfig: AppConfig = testAppConfig(),
        accessTokenService: AppJwtService = newJwtService(),
        lookupService: PhoneLookupService = PhoneLookupService(),
        searchService: NumberSearchService = newSearchService(),
    ) {
        if (pluginOrNull(Authentication) == null) {
            configureSecurity(
                accessTokenService,
                appConfig.jwtIssuer,
                appConfig.jwtAudience
            )
        }

        routing {
            numberRoutes(appConfig, accessTokenService::verify, lookupService, searchService)
        }
    }

    private fun newJwtService(): AppJwtService {
        return AppJwtService(
            issuer = "test-issuer",
            audience = "test-audience",
            secret = "test-secret",
            ttlSeconds = 900L
        )
    }

    private fun testAppConfig(): AppConfig {
        return AppConfig(
            port = 8080,
            host = "0.0.0.0",
            googleClientId = "client-id",
            googleClientSecret = "client-secret",
            publicBackendBaseUrl = "https://api.example.com",
            frontendBaseUrl = "http://localhost:5173",
            allowedOrigins = listOf(CorsOrigin(host = "localhost:5173", schemes = listOf("http"))),
            jwtSecret = "test-secret",
            jwtIssuer = "test-issuer",
            jwtAudience = "test-audience",
            jwtTtlSeconds = 900L,
            authCookieSecure = false,
            authCookieSameSite = "Lax",
            googleOauthStateTtlSeconds = 600,
            redirectContextTtlSeconds = 600,
            refreshTokenTtlSeconds = 600,
            oauthFlowCookieSameSite = "Lax"
        )
    }

    private fun newSearchService(): NumberSearchService {
        val dbPath = Files.createTempFile("number-routes-test", ".db").toString()
        return NumberSearchService(NumberSearchRepository(dbPath))
    }

    private fun user(): GoogleUser = GoogleUser("subject", "user@example.com", "Jane", "Jane")
}
