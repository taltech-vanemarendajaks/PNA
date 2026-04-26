package com.pna.backend.dal

import com.pna.backend.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.ClassLoaderResourceAccessor

class Database(private val databaseConfig: DatabaseConfig) : AutoCloseable {
    val dataSource: HikariDataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = databaseConfig.jdbcUrl
            username = databaseConfig.username
            password = databaseConfig.password
            maximumPoolSize = databaseConfig.maximumPoolSize
            minimumIdle = databaseConfig.minimumIdle
            connectionTimeout = databaseConfig.connectionTimeoutMs
            idleTimeout = databaseConfig.idleTimeoutMs
            maxLifetime = databaseConfig.maxLifetimeMs
            isAutoCommit = databaseConfig.autoCommit
            poolName = databaseConfig.poolName
            extractCurrentSchemaSearchPath(databaseConfig.jdbcUrl)?.let { searchPath ->
                connectionInitSql = "SET search_path TO $searchPath"
            }
        }
    )

    fun migrate() {
        dataSource.connection.use { connection ->
            val database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(JdbcConnection(connection))

            Liquibase(
                "db/changelog/db.changelog-master.yaml",
                ClassLoaderResourceAccessor(),
                database
            ).use { liquibase ->
                liquibase.update(Contexts(), LabelExpression())
            }
        }
    }

    override fun close() {
        dataSource.close()
    }

    private fun extractCurrentSchemaSearchPath(jdbcUrl: String): String? {
        val query = jdbcUrl.substringAfter('?', missingDelimiterValue = "")
        if (query.isBlank()) {
            return null
        }

        val currentSchemaValue = query
            .split('&')
            .firstOrNull { part -> part.substringBefore('=') == "currentSchema" }
            ?.substringAfter('=', missingDelimiterValue = "")
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return URLDecoder.decode(currentSchemaValue, StandardCharsets.UTF_8)
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ") { schemaName ->
                "\"${schemaName.replace("\"", "\"\"")}\""
            }
    }
}
