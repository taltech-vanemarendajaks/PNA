package com.pna.backend.dal.repositories

import com.pna.backend.domain.auth.response.PhoneNumberLookupResult
import com.pna.backend.domain.auth.response.SavedNumberSearchResponse
import java.sql.DriverManager
import java.time.Instant
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NumberSearchRepository(
    databasePath: String = System.getenv("NUMBER_SEARCH_DB_PATH")
        ?.trim()
        ?.ifBlank { null }
        ?: "number-searches.db",
    private val json: Json = Json
) {
    private val jdbcUrl = "jdbc:sqlite:$databasePath"

    init {
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS number_searches (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        number TEXT NOT NULL,
                        result_json TEXT NOT NULL,
                        created_at TEXT NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }

    fun save(number: String, result: PhoneNumberLookupResult) {
        val normalizedNumber = number.trim()
        val createdAt = Instant.now().toString()
        val resultJson = json.encodeToString(result)

        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.prepareStatement(
                "INSERT INTO number_searches(number, result_json, created_at) VALUES (?, ?, ?)"
            ).use { statement ->
                statement.setString(1, normalizedNumber)
                statement.setString(2, resultJson)
                statement.setString(3, createdAt)
                statement.executeUpdate()
            }
        }
    }

    fun findLatestByNumber(number: String): SavedNumberSearchResponse? {
        val normalizedNumber = number.trim()
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.prepareStatement(
                "SELECT id, number, result_json, created_at FROM number_searches WHERE number = ? ORDER BY id DESC LIMIT 1"
            ).use { statement ->
                statement.setString(1, normalizedNumber)
                statement.executeQuery().use { resultSet ->
                    if (!resultSet.next()) {
                        return null
                    }

                    return SavedNumberSearchResponse(
                        id = resultSet.getLong("id"),
                        number = resultSet.getString("number"),
                        result = runCatching {
                            json.decodeFromString<PhoneNumberLookupResult>(resultSet.getString("result_json"))
                        }.getOrDefault(PhoneNumberLookupResult()),
                        createdAt = resultSet.getString("created_at")
                    )
                }
            }
        }
    }

    fun findAll(): List<SavedNumberSearchResponse> {
        DriverManager.getConnection(jdbcUrl).use { connection ->
            connection.prepareStatement(
                "SELECT id, number, result_json, created_at FROM number_searches ORDER BY id DESC"
            ).use { statement ->
                statement.executeQuery().use { resultSet ->
                    val rows = mutableListOf<SavedNumberSearchResponse>()
                    while (resultSet.next()) {
                        rows += SavedNumberSearchResponse(
                            id = resultSet.getLong("id"),
                            number = resultSet.getString("number"),
                            result = runCatching {
                                json.decodeFromString<PhoneNumberLookupResult>(resultSet.getString("result_json"))
                            }.getOrDefault(PhoneNumberLookupResult()),
                            createdAt = resultSet.getString("created_at")
                        )
                    }
                    return rows
                }
            }
        }
    }
}
