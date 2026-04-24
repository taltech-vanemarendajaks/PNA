package dal.repositories

import com.pna.backend.dal.repositories.RefreshTokenRecord
import com.pna.backend.dal.repositories.RefreshTokenRepository
import com.pna.backend.dal.repositories.UserRepository
import domain.auth.GoogleUser
import java.time.Instant
import support.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RefreshTokenRepositoryTest {
    @Test
    fun `rotate stores last used at for the previous token and creates the replacement token`() {
        TestDatabase.newDatabase("refresh_token_repository_rotate").use { database ->
            database.migrate()

            val userRepository = UserRepository(database)
            val repository = RefreshTokenRepository(database, userRepository)

            val initialRecord = record(
                id = "token-1",
                tokenHash = "hash-1",
                familyId = "family-1",
                createdAt = "2026-04-17T18:00:00Z",
                expiresAt = "2026-04-17T19:00:00Z"
            )
            val replacementRecord = record(
                id = "token-2",
                tokenHash = "hash-2",
                familyId = "family-1",
                createdAt = "2026-04-17T18:05:00Z",
                expiresAt = "2026-04-17T19:05:00Z",
                userAgent = "JUnit Rotated",
                ipAddress = "10.0.0.2"
            )
            val rotatedAt = Instant.parse("2026-04-17T18:05:00Z")

            repository.save(initialRecord)

            val rotated = repository.rotate(initialRecord.id, initialRecord.tokenHash, replacementRecord, rotatedAt)

            assertTrue(rotated)

            val previous = repository.findByHash(initialRecord.tokenHash)
            assertNotNull(previous)
            assertEquals(replacementRecord.id, previous.replacedByTokenId)
            assertEquals(rotatedAt.toString(), previous.lastUsedAt)

            val replacement = repository.findByHash(replacementRecord.tokenHash)
            assertNotNull(replacement)
            assertEquals(replacementRecord.id, replacement.id)
            assertNull(replacement.lastUsedAt)
            assertEquals(replacementRecord.userAgent, replacement.userAgent)
            assertEquals(replacementRecord.ipAddress, replacement.ipAddress)
        }
    }

    private fun record(
        id: String,
        tokenHash: String,
        familyId: String,
        createdAt: String,
        expiresAt: String,
        userAgent: String? = "JUnit",
        ipAddress: String? = "127.0.0.1"
    ): RefreshTokenRecord {
        return RefreshTokenRecord(
            id = id,
            userId = null,
            user = GoogleUser(
                subject = "subject-1",
                email = "user@example.com",
                name = "Jane Doe",
                givenName = "Jane"
            ),
            tokenHash = tokenHash,
            familyId = familyId,
            expiresAt = expiresAt,
            createdAt = createdAt,
            lastUsedAt = null,
            revokedAt = null,
            replacedByTokenId = null,
            userAgent = userAgent,
            ipAddress = ipAddress
        )
    }
}