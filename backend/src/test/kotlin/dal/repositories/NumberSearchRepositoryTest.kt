package dal.repositories

import com.pna.backend.dal.Database
import com.pna.backend.dal.repositories.NumberSearchRepository
import com.pna.backend.dal.repositories.UserRepository
import com.pna.backend.domain.auth.response.PhoneNumberLookupResult
import domain.auth.GoogleUser
import support.TestDatabase
import java.sql.Connection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NumberSearchRepositoryTest {

    @Test
    fun `history returns all saved results ordered newest first for each repeated number lookup`() {
        TestDatabase.newDatabase("number_search_stable_history").use { database ->
            database.migrate()

            val userRepository = UserRepository(database)
            val repository = NumberSearchRepository(database, userRepository)

            val user = GoogleUser("subject-1", "user@example.com", "Jane", "Jane")
            val otherUser = GoogleUser("subject-2", "user2@example.com", "Jack", "Jack")

            val resultA = lookupResult(internationalFormat = "+372 5555 1234", carrier = "Carrier A")
            val resultB = lookupResult(internationalFormat = "+372 5555 1234", carrier = "Carrier B")
            val resultC = lookupResult(internationalFormat = "+372 5555 4321", carrier = "Carrier C")

            repository.save(user, "055551234", resultA)
            repository.save(user, "055551234", resultB)
            repository.save(user, "055551234", resultA)
            repository.save(otherUser, "055554321", resultC)

            val history = repository.findAllByUser(user)

            assertEquals(3, history.size)
            assertEquals(listOf("055551234", "055551234", "055551234"), history.map { it.number })
            assertEquals(3, history.size)
            assertEquals(listOf("055551234", "055551234", "055551234"), history.map { it.number })
            assertEquals(
                listOf(
                    listOf("Carrier B", "Carrier A"),
                    listOf("Carrier B", "Carrier A"),
                    listOf("Carrier B", "Carrier A"),
                ),
                history.map { search -> search.results.map { it.carrier } }
            )
            assertEquals(
                listOf(
                    history[0].results.map { it.createdAt },
                    history[1].results.map { it.createdAt },
                    history[2].results.map { it.createdAt },
                ).all { timestamps -> timestamps == timestamps.sortedDescending() },
                true
            )

            database.dataSource.connection.use { connection ->
                assertEquals(4, countRows(connection, "user_search"))
                assertEquals(3, countRows(connection, "phone_number_results"))
                assertEquals(2, countRows(connection, "phone_number"))
            }
        }
    }

    @Test
    fun `find all by user returns only that authenticated users history`() {
        TestDatabase.newDatabase("number_search_history").use { database ->
            database.migrate()

            val userRepository = UserRepository(database)
            val repository = NumberSearchRepository(database, userRepository)

            val result = lookupResult(internationalFormat = "+372 5555 1234", carrier = "Carrier A")

            repository.save(GoogleUser("subject-1", "one@example.com", "One", "One"), "055551234", result)
            repository.save(GoogleUser("subject-2", "two@example.com", "Two", "Two"), "066661234", result)
            repository.save(GoogleUser("subject-1", "one@example.com", "One", "One"), "077771234", result)

            val history = repository.findAllByUser(GoogleUser("subject-1", null, null, null))

            assertEquals(2, history.size)
            assertEquals(listOf("077771234", "055551234"), history.map { it.number })
            assertTrue(history.all { it.id.isNotBlank() })
            assertTrue(history.all { it.results.isNotEmpty() })
        }
    }

    @Test
    fun `delete by id removes only the owned user search row and preserves phone data`() {
        TestDatabase.newDatabase("number_search_delete_by_id").use { database ->
            database.migrate()

            val userRepository = UserRepository(database)
            val repository = NumberSearchRepository(database, userRepository)

            val user = GoogleUser("subject-1", "user@example.com", "Jane", "Jane")
            val result = lookupResult(internationalFormat = "+372 5555 1234", carrier = "Carrier A")

            repository.save(user, "055551234", result)
            repository.save(user, "055551234", result)

            val searchIds = searchIdsForUser(database, user.subject)
            val deleted = repository.deleteById(user, searchIds.first())

            assertTrue(deleted)
            assertEquals(1, repository.findAllByUser(user).size)

            database.dataSource.connection.use { connection ->
                assertEquals(1, countRows(connection, "user_search"))
                assertEquals(1, countRows(connection, "phone_number"))
                assertEquals(1, countRows(connection, "phone_number_results"))
            }
        }
    }

    @Test
    fun `delete by id returns false when search is missing or owned by another user`() {
        TestDatabase.newDatabase("number_search_delete_missing").use { database ->
            database.migrate()

            val userRepository = UserRepository(database)
            val repository = NumberSearchRepository(database, userRepository)

            val owner = GoogleUser("subject-1", "owner@example.com", "Owner", "Owner")
            val otherUser = GoogleUser("subject-2", "other@example.com", "Other", "Other")
            val result = lookupResult(internationalFormat = "+372 5555 1234", carrier = "Carrier A")

            repository.save(owner, "055551234", result)
            val ownedSearchId = searchIdsForUser(database, owner.subject).single()

            assertEquals(false, repository.deleteById(owner, "missing-search-id"))
            assertEquals(false, repository.deleteById(otherUser, ownedSearchId))

            database.dataSource.connection.use { connection ->
                assertEquals(1, countRows(connection, "user_search"))
                assertEquals(1, countRows(connection, "phone_number"))
                assertEquals(1, countRows(connection, "phone_number_results"))
            }
        }
    }

    @Test
    fun `delete all by user removes only that users history and preserves shared phone data`() {
        TestDatabase.newDatabase("number_search_delete_all").use { database ->
            database.migrate()

            val userRepository = UserRepository(database)
            val repository = NumberSearchRepository(database, userRepository)

            val user = GoogleUser("subject-1", "one@example.com", "One", "One")
            val otherUser = GoogleUser("subject-2", "two@example.com", "Two", "Two")
            val result = lookupResult(internationalFormat = "+372 5555 1234", carrier = "Carrier A")

            repository.save(user, "055551234", result)
            repository.save(user, "055551234", result)
            repository.save(otherUser, "055551234", result)

            val deletedCount = repository.deleteAllByUser(user)

            assertEquals(2, deletedCount)
            assertEquals(0, repository.findAllByUser(user).size)
            assertEquals(1, repository.findAllByUser(otherUser).size)

            database.dataSource.connection.use { connection ->
                assertEquals(1, countRows(connection, "user_search"))
                assertEquals(1, countRows(connection, "phone_number"))
                assertEquals(1, countRows(connection, "phone_number_results"))
            }
        }
    }

    private fun lookupResult(
        internationalFormat: String,
        carrier: String
    ): PhoneNumberLookupResult {
        return PhoneNumberLookupResult(
            country = "Estonia",
            countryCode = 372,
            regionCode = "EE",
            numberType = "Mobile",
            internationalFormat = internationalFormat,
            carrier = carrier,
            timeZones = listOf("Europe/Tallinn")
        )
    }

    private fun countRows(connection: Connection, tableName: String): Int {
        connection.prepareStatement("SELECT COUNT(*) FROM $tableName").use { statement ->
            statement.executeQuery().use { resultSet ->
                resultSet.next()
                return resultSet.getInt(1)
            }
        }
    }

    private fun searchIdsForUser(database: Database, subject: String): List<String> {
        database.dataSource.connection.use { connection ->
            connection.prepareStatement(
                """
                SELECT us.id
                FROM user_search us
                JOIN users u ON u.id = us.user_id
                WHERE u.subject = ?
                ORDER BY us.created_at DESC, us.id DESC
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, subject)
                statement.executeQuery().use { resultSet ->
                    val ids = mutableListOf<String>()
                    while (resultSet.next()) {
                        ids += resultSet.getString("id")
                    }
                    return ids.toList()
                }
            }
        }
    }
}
