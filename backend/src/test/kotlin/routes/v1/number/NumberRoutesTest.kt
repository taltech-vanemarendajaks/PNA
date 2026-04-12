package routes.v1.number

import com.pna.backend.dal.repositories.NumberSearchRepository
import com.pna.backend.routes.v1.auth.AUTH_SESSION_COOKIE_NAME
import com.pna.backend.routes.v1.number.numberRoutes
import com.pna.backend.services.AuthSessionService
import com.pna.backend.services.NumberSearchService
import com.pna.backend.services.PhoneLookupService
import domain.auth.GoogleUser
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumberRoutesTest {
    @Test
    fun `search returns unauthorized when session cookie is missing`() = testApplication {
        val authSessionService = AuthSessionService()
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(authSessionService, lookupService, searchService)
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `search returns unauthorized when session is invalid`() = testApplication {
        val authSessionService = AuthSessionService()
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(authSessionService, lookupService, searchService)
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            cookie(AUTH_SESSION_COOKIE_NAME, "missing-session")
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `search returns bad request when number is blank`() = testApplication {
        val authSessionService = AuthSessionService()
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()
        val sessionId = authSessionService.create(
            GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")
        )

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(authSessionService, lookupService, searchService)
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            cookie(AUTH_SESSION_COOKIE_NAME, sessionId)
            setBody("""{"number":""}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `search returns success message when request is valid`() = testApplication {
        val authSessionService = AuthSessionService()
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()
        val sessionId = authSessionService.create(
            GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")
        )

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(authSessionService, lookupService, searchService)
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            cookie(AUTH_SESSION_COOKIE_NAME, sessionId)
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
    fun `search persists and searches endpoint returns saved numbers`() = testApplication {
        val authSessionService = AuthSessionService()
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()
        val sessionId = authSessionService.create(
            GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")
        )

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(authSessionService, lookupService, searchService)
            }
        }

        val searchedNumber = "1234567890"

        val searchResponse = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            cookie(AUTH_SESSION_COOKIE_NAME, sessionId)
            setBody("""{"number":"$searchedNumber"}""")
        }
        assertEquals(HttpStatusCode.OK, searchResponse.status)

        val secondSearchResponse = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            cookie(AUTH_SESSION_COOKIE_NAME, sessionId)
            setBody("""{"number":"$searchedNumber"}""")
        }
        assertEquals(HttpStatusCode.OK, secondSearchResponse.status)

        val searchesResponse = client.get("/api/v1/number/all") {
            cookie(AUTH_SESSION_COOKIE_NAME, sessionId)
        }

        assertEquals(HttpStatusCode.OK, searchesResponse.status)
        val body = searchesResponse.bodyAsText()
        assertTrue(body.contains("\"number\":\"$searchedNumber\""))
        assertTrue(body.contains("\"result\""))
        val occurrences = "\"number\":\"$searchedNumber\"".toRegex().findAll(body).count()
        assertEquals(1, occurrences)
    }

    @Test
    fun `searches returns unauthorized when session cookie is missing`() = testApplication {
        val authSessionService = AuthSessionService()
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(authSessionService, lookupService, searchService)
            }
        }

        val response = client.get("/api/v1/number/all")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    private fun newSearchService(): NumberSearchService {
        val dbPath = Files.createTempFile("number-routes-test", ".db").toString()
        return NumberSearchService(NumberSearchRepository(dbPath))
    }
}
