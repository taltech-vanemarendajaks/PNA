package com.pna.backend.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AppConfigCorsParsingTest {
    @Test
    fun `parseOrigins preserves port in allowed origins`() {
        val allowedOrigins = AppConfig.parseOrigins(
            originsRaw = "http://localhost:4173,https://example.com",
            frontendBaseUrl = "http://localhost:5173",
            allowAnyHost = false
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
    fun `validateCookieAndCorsConfig rejects wildcard CORS with cookie auth`() {
        assertFailsWith<IllegalArgumentException> {
            AppConfig.validateCookieAndCorsConfig(
                allowAnyHost = true,
                authCookieSecure = false,
                authCookieSameSite = AppConfig.normalizeSameSite("Lax")
            )
        }
    }

    @Test
    fun `parseOrigins rejects origins with path query fragment or user info`() {
        listOf(
            "http://localhost:4173/app",
            "http://localhost:4173?x=1",
            "http://localhost:4173#frag",
            "http://user@localhost:4173"
        ).forEach { origin ->
            assertFailsWith<IllegalArgumentException> {
                AppConfig.parseOrigins(
                    originsRaw = origin,
                    frontendBaseUrl = "http://localhost:5173",
                    allowAnyHost = false
                )
            }
        }
    }

    @Test
    fun `isAllowedOrigin accepts exact configured origin and rejects malformed values`() {
        val allowedOrigins = AppConfig.parseOrigins(
            originsRaw = "http://localhost:4173",
            frontendBaseUrl = "http://localhost:5173",
            allowAnyHost = false
        )

        assertEquals(true, AppConfig.isAllowedOrigin("http://localhost:5173", allowedOrigins))
        assertEquals(true, AppConfig.isAllowedOrigin("http://localhost:4173/", allowedOrigins))
        assertEquals(false, AppConfig.isAllowedOrigin("https://localhost:5173", allowedOrigins))
        assertEquals(false, AppConfig.isAllowedOrigin("http://localhost:5173/app", allowedOrigins))
        assertEquals(false, AppConfig.isAllowedOrigin("http://evil.example", allowedOrigins))
    }

    @Test
    fun `normalizeBaseUrl trims trailing slash and rejects paths`() {
        assertEquals(
            "https://api.example.com",
            AppConfig.normalizeBaseUrl("https://api.example.com/", "PUBLIC_BACKEND_BASE_URL")
        )

        assertFailsWith<IllegalArgumentException> {
            AppConfig.normalizeBaseUrl("https://api.example.com/callback", "PUBLIC_BACKEND_BASE_URL")
        }
    }
}
