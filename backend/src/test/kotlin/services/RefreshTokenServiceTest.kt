package services

import com.pna.backend.dal.repositories.RefreshTokenRepository
import com.pna.backend.services.RefreshTokenService
import domain.auth.GoogleUser
import java.nio.file.Files
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RefreshTokenServiceTest {
    @Test
    fun `rotates refresh tokens and rejects replayed tokens`() {
        val service = newService()
        val initialToken = service.createRefreshToken(user())

        val rotated = service.rotateRefreshToken(initialToken)

        assertNotNull(rotated)
        assertEquals(user().subject, rotated.user.subject)
        assertNull(service.rotateRefreshToken(initialToken))
        assertNull(service.rotateRefreshToken(rotated.refreshToken))
    }

    @Test
    fun `revokes refresh tokens on logout`() {
        val service = newService()
        val refreshToken = service.createRefreshToken(user())

        service.revokeRefreshToken(refreshToken)

        assertNull(service.rotateRefreshToken(refreshToken))
    }

    private fun newService(): RefreshTokenService {
        val databasePath = Files.createTempFile("refresh-token-service-test", ".db").toString()
        return RefreshTokenService(
            repository = RefreshTokenRepository(databasePath),
            ttlSeconds = 3600,
            clock = Clock.fixed(Instant.parse("2026-04-17T18:00:00Z"), ZoneOffset.UTC)
        )
    }

    private fun user(): GoogleUser = GoogleUser("subject", "user@example.com", "Jane", "Jane")
}
