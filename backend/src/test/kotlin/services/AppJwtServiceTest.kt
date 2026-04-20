package services

import com.pna.backend.services.AppJwtService
import domain.auth.GoogleUser
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppJwtServiceTest {
    @Test
    fun `issues and verifies access tokens with user claims`() {
        val issuedAt = Instant.now().minusSeconds(5)
        val service = AppJwtService(
            secret = "test-secret",
            issuer = "https://api.example.com",
            audience = "pna-clients",
            ttlSeconds = 60,
            clock = java.time.Clock.fixed(issuedAt, java.time.ZoneOffset.UTC)
        )

        val token = service.issueAccessToken(user())
        val verifiedUser = service.verify(token)

        assertEquals(user().subject, verifiedUser?.subject)
        assertEquals(user().email, verifiedUser?.email)
        assertEquals(user().name, verifiedUser?.name)
        assertEquals(user().givenName, verifiedUser?.givenName)
    }

    @Test
    fun `rejects expired access tokens`() {
        val issuedAt = Instant.now().minusSeconds(120)
        val token = AppJwtService(
            secret = "test-secret",
            issuer = "https://api.example.com",
            audience = "pna-clients",
            ttlSeconds = 60,
            clock = java.time.Clock.fixed(issuedAt, java.time.ZoneOffset.UTC)
        ).issueAccessToken(user())

        val expiredVerifier = AppJwtService(
            secret = "test-secret",
            issuer = "https://api.example.com",
            audience = "pna-clients",
            ttlSeconds = 60,
            clock = java.time.Clock.systemUTC()
        )

        assertNull(expiredVerifier.verify(token))
    }

    private fun user(): GoogleUser = GoogleUser("subject", "user@example.com", "Jane", "Jane")
}
