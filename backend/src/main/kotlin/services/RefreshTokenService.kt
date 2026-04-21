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
    fun createRefreshToken(user: GoogleUser): String {
        val refreshToken = generateToken()
        repository.save(buildRecord(refreshToken, UUID.randomUUID().toString(), user))
        return refreshToken
    }

    fun rotateRefreshToken(refreshToken: String): RefreshTokenRotation? {
        val tokenHash = hashToken(refreshToken)
        val record = repository.findByHash(tokenHash) ?: return null
        val now = clock.instant()

        if (record.revokedAt != null || record.isExpired(now)) {
            repository.revokeFamily(record.familyId, now.toString())
            return null
        }

        if (record.replacedByTokenHash != null) {
            repository.revokeFamily(record.familyId, now.toString())
            return null
        }

        val replacementToken = generateToken()
        val replacementRecord = buildRecord(replacementToken, record.familyId, record.toUser())

        if (!repository.rotate(record.tokenHash, replacementRecord)) {
            return null
        }

        return RefreshTokenRotation(refreshToken = replacementToken, user = record.toUser())
    }

    fun revokeRefreshToken(refreshToken: String?) {
        if (refreshToken.isNullOrBlank()) {
            return
        }

        val record = repository.findByHash(hashToken(refreshToken)) ?: return
        repository.revokeFamily(record.familyId, clock.instant().toString())
    }

    private fun buildRecord(refreshToken: String, familyId: String, user: GoogleUser): RefreshTokenRecord {
        return RefreshTokenRecord(
            tokenHash = hashToken(refreshToken),
            familyId = familyId,
            subject = user.subject,
            email = user.email,
            name = user.name,
            givenName = user.givenName,
            expiresAt = clock.instant().plusSeconds(ttlSeconds.toLong()).toString(),
            revokedAt = null,
            replacedByTokenHash = null
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
