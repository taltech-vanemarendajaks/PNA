package routes.v1.number

import com.pna.backend.config.RootConfig
import com.pna.backend.dal.Database
import com.pna.backend.dal.repositories.NumberSearchRepository
import com.pna.backend.dal.repositories.UserRepository
import com.pna.backend.plugins.configureSecurity
import com.pna.backend.routes.v1.auth.AUTH_ACCESS_COOKIE_NAME
import com.pna.backend.routes.v1.number.numberRoutes
import com.pna.backend.services.AppJwtService
import com.pna.backend.services.NumberSearchService
import com.pna.backend.services.PhoneLookupService
import domain.auth.GoogleUser
import io.ktor.client.request.cookie
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.auth.Authentication
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import support.TestDatabase
import testJwtService
import testRootConfig
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumberRoutesTest {
    @Test
    fun `search returns unauthorized when bearer token is missing`() = testApplication {
        application {
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
        val jwtService = testJwtService()

        application {
            installNumberRoutes(accessTokenService = jwtService)
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
        val jwtService = testJwtService()

        application {
            installNumberRoutes(accessTokenService = jwtService)
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
        val jwtService = testJwtService()

        application {
            installNumberRoutes(accessTokenService = jwtService)
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
        val jwtService = testJwtService()

        application {
            installNumberRoutes(accessTokenService = jwtService)
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.post("/api/v1/number/search") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            cookie(AUTH_ACCESS_COOKIE_NAME, token)
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `search persists and searches endpoint returns authenticated user history`() = testApplication {
        val jwtService = testJwtService()

        application {
            installNumberRoutes(accessTokenService = jwtService)
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
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.OK, searchesResponse.status)
        assertEquals("private, no-store", searchesResponse.headers[HttpHeaders.CacheControl])
        val body = searchesResponse.bodyAsText()
        assertTrue(body.contains("\"number\":\"$searchedNumber\""))
        assertTrue(body.contains("\"results\""))
        val occurrences = "\"number\":\"$searchedNumber\"".toRegex().findAll(body).count()
        assertEquals(2, occurrences)
    }

    @Test
    fun `search appends per user history for authenticated subject`() {
        TestDatabase.newDatabase("number_routes_history").use { database ->
            database.migrate()

            testApplication {
                val userRepository = UserRepository(database)
                val searchService = NumberSearchService(NumberSearchRepository(database, userRepository))
                val jwtService = testJwtService()

                application {
                    installNumberRoutes(
                        database = database,
                        accessTokenService = jwtService,
                        searchService = searchService
                    )
                }

                val token = jwtService.issueAccessToken(
                    GoogleUser("subject-123", "user@example.com", "Jane", "Jane")
                )

                val response = client.post("/api/v1/number/search") {
                    header(HttpHeaders.Origin, "http://localhost:5173")
                    header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    header(HttpHeaders.Authorization, "Bearer $token")
                    setBody("""{"number":"1234567890"}""")
                }

                assertEquals(HttpStatusCode.OK, response.status)

                database.dataSource.connection.use { connection ->
                    connection.createStatement().use { statement ->
                        statement.executeQuery(
                            "SELECT u.subject, us.raw_input FROM user_search us JOIN users u ON u.id = us.user_id"
                        ).use { resultSet ->
                            assertTrue(resultSet.next())
                            assertEquals("subject-123", resultSet.getString("subject"))
                            assertEquals("1234567890", resultSet.getString("raw_input"))
                            assertTrue(!resultSet.next())
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `search history endpoint is scoped to the authenticated user`() = testApplication {
        val jwtService = testJwtService()

        application {
            installNumberRoutes(accessTokenService = jwtService)
        }

        val firstUserToken = jwtService.issueAccessToken(
            GoogleUser("subject-1", "one@example.com", "One", "One")
        )

        val secondUserToken = jwtService.issueAccessToken(
            GoogleUser("subject-2", "two@example.com", "Two", "Two")
        )

        client.post("/api/v1/number/search") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $firstUserToken")
            setBody("""{"number":"111111"}""")
        }

        client.post("/api/v1/number/search") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $secondUserToken")
            setBody("""{"number":"222222"}""")
        }

        val firstUserHistory = client.get("/api/v1/number/all") {
            header(HttpHeaders.Origin, "http://localhost:5173")
            header(HttpHeaders.Authorization, "Bearer $firstUserToken")
        }

        assertEquals(HttpStatusCode.OK, firstUserHistory.status)

        val body = firstUserHistory.bodyAsText()
        assertTrue(body.contains("\"number\":\"111111\""))
        assertTrue(!body.contains("\"number\":\"222222\""))
    }

    @Test
    fun `searches returns unauthorized when bearer token is missing`() = testApplication {
        application {
            installNumberRoutes()
        }

        val response = client.get("/api/v1/number/all")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `search rejects disallowed origin`() = testApplication {
        val jwtService = testJwtService()

        application {
            installNumberRoutes(accessTokenService = jwtService)
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

    @Test
    fun `searches rejects disallowed origin`() = testApplication {
        val jwtService = testJwtService()

        application {
            installNumberRoutes(accessTokenService = jwtService)
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.get("/api/v1/number/all") {
            header(HttpHeaders.Origin, "https://evil.example")
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertTrue(response.bodyAsText().contains("Invalid origin"))
    }

    @Test
    fun `android search returns unauthorized when bearer token is missing`() = testApplication {
        application {
            installNumberRoutes()
        }

        val response = client.post("/api/v1/number/android-search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `android search returns unauthorized when bearer token is invalid`() = testApplication {
        application {
            installNumberRoutes()
        }

        val response = client.post("/api/v1/number/android-search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer invalid-token")
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `android search returns bad request when number is blank`() = testApplication {
        val jwtService = testJwtService()

        application {
            installNumberRoutes(accessTokenService = jwtService)
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.post("/api/v1/number/android-search") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"number":""}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `android search returns success when request is valid without origin header`() = testApplication {
        val jwtService = testJwtService()

        application {
            installNumberRoutes(accessTokenService = jwtService)
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.post("/api/v1/number/android-search") {
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
    fun `android search ignores disallowed origin and relies on bearer token`() = testApplication {
        val jwtService = testJwtService()

        application {
            installNumberRoutes(accessTokenService = jwtService)
        }

        val token = jwtService.issueAccessToken(user())

        val response = client.post("/api/v1/number/android-search") {
            header(HttpHeaders.Origin, "https://evil.example")
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody("""{"number":"1234567890"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"result\""))
    }


    private fun Application.installNumberRoutes(
        rootConfig: RootConfig = testRootConfig(),
        database: Database? = null,
        accessTokenService: AppJwtService = testJwtService(),
        lookupService: PhoneLookupService = PhoneLookupService(),
        searchService: NumberSearchService? = null
    ) {
        val ownedDatabase = database ?: TestDatabase.newDatabase(uniqueSchemaName("number_routes")).also {
            it.migrate()
        }

        environment.monitor.subscribe(ApplicationStopped) {
            ownedDatabase.close()
        }

        if (pluginOrNull(ContentNegotiation) == null) {
            this.install(ContentNegotiation) {
                json()
            }
        }

        if (pluginOrNull(Authentication) == null) {
            configureSecurity(
                accessTokenService,
                rootConfig.jwt.issuer,
                rootConfig.jwt.audience
            )
        }

        val resolvedSearchService = searchService ?: newSearchService(ownedDatabase)

        routing {
            numberRoutes(
                rootConfig,
                accessTokenService::verify,
                lookupService,
                resolvedSearchService
            )
        }
    }

    private fun newSearchService(database: Database): NumberSearchService {
        val userRepository = UserRepository(database)
        return NumberSearchService(NumberSearchRepository(database, userRepository))
    }

    private fun uniqueSchemaName(prefix: String): String {
        return "${prefix}_${UUID.randomUUID().toString().replace("-", "")}"
    }

    private fun user(): GoogleUser {
        return GoogleUser("subject", "user@example.com", "Jane", "Jane")
    }
}