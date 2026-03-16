package com.pna.backend.config

import io.github.cdimascio.dotenv.Dotenv
import java.net.URI

data class CorsOrigin(
    val host: String,
    val schemes: List<String>
)

data class AppConfig(
    val host: String,
    val port: Int,
    val googleClientId: String?,
    val allowAnyHost: Boolean,
    val allowedOrigins: List<CorsOrigin>
) {
    companion object {
        fun load(): AppConfig {
            val dotenv = Dotenv.configure().ignoreIfMissing().load()

            val host = readValue("APP_HOST", dotenv) ?: "0.0.0.0"
            val port = (readValue("APP_PORT", dotenv) ?: "8080").toIntOrNull() ?: 8080
            val googleClientId = readValue("GOOGLE_CLIENT_ID", dotenv)?.takeIf { it.isNotBlank() }
            val allowAnyHost = (readValue("CORS_ALLOW_ANY_HOST", dotenv) ?: "true").toBooleanStrictOrNull() ?: true
            val allowedOrigins = parseOrigins(readValue("CORS_ALLOWED_ORIGINS", dotenv))

            return AppConfig(
                host = host,
                port = port,
                googleClientId = googleClientId,
                allowAnyHost = allowAnyHost,
                allowedOrigins = allowedOrigins
            )
        }

        private fun readValue(key: String, dotenv: Dotenv): String? {
            return System.getenv(key) ?: dotenv[key]
        }

        private fun parseOrigins(originsRaw: String?): List<CorsOrigin> {
            if (originsRaw.isNullOrBlank()) {
                return emptyList()
            }

            return originsRaw
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { origin ->
                    if ("://" in origin) {
                        val uri = runCatching { URI(origin) }.getOrNull() ?: return@mapNotNull null
                        val host = uri.host ?: return@mapNotNull null
                        val scheme = uri.scheme ?: return@mapNotNull null
                        CorsOrigin(host = host, schemes = listOf(scheme))
                    } else {
                        CorsOrigin(host = origin, schemes = listOf("http", "https"))
                    }
                }
        }
    }
}
