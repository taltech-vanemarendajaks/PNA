package com.pna.backend.dal.repositories

import com.pna.backend.dal.Database
import domain.auth.GoogleUser
import java.sql.Connection
import java.time.Instant
import java.time.OffsetDateTime

class UserRepository(private val database: Database) {
    fun upsertUser(user: GoogleUser): String {
        val createdAt = Instant.now().toString()

        database.dataSource.connection.use { connection ->
            connection.autoCommit = false

            try {
                val userId = upsertUser(connection, user, createdAt)
                connection.commit()
                return userId
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun upsertUser(connection: Connection, user: GoogleUser, createdAt: String): String {
        val userId = java.util.UUID.randomUUID().toString()

        connection.prepareStatement(
            "SELECT id FROM users WHERE subject = ? LIMIT 1"
        ).use { statement ->
            statement.setString(1, user.subject)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    val userId = resultSet.getString("id")
                    connection.prepareStatement(
                        "UPDATE users SET email = ?, name = ?, given_name = ? WHERE id = ?"
                    ).use { updateStatement ->
                        updateStatement.setString(1, user.email)
                        updateStatement.setString(2, user.name)
                        updateStatement.setString(3, user.givenName)
                        updateStatement.setString(4, userId)
                        updateStatement.executeUpdate()
                    }
                    return userId
                }
            }
        }

        connection.prepareStatement(
            """
                INSERT INTO users(id, subject, email, name, given_name, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (subject)
                DO UPDATE SET
                    email = EXCLUDED.email,
                    name = EXCLUDED.name,
                    given_name = EXCLUDED.given_name
                RETURNING id
                """.trimIndent()
        ).use { statement ->
            statement.setString(1, userId)
            statement.setString(2, user.subject)
            statement.setString(3, user.email)
            statement.setString(4, user.name)
            statement.setString(5, user.givenName)
            statement.setObject(6, OffsetDateTime.parse(createdAt))
            statement.executeQuery().use { resultSet ->
                check(resultSet.next()) { "Upsert user did not return an id" }
                return resultSet.getString("id")
            }
        }
    }
}
