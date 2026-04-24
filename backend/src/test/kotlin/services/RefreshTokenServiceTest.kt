package services

import com.pna.backend.dal.Database
import com.pna.backend.dal.repositories.RefreshTokenRepository
import com.pna.backend.dal.repositories.UserRepository
import com.pna.backend.services.RefreshTokenService
import com.pna.backend.services.SessionClientMetadata
import domain.auth.GoogleUser
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import support.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RefreshTokenServiceTest {
    @Test
    fun `rotates refresh tokens and rejects replayed tokens`() {
        TestDatabase.newDatabase("refresh_token_service_rotate").use { database ->
            database.migrate()

            val service = newService(database)
            val initialToken = service.createRefreshToken(user(), metadata())

            val rotated = service.rotateRefreshToken(initialToken, metadata(ipAddress = "10.0.0.2"))

            assertNotNull(rotated)
            assertEquals(user().subject, rotated.user.subject)
            assertNull(service.rotateRefreshToken(initialToken, metadata()))
            assertNull(service.rotateRefreshToken(rotated.refreshToken, metadata()))
        }
    }

    @Test
    fun `revokes refresh tokens on logout`() {
        TestDatabase.newDatabase("refresh_token_service_revoke").use { database ->
            database.migrate()

            val service = newService(database)
            val refreshToken = service.createRefreshToken(user(), metadata())

            service.revokeRefreshToken(refreshToken)

            assertNull(service.rotateRefreshToken(refreshToken, metadata()))
        }
    }

    private fun newService(database: Database): RefreshTokenService {
        val userRepository = UserRepository(database)

        return RefreshTokenService(
            repository = RefreshTokenRepository(database, userRepository),
            ttlSeconds = 3600,
            clock = Clock.fixed(Instant.parse("2026-04-17T18:00:00Z"), ZoneOffset.UTC)
        )
    }

    private fun metadata(userAgent: String? = "JUnit", ipAddress: String? = "127.0.0.1") =
        SessionClientMetadata(userAgent = userAgent, ipAddress = ipAddress)

    private fun user(): GoogleUser = GoogleUser("subject", "user@example.com", "Jane", "Jane")
}