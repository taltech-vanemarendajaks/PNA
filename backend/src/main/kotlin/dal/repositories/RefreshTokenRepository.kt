package com.pna.backend.dal.repositories

import com.pna.backend.dal.Database
import domain.auth.GoogleUser
import java.sql.ResultSet
import java.time.Instant
import java.time.ZoneOffset

data class RefreshTokenRecord(
    val id: String,
    val userId: String?,
    val user: GoogleUser,
    val tokenHash: String,
    val familyId: String,
    val expiresAt: String,
    val createdAt: String,
    val lastUsedAt: String?,
    val revokedAt: String?,
    val replacedByTokenId: String?,
    val userAgent: String?,
    val ipAddress: String?
) {
    fun isExpired(now: Instant): Boolean = runCatching { Instant.parse(expiresAt) }
        .getOrDefault(Instant.EPOCH)
        .let { !it.isAfter(now) }
}

class RefreshTokenRepository(
    private val database: Database,
    private val userRepository: UserRepository
) {
    fun save(record: RefreshTokenRecord) {
        database.dataSource.connection.use { connection ->
            connection.autoCommit = false

            try {
                val userId = record.userId ?: userRepository.upsertUser(connection, record.user, record.createdAt)

                connection.prepareStatement(
                    """
                    INSERT INTO refresh_tokens(
                        id, user_id, family_id, token_hash, expires_at, created_at, last_used_at, revoked_at,
                        replaced_by_token_id, user_agent, ip_address
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, record.id)
                    statement.setString(2, userId)
                    statement.setString(3, record.familyId)
                    statement.setString(4, record.tokenHash)
                    statement.setObject(5, parseInstant(record.expiresAt))
                    statement.setObject(6, parseInstant(record.createdAt))
                    statement.setObject(7, record.lastUsedAt?.let(::parseInstant))
                    statement.setObject(8, record.revokedAt?.let(::parseInstant))
                    statement.setString(9, record.replacedByTokenId)
                    statement.setString(10, record.userAgent)
                    statement.setString(11, record.ipAddress)
                    statement.executeUpdate()
                }

                connection.commit()
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun findByHash(tokenHash: String): RefreshTokenRecord? {
        database.dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT
                    rt.id,
                    rt.user_id,
                    rt.family_id,
                    rt.token_hash,
                    rt.expires_at,
                    rt.created_at,
                    rt.last_used_at,
                    rt.revoked_at,
                    rt.replaced_by_token_id,
                    rt.user_agent,
                    rt.ip_address,
                    u.subject,
                    u.email,
                    u.name,
                    u.given_name
                FROM refresh_tokens rt
                JOIN users u ON u.id = rt.user_id
                WHERE rt.token_hash = ?
                LIMIT 1
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, tokenHash)
                statement.executeQuery().use { resultSet ->
                    if (!resultSet.next()) {
                        return null
                    }

                    return mapRecord(resultSet)
                }
            }
        }
    }

    fun rotate(currentTokenId: String, currentTokenHash: String, replacement: RefreshTokenRecord, rotatedAt: Instant): Boolean {
        database.dataSource.connection.use { connection ->
            connection.autoCommit = false

            try {
                val userId = replacement.userId ?: userRepository.upsertUser(connection, replacement.user, replacement.createdAt)
                connection.prepareStatement(
                    """
                    INSERT INTO refresh_tokens(
                        id, user_id, family_id, token_hash, expires_at, created_at, last_used_at, revoked_at,
                        replaced_by_token_id, user_agent, ip_address
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, replacement.id)
                    statement.setString(2, userId)
                    statement.setString(3, replacement.familyId)
                    statement.setString(4, replacement.tokenHash)
                    statement.setObject(5, parseInstant(replacement.expiresAt))
                    statement.setObject(6, parseInstant(replacement.createdAt))
                    statement.setObject(7, replacement.lastUsedAt?.let(::parseInstant))
                    statement.setObject(8, replacement.revokedAt?.let(::parseInstant))
                    statement.setString(9, replacement.replacedByTokenId)
                    statement.setString(10, replacement.userAgent)
                    statement.setString(11, replacement.ipAddress)
                    statement.executeUpdate()
                }

                val updatedRows = connection.prepareStatement(
                    """
                    UPDATE refresh_tokens
                    SET replaced_by_token_id = ?, last_used_at = ?
                    WHERE id = ? AND token_hash = ? AND revoked_at IS NULL AND replaced_by_token_id IS NULL
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, replacement.id)
                    statement.setObject(2, rotatedAt.atOffset(ZoneOffset.UTC))
                    statement.setString(3, currentTokenId)
                    statement.setString(4, currentTokenHash)
                    statement.executeUpdate()
                }

                if (updatedRows != 1) {
                    connection.rollback()
                    return false
                }

                connection.commit()
                return true
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun revokeFamily(familyId: String, revokedAt: String) {
        database.dataSource.connection.use { connection ->
            connection.prepareStatement(
                "UPDATE refresh_tokens SET revoked_at = COALESCE(revoked_at, ?) WHERE family_id = ?"
            ).use { statement ->
                statement.setObject(1, parseInstant(revokedAt))
                statement.setString(2, familyId)
                statement.executeUpdate()
            }
        }
    }

    private fun mapRecord(resultSet: ResultSet): RefreshTokenRecord {
        return RefreshTokenRecord(
            id = resultSet.getString("id"),
            userId = resultSet.getString("user_id"),
            user = GoogleUser(
                subject = resultSet.getString("subject"),
                email = resultSet.getString("email"),
                name = resultSet.getString("name"),
                givenName = resultSet.getString("given_name")
            ),
            tokenHash = resultSet.getString("token_hash"),
            familyId = resultSet.getString("family_id"),
            expiresAt = resultSet.getObject("expires_at", java.time.OffsetDateTime::class.java).toInstant().toString(),
            createdAt = resultSet.getObject("created_at", java.time.OffsetDateTime::class.java).toInstant().toString(),
            lastUsedAt = resultSet.getObject("last_used_at", java.time.OffsetDateTime::class.java)?.toInstant()?.toString(),
            revokedAt = resultSet.getObject("revoked_at", java.time.OffsetDateTime::class.java)?.toInstant()?.toString(),
            replacedByTokenId = resultSet.getString("replaced_by_token_id"),
            userAgent = resultSet.getString("user_agent"),
            ipAddress = resultSet.getString("ip_address")
        )
    }

    private fun parseInstant(value: String) = java.time.OffsetDateTime.parse(value)
}
