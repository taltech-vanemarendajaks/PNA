package com.pna.backend.routes.v1.number

import com.pna.backend.routes.v1.auth.AUTH_SESSION_COOKIE_NAME
import com.pna.backend.services.AuthSessionService
import domain.auth.GoogleUser
import io.ktor.client.request.cookie
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberRoutesTest {
    @Test
    fun `search returns unauthorized when session cookie is missing`() = testApplication {
        val authSessionService = AuthSessionService()

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(authSessionService)
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

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(authSessionService)
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
        val sessionId = authSessionService.create(
            GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")
        )

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(authSessionService)
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
        val sessionId = authSessionService.create(
            GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")
        )

        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(authSessionService)
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            cookie(AUTH_SESSION_COOKIE_NAME, sessionId)
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("{\"message\":\"search was successful\"}", response.bodyAsText())
    }
}
