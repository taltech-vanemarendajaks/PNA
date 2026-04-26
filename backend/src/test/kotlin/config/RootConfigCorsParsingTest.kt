package config

import com.pna.backend.config.RootConfig
import com.pna.backend.config.CorsOrigin
import io.github.cdimascio.dotenv.Dotenv
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RootConfigCorsParsingTest {
    @Test
    fun `parseOrigins preserves port in allowed origins`() {
        val allowedOrigins = RootConfig.parseOrigins(
            listOf("http://localhost:4173","https://example.com"),
            "http://localhost:5173"
        )

        assertEquals(
            listOf(
                CorsOrigin(host = "localhost:5173", schemes = listOf("http")),
                CorsOrigin(host = "localhost:4173", schemes = listOf("http")),
                CorsOrigin(host = "example.com", schemes = listOf("https"))
            ),
            allowedOrigins
        )
    }

    @Test
    fun `parseOrigins includes frontend origin when no extra origins are configured`() {
        val allowedOrigins = RootConfig.parseOrigins(
            emptyList(),
            "http://localhost:5173"
        )

        assertEquals(
            listOf(
                CorsOrigin(host = "localhost:5173", schemes = listOf("http"))
            ),
            allowedOrigins
        )
    }

    @Test
    fun `parseOrigins removes duplicates`() {
        val allowedOrigins = RootConfig.parseOrigins(
            listOf("http://localhost:5173","https://example.com","https://example.com"),
            "http://localhost:5173"
        )

        assertEquals(
            listOf(
                CorsOrigin(host = "localhost:5173", schemes = listOf("http")),
                CorsOrigin(host = "example.com", schemes = listOf("https"))
            ),
            allowedOrigins
        )
    }

    @Test
    fun `isAllowedOrigin accepts exact configured origin and rejects wrong scheme host or malformed values`() {
        val allowedOrigins = RootConfig.parseOrigins(
            listOf("http://localhost:4173"),
            "http://localhost:5173"
        )

        assertEquals(true, RootConfig.isAllowedOrigin("http://localhost:5173", allowedOrigins))
        assertEquals(true, RootConfig.isAllowedOrigin("http://localhost:4173/", allowedOrigins))
        assertEquals(false, RootConfig.isAllowedOrigin("https://localhost:5173", allowedOrigins))
        assertEquals(false, RootConfig.isAllowedOrigin("http://evil.example", allowedOrigins))
        assertEquals(false, RootConfig.isAllowedOrigin("not-a-url", allowedOrigins))
    }

    @Test
    fun `normalizeSameSite returns expected values`() {
        assertEquals("Lax", RootConfig.normalizeSameSite("Lax"))
        assertEquals("Strict", RootConfig.normalizeSameSite("strict"))
        assertEquals("None", RootConfig.normalizeSameSite("NONE"))
    }

    @Test
    fun `normalizeSameSite rejects invalid values`() {
        assertFailsWith<IllegalArgumentException> {
            RootConfig.normalizeSameSite("something-else")
        }
    }

    @Test
    fun `validateCookieSettings rejects SameSite None without secure cookie`() {
        val error = assertFailsWith<IllegalArgumentException> {
            RootConfig.validateCookieSettings(
                authCookieSecure = false,
                authCookieSameSite = "None"
            )
        }

        assertEquals(
            "AUTH_COOKIE_SAME_SITE=None requires AUTH_COOKIE_SECURE=true",
            error.message
        )
    }

    @Test
    fun `normalizeBaseUrl trims trailing slash`() {
        assertEquals(
            "https://api.example.com",
            RootConfig.normalizeBaseUrl("https://api.example.com/")
        )
    }

    @Test
    fun `loadConfigFile supports injected property overrides for deterministic tests`() {
        val emptyDotenvDir = Files.createTempDirectory("root-config-empty-dotenv")
        val dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .directory(emptyDotenvDir.toString())
            .load()

        val config = RootConfig.loadConfigFile(
            dotenv = dotenv,
            propertyOverrides = mapOf(
                "jwt.secret" to "injected-secret",
                "google.clientId" to "injected-client-id",
                "google.clientSecret" to "injected-client-secret",
                "database.jdbcUrl" to "injected-database-jdbc-url",
                "database.username" to "injected-database-username",
                "database.password" to "injected-database-password",
            )
        )

        assertEquals("injected-secret", config.jwt.secret)
        assertEquals("injected-client-id", config.google.clientId)
        assertEquals("injected-client-secret", config.google.clientSecret)
        assertEquals("injected-database-jdbc-url", config.database.jdbcUrl)
        assertEquals("injected-database-username", config.database.username)
        assertEquals("injected-database-password", config.database.password)
    }
}
