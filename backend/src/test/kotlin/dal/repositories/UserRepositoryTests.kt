package dal.repositories

import com.pna.backend.dal.repositories.UserRepository
import domain.auth.GoogleUser
import support.TestDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.use

class UserRepositoryTests {
    @Test
    fun `upsert user reuses row keyed by subject and updates profile fields`() {
        TestDatabase.newDatabase("number_search_user_upsert").use { database ->
            database.migrate()

            val userRepository = UserRepository(database)

            val firstId = userRepository.upsertUser(
                GoogleUser(
                    subject = "google-subject-123",
                    email = "first@example.com",
                    name = "First Name",
                    givenName = "First"
                )
            )

            val secondId = userRepository.upsertUser(
                GoogleUser(
                    subject = "google-subject-123",
                    email = "updated@example.com",
                    name = "Updated Name",
                    givenName = "Updated"
                )
            )

            assertEquals(firstId, secondId)

            database.dataSource.connection.use { connection ->
                connection.prepareStatement(
                    "SELECT id, subject, email, name, given_name FROM users WHERE subject = ?"
                ).use { statement ->
                    statement.setString(1, "google-subject-123")
                    statement.executeQuery().use { resultSet ->
                        assertTrue(resultSet.next())
                        assertEquals(firstId, resultSet.getString("id"))
                        assertEquals("updated@example.com", resultSet.getString("email"))
                        assertEquals("Updated Name", resultSet.getString("name"))
                        assertEquals("Updated", resultSet.getString("given_name"))
                        assertTrue(!resultSet.next())
                    }
                }
            }
        }
    }
}
