package com.pna.backend.dal.repositories

import domain.auth.GoogleUser
import java.sql.DriverManager
import java.time.Instant

data class RefreshTokenRecord(
    val tokenHash: String,
    val familyId: String,
    val subject: String,
    val email: String?,
    val name: String?,
    val givenName: String?,
    val expiresAt: String,
    val revokedAt: String?,
    val replacedByTokenHash: String?
) {
    fun toUser(): GoogleUser = GoogleUser(
        subject = subject,
        email = email,
        name = name,
        givenName = givenName
    )

    fun isExpired(now: Instant): Boolean = runCatching { Instant.parse(expiresAt) }
        .getOrDefault(Instant.EPOCH)
        .let { !it.isAfter(now) }
}

class RefreshTokenRepository(
    databasePath: String = "refresh-tokens.db"
) {
    private val jdbcUrl = "jdbc:sqlite:$databasePath"
    private val expectedColumns = listOf(
        "token_hash",
        "family_id",
        "subject",
        "email",
        "name",
        "given_name",
        "expires_at",
        "revoked_at",
        "replaced_by_token_hash"
    )

    init {
        DriverManager.getConnection(jdbcUrl).use { connection ->
            ensureTable(connection)
            migrateSchemaIfNeeded(connection)
        }
    }

    fun save(record: RefreshTokenRecord) {
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.prepareStatement(
                """
                INSERT INTO refresh_tokens(
                    token_hash, family_id, subject, email, name, given_name, expires_at, revoked_at, replaced_by_token_hash
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                bindRecord(statement, record)
                statement.executeUpdate()
            }
        }
    }

    fun findByHash(tokenHash: String): RefreshTokenRecord? {
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.prepareStatement(
                """
                SELECT token_hash, family_id, subject, email, name, given_name, expires_at, revoked_at, replaced_by_token_hash
                FROM refresh_tokens
                WHERE token_hash = ?
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

    fun rotate(currentTokenHash: String, replacement: RefreshTokenRecord): Boolean {
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.autoCommit = false

            try {
                val updatedRows = connection.prepareStatement(
                    """
                    UPDATE refresh_tokens
                    SET replaced_by_token_hash = ?
                    WHERE token_hash = ? AND revoked_at IS NULL AND replaced_by_token_hash IS NULL
                    """.trimIndent()
                ).use { statement ->
                    statement.setString(1, replacement.tokenHash)
                    statement.setString(2, currentTokenHash)
                    statement.executeUpdate()
                }

                if (updatedRows != 1) {
                    connection.rollback()
                    return false
                }

                connection.prepareStatement(
                    """
                    INSERT INTO refresh_tokens(
                        token_hash, family_id, subject, email, name, given_name, expires_at, revoked_at, replaced_by_token_hash
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { statement ->
                    bindRecord(statement, replacement)
                    statement.executeUpdate()
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
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.prepareStatement(
                "UPDATE refresh_tokens SET revoked_at = COALESCE(revoked_at, ?) WHERE family_id = ?"
            ).use { statement ->
                statement.setString(1, revokedAt)
                statement.setString(2, familyId)
                statement.executeUpdate()
            }
        }
    }

    private fun bindRecord(statement: java.sql.PreparedStatement, record: RefreshTokenRecord) {
        statement.setString(1, record.tokenHash)
        statement.setString(2, record.familyId)
        statement.setString(3, record.subject)
        statement.setString(4, record.email)
        statement.setString(5, record.name)
        statement.setString(6, record.givenName)
        statement.setString(7, record.expiresAt)
        statement.setString(8, record.revokedAt)
        statement.setString(9, record.replacedByTokenHash)
    }

    private fun mapRecord(resultSet: java.sql.ResultSet): RefreshTokenRecord = RefreshTokenRecord(
        tokenHash = resultSet.getString("token_hash"),
        familyId = resultSet.getString("family_id"),
        subject = resultSet.getString("subject"),
        email = resultSet.getString("email"),
        name = resultSet.getString("name"),
        givenName = resultSet.getString("given_name"),
        expiresAt = resultSet.getString("expires_at"),
        revokedAt = resultSet.getString("revoked_at"),
        replacedByTokenHash = resultSet.getString("replaced_by_token_hash")
    )

    private fun ensureTable(connection: java.sql.Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate(
                """
                CREATE TABLE IF NOT EXISTS refresh_tokens (
                    token_hash TEXT PRIMARY KEY,
                    family_id TEXT NOT NULL,
                    subject TEXT NOT NULL,
                    email TEXT,
                    name TEXT,
                    given_name TEXT,
                    expires_at TEXT NOT NULL,
                    revoked_at TEXT,
                    replaced_by_token_hash TEXT
                )
                """.trimIndent()
            )
        }
    }

    private fun migrateSchemaIfNeeded(connection: java.sql.Connection) {
        val existingColumns = connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(refresh_tokens)").use { resultSet ->
                buildList {
                    while (resultSet.next()) {
                        add(resultSet.getString("name"))
                    }
                }
            }
        }

        if (existingColumns == expectedColumns) {
            return
        }

        connection.autoCommit = false
        try {
            connection.createStatement().use { statement ->
                statement.executeUpdate("DROP TABLE IF EXISTS refresh_tokens_v2")
                statement.executeUpdate(
                    """
                    CREATE TABLE refresh_tokens_v2 (
                        token_hash TEXT PRIMARY KEY,
                        family_id TEXT NOT NULL,
                        subject TEXT NOT NULL,
                        email TEXT,
                        name TEXT,
                        given_name TEXT,
                        expires_at TEXT NOT NULL,
                        revoked_at TEXT,
                        replaced_by_token_hash TEXT
                    )
                    """.trimIndent()
                )
                statement.executeUpdate(
                    """
                    INSERT INTO refresh_tokens_v2(
                        token_hash, family_id, subject, email, name, given_name, expires_at, revoked_at, replaced_by_token_hash
                    )
                    SELECT
                        token_hash,
                        family_id,
                        subject,
                        email,
                        name,
                        given_name,
                        expires_at,
                        revoked_at,
                        replaced_by_token_hash
                    FROM refresh_tokens
                    """.trimIndent()
                )
                statement.executeUpdate("DROP TABLE refresh_tokens")
                statement.executeUpdate("ALTER TABLE refresh_tokens_v2 RENAME TO refresh_tokens")
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
