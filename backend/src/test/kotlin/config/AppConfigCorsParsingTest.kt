package config

import com.pna.backend.config.AppConfig
import com.pna.backend.config.CorsOrigin
import kotlin.test.Test
import kotlin.test.assertEquals

class AppConfigCorsParsingTest {
    @Test
    fun `parseOrigins preserves port in allowed origins`() {
        val allowedOrigins = AppConfig.parseOrigins(
            originsRaw = "http://localhost:4173,https://example.com",
            frontendBaseUrl = "http://localhost:5173"
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
        val allowedOrigins = AppConfig.parseOrigins(
            originsRaw = null,
            frontendBaseUrl = "http://localhost:5173"
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
        val allowedOrigins = AppConfig.parseOrigins(
            originsRaw = "http://localhost:5173,https://example.com,https://example.com",
            frontendBaseUrl = "http://localhost:5173"
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
        val allowedOrigins = AppConfig.parseOrigins(
            originsRaw = "http://localhost:4173",
            frontendBaseUrl = "http://localhost:5173"
        )

        assertEquals(true, AppConfig.isAllowedOrigin("http://localhost:5173", allowedOrigins))
        assertEquals(true, AppConfig.isAllowedOrigin("http://localhost:4173/", allowedOrigins))
        assertEquals(false, AppConfig.isAllowedOrigin("https://localhost:5173", allowedOrigins))
        assertEquals(false, AppConfig.isAllowedOrigin("http://evil.example", allowedOrigins))
        assertEquals(false, AppConfig.isAllowedOrigin("not-a-url", allowedOrigins))
    }

    @Test
    fun `normalizeSameSite returns expected values and defaults to lax`() {
        assertEquals("Lax", AppConfig.normalizeSameSite("Lax"))
        assertEquals("Strict", AppConfig.normalizeSameSite("strict"))
        assertEquals("None", AppConfig.normalizeSameSite("NONE"))
        assertEquals("Lax", AppConfig.normalizeSameSite("something-else"))
    }

    @Test
    fun `normalizeBaseUrl trims trailing slash`() {
        assertEquals(
            "https://api.example.com",
            AppConfig.normalizeBaseUrl("https://api.example.com/")
        )
    }
}