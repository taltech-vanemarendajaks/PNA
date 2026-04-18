package routes.v1.number

import com.pna.backend.dal.repositories.NumberSearchRepository
import com.pna.backend.routes.v1.auth.AUTH_ACCESS_COOKIE_NAME
import com.pna.backend.routes.v1.number.numberRoutes
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
    fun `search returns unauthorized when bearer token is missing`() = testApplication {
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(::verifyAccessToken, lookupService, searchService)
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `search returns unauthorized when bearer token is invalid`() = testApplication {
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(::verifyAccessToken, lookupService, searchService)
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer invalid-token")
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `search returns bad request when number is blank`() = testApplication {
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(::verifyAccessToken, lookupService, searchService)
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid-token")
            setBody("""{"number":""}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `search returns success message when request is valid`() = testApplication {
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(::verifyAccessToken, lookupService, searchService)
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid-token")
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
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(::verifyAccessToken, lookupService, searchService)
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "bearer valid-token")
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `search accepts auth cookie`() = testApplication {
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(::verifyAccessToken, lookupService, searchService)
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            cookie(AUTH_ACCESS_COOKIE_NAME, "valid-token")
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `search persists and searches endpoint returns saved numbers`() = testApplication {
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(::verifyAccessToken, lookupService, searchService)
            }
        }

        val searchedNumber = "1234567890"

        val searchResponse = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid-token")
            setBody("""{"number":"$searchedNumber"}""")
        }
        assertEquals(HttpStatusCode.OK, searchResponse.status)

        val secondSearchResponse = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid-token")
            setBody("""{"number":"$searchedNumber"}""")
        }
        assertEquals(HttpStatusCode.OK, secondSearchResponse.status)

        val searchesResponse = client.get("/api/v1/number/all") {
            header(HttpHeaders.Authorization, "Bearer valid-token")
        }

        assertEquals(HttpStatusCode.OK, searchesResponse.status)
        val body = searchesResponse.bodyAsText()
        assertTrue(body.contains("\"number\":\"$searchedNumber\""))
        assertTrue(body.contains("\"result\""))
        val occurrences = "\"number\":\"$searchedNumber\"".toRegex().findAll(body).count()
        assertEquals(1, occurrences)
    }

    @Test
    fun `searches returns unauthorized when bearer token is missing`() = testApplication {
        val lookupService = PhoneLookupService()
        val searchService = newSearchService()

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(::verifyAccessToken, lookupService, searchService)
            }
        }

        val response = client.get("/api/v1/number/all")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    private fun verifyAccessToken(token: String): GoogleUser? {
        return if (token == "valid-token") {
            GoogleUser("subject", "user@example.com", "Jane", "Jane")
        } else {
            null
        }
    }

    private fun newSearchService(): NumberSearchService {
        val dbPath = Files.createTempFile("number-routes-test", ".db").toString()
        return NumberSearchService(NumberSearchRepository(dbPath))
    }
}
