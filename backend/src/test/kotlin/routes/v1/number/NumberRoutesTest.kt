package com.pna.backend.routes.v1.number

import domain.auth.GoogleUser
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
    fun `search returns internal server error when google client id is missing`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes(null)
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `search returns unauthorized when bearer token is missing`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes("test-google-client-id") { null }
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `search returns unauthorized when google token is invalid`() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes("test-google-client-id") { null }
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
        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes("test-google-client-id") {
                    GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")
                }
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
        application {
            install(ContentNegotiation) { json() }
            routing {
                numberRoutes("test-google-client-id") {
                    GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")
                }
            }
        }

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid-token")
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("{\"message\":\"search was successful\"}", response.bodyAsText())
    }
}
