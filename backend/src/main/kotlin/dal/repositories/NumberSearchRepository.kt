package com.pna.backend.dal.repositories

import com.pna.backend.dal.Database
import com.pna.backend.domain.auth.response.PhoneNumberLookupResult
import com.pna.backend.domain.auth.response.SavedNumberSearchResponse
import com.pna.backend.domain.auth.response.SavedPhoneNumberLookupResult
import domain.auth.GoogleUser
import java.security.MessageDigest
import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Base64
import java.util.UUID

class NumberSearchRepository(
    private val database: Database,
    private val userRepository: UserRepository
) {
    fun save(user: GoogleUser, number: String, result: PhoneNumberLookupResult) {
        val rawInput = number.trim()
        val createdAt = Instant.now().toString()
        val normalizedPhone = normalizePhoneNumber(rawInput, result)

        database.dataSource.connection.use { connection ->
            connection.autoCommit = false

            try {
                val userId = userRepository.upsertUser(connection, user, createdAt)
                val phoneNumberId = ensurePhoneNumber(connection, normalizedPhone, createdAt)
                ensurePhoneNumberResult(connection, phoneNumberId, result, createdAt)
                insertUserSearch(connection, userId, phoneNumberId, rawInput, createdAt)

                connection.commit()
            } catch (error: Throwable) {
                connection.rollback()
                throw error
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun findAllByUser(user: GoogleUser): List<SavedNumberSearchResponse> {
        database.dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT
                    us.id,
                    us.raw_input,
                    us.created_at,
                    pr.country,
                    pr.country_code,
                    pr.region_code,
                    pr.number_type,
                    pr.international_format,
                    pr.carrier,
                    pr.time_zones,
                    pr.created_at AS result_created_at
                FROM user_search us
                JOIN users u ON u.id = us.user_id
                LEFT JOIN phone_number_results pr ON pr.phone_number_id = us.phone_number_id
                WHERE u.subject = ?
                ORDER BY us.created_at DESC, us.id DESC, pr.created_at DESC NULLS LAST, pr.id DESC
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, user.subject)
                statement.executeQuery().use { resultSet ->
                    val rows = linkedMapOf<String, SavedSearchAccumulator>()
                    while (resultSet.next()) {
                        val searchId = resultSet.getString("id")
                        val accumulator = rows.getOrPut(searchId) {
                            SavedSearchAccumulator(
                                id = searchId,
                                number = resultSet.getString("raw_input"),
                                createdAt = resultSet.getObject("created_at", OffsetDateTime::class.java).toInstant().toString(),
                            )
                        }

                        mapSavedResult(resultSet)?.let { accumulator.results += it }
                    }

                    return rows.values.map { search ->
                        SavedNumberSearchResponse(
                            id = search.id,
                            number = search.number,
                            results = search.results,
                            createdAt = search.createdAt,
                        )
                    }
                }
            }
        }
    }

    fun deleteById(user: GoogleUser, searchId: String): Boolean {
        database.dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                DELETE FROM user_search us
                USING users u
                WHERE us.user_id = u.id
                  AND u.subject = ?
                  AND us.id = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, user.subject)
                statement.setString(2, searchId)
                return statement.executeUpdate() == 1
            }
        }
    }

    fun deleteAllByUser(user: GoogleUser): Int {
        database.dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                DELETE FROM user_search us
                USING users u
                WHERE us.user_id = u.id
                  AND u.subject = ?
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, user.subject)
                return statement.executeUpdate()
            }
        }
    }

    private fun ensurePhoneNumber(connection: Connection, normalizedPhone: NormalizedPhoneNumber, createdAt: String): String {
        connection.prepareStatement(
            "SELECT id, e164_number FROM phone_number WHERE canonical_number = ? LIMIT 1"
        ).use { statement ->
            statement.setString(1, normalizedPhone.canonicalNumber)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    val phoneNumberId = resultSet.getString("id")
                    val existingE164 = resultSet.getString("e164_number")
                    if (existingE164.isNullOrBlank() && !normalizedPhone.e164Number.isNullOrBlank()) {
                        connection.prepareStatement(
                            "UPDATE phone_number SET e164_number = ? WHERE id = ?"
                        ).use { updateStatement ->
                            updateStatement.setString(1, normalizedPhone.e164Number)
                            updateStatement.setString(2, phoneNumberId)
                            updateStatement.executeUpdate()
                        }
                    }
                    return phoneNumberId
                }
            }
        }

        val phoneNumberId = UUID.randomUUID().toString()
        connection.prepareStatement(
            """
            INSERT INTO phone_number(id, canonical_number, e164_number, created_at)
            VALUES (?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, phoneNumberId)
            statement.setString(2, normalizedPhone.canonicalNumber)
            statement.setString(3, normalizedPhone.e164Number)
            statement.setObject(4, OffsetDateTime.parse(createdAt))
            statement.executeUpdate()
        }

        return phoneNumberId
    }

    private fun ensurePhoneNumberResult(
        connection: Connection,
        phoneNumberId: String,
        result: PhoneNumberLookupResult,
        createdAt: String
    ): String {
        val resultHash = computeResultHash(result)

        connection.prepareStatement(
            "SELECT id FROM phone_number_results WHERE phone_number_id = ? AND result_hash = ? LIMIT 1"
        ).use { statement ->
            statement.setString(1, phoneNumberId)
            statement.setString(2, resultHash)
            statement.executeQuery().use { resultSet ->
                if (resultSet.next()) {
                    return resultSet.getString("id")
                }
            }
        }

        val resultId = UUID.randomUUID().toString()
        connection.prepareStatement(
            """
            INSERT INTO phone_number_results(
                id, phone_number_id, country, country_code, region_code, number_type,
                international_format, carrier, time_zones, result_hash, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, resultId)
            statement.setString(2, phoneNumberId)
            statement.setString(3, result.country)
            statement.setObject(4, result.countryCode)
            statement.setString(5, result.regionCode)
            statement.setString(6, result.numberType)
            statement.setString(7, result.internationalFormat)
            statement.setString(8, result.carrier)
            statement.setArray(9, connection.createArrayOf("text", result.timeZones?.toTypedArray() ?: emptyArray()))
            statement.setString(10, resultHash)
            statement.setObject(11, OffsetDateTime.parse(createdAt))
            statement.executeUpdate()
        }

        return resultId
    }

    private fun insertUserSearch(
        connection: Connection,
        userId: String,
        phoneNumberId: String,
        rawInput: String,
        createdAt: String
    ) {
        connection.prepareStatement(
            """
            INSERT INTO user_search(id, user_id, phone_number_id, raw_input, created_at)
            VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
        ).use { statement ->
            statement.setString(1, UUID.randomUUID().toString())
            statement.setString(2, userId)
            statement.setString(3, phoneNumberId)
            statement.setString(4, rawInput)
            statement.setObject(5, OffsetDateTime.parse(createdAt))
            statement.executeUpdate()
        }
    }

    private fun mapSavedResult(resultSet: ResultSet): SavedPhoneNumberLookupResult? {
        val resultCreatedAt = resultSet.getObject("result_created_at", OffsetDateTime::class.java)
            ?: return null

        return SavedPhoneNumberLookupResult(
            country = resultSet.getString("country"),
            countryCode = resultSet.getObject("country_code") as? Int,
            regionCode = resultSet.getString("region_code"),
            numberType = resultSet.getString("number_type"),
            internationalFormat = resultSet.getString("international_format"),
            carrier = resultSet.getString("carrier"),
            timeZones = readTimeZones(resultSet),
            createdAt = resultCreatedAt.toInstant().toString(),
        )
    }

    private fun readTimeZones(resultSet: ResultSet): List<String>? {
        val array = resultSet.getArray("time_zones") ?: return null
        val values = (array.array as? Array<*>)
            ?.mapNotNull { it?.toString() }
            ?.takeIf { it.isNotEmpty() }
        array.free()
        return values
    }

    private fun normalizePhoneNumber(rawInput: String, result: PhoneNumberLookupResult): NormalizedPhoneNumber {
        val trimmedInput = rawInput.trim()
        val e164Number = result.internationalFormat?.let(::toE164Number)
        val canonicalNumber = e164Number ?: trimmedInput

        return NormalizedPhoneNumber(
            canonicalNumber = canonicalNumber,
            e164Number = e164Number
        )
    }

    private fun toE164Number(internationalFormat: String): String? {
        val digitsOnly = internationalFormat.filter { character -> character.isDigit() }
        return digitsOnly.takeIf { it.isNotBlank() }
            ?.let { "+$it" }
    }

    private fun computeResultHash(result: PhoneNumberLookupResult): String {
        val canonical = listOf(
            result.country.orEmpty(),
            result.countryCode?.toString().orEmpty(),
            result.regionCode.orEmpty(),
            result.numberType.orEmpty(),
            result.internationalFormat.orEmpty(),
            result.carrier.orEmpty(),
            result.timeZones.orEmpty().joinToString("|")
        ).joinToString("\u001F")

        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private data class NormalizedPhoneNumber(
        val canonicalNumber: String,
        val e164Number: String?
    )

    private data class SavedSearchAccumulator(
        val id: String,
        val number: String,
        val createdAt: String,
        val results: MutableList<SavedPhoneNumberLookupResult> = mutableListOf(),
    )
}
