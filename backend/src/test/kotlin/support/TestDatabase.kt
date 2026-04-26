package support

import com.pna.backend.config.DatabaseConfig
import com.pna.backend.dal.Database
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

object TestDatabase {
    private val postgres = PostgreSQLContainer("postgres:16-alpine")
        .withUsername("test")
        .withPassword("test")
        .withDatabaseName("postgres")

    init {
        postgres.start()
    }

    fun newDatabase(schemaName: String): Database {
        recreateSchema(schemaName)

        val config = DatabaseConfig(
            jdbcUrl = buildJdbcUrl(schemaName),
            username = postgres.username,
            password = postgres.password,
            maximumPoolSize = 2,
            minimumIdle = 1,
            connectionTimeoutMs = 10_000,
            idleTimeoutMs = 60_000,
            maxLifetimeMs = 1_800_000,
            autoCommit = true,
            poolName = "test-pool-$schemaName"
        )

        return Database(config)
    }

    private fun buildJdbcUrl(schemaName: String): String {
        val separator = if ('?' in postgres.jdbcUrl) '&' else '?'
        return "${postgres.jdbcUrl}${separator}currentSchema=$schemaName"
    }

    private fun recreateSchema(schemaName: String) {
        DriverManager.getConnection(
            postgres.jdbcUrl,
            postgres.username,
            postgres.password
        ).use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("""DROP SCHEMA IF EXISTS "$schemaName" CASCADE""")
                statement.execute("""CREATE SCHEMA "$schemaName"""")
            }
        }
    }
}