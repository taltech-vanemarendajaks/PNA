package com.pna.backend.services

import com.pna.backend.dal.repositories.RefreshTokenRecord
import com.pna.backend.dal.repositories.RefreshTokenRepository
import domain.auth.GoogleUser
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.util.Base64
import java.util.UUID

data class RefreshTokenRotation(
    val refreshToken: String,
    val user: GoogleUser
)

class RefreshTokenService(
    private val repository: RefreshTokenRepository,
    private val ttlSeconds: Int,
    private val clock: Clock = Clock.systemUTC()
) {
    fun createRefreshToken(user: GoogleUser, metadata: SessionClientMetadata): String {
        val refreshToken = generateToken()
        repository.save(buildRecord(refreshToken, UUID.randomUUID().toString(), user, metadata))
        return refreshToken
    }

    fun rotateRefreshToken(refreshToken: String, metadata: SessionClientMetadata): RefreshTokenRotation? {
        val tokenHash = hashToken(refreshToken)
        val record = repository.findByHash(tokenHash) ?: return null
        val now = clock.instant()

        if (record.revokedAt != null || record.isExpired(now)) {
            repository.revokeFamily(record.familyId, now.toString())
            return null
        }

        if (record.replacedByTokenId != null) {
            repository.revokeFamily(record.familyId, now.toString())
            return null
        }

        val replacementToken = generateToken()
        val replacementRecord = buildRecord(
            replacementToken,
            record.familyId,
            record.user,
            metadata,
            record.userId
        )

        if (!repository.rotate(record.id, record.tokenHash, replacementRecord, now)) {
            return null
        }

        return RefreshTokenRotation(refreshToken = replacementToken, user = record.user)
    }

    fun revokeRefreshToken(refreshToken: String?) {
        if (refreshToken.isNullOrBlank()) {
            return
        }

        val record = repository.findByHash(hashToken(refreshToken)) ?: return
        repository.revokeFamily(record.familyId, clock.instant().toString())
    }

    private fun buildRecord(
        refreshToken: String,
        familyId: String,
        user: GoogleUser,
        metadata: SessionClientMetadata,
        userId: String? = null
    ): RefreshTokenRecord {
        val now = clock.instant()
        return RefreshTokenRecord(
            UUID.randomUUID().toString(),
            userId,
            user,
            hashToken(refreshToken),
            familyId,
            now.plusSeconds(ttlSeconds.toLong()).toString(),
            now.toString(),
            null,
            null,
            null,
            metadata.userAgent,
            metadata.ipAddress
        )
    }

    private fun generateToken(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }
}
