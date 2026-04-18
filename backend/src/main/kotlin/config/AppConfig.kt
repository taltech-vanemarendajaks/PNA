package com.pna.backend.config

import io.github.cdimascio.dotenv.Dotenv
import java.net.URI

data class CorsOrigin(
    val host: String,
    val schemes: List<String>
)

data class AppConfig(
    val port: Int,
    val host: String,
    val googleClientId: String,
    val googleClientSecret: String,
    val publicBackendBaseUrl: String,
    val frontendBaseUrl: String,
    val allowedOrigins: List<CorsOrigin>,
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtTtlSeconds: Long,
    val authCookieSecure: Boolean,
    val authCookieSameSite: String,
    val googleOauthStateTtlSeconds: Int,
    val redirectContextTtlSeconds: Int,
    val oauthFlowCookieSameSite: String
) {
    companion object {
        fun load(): AppConfig {
            val dotenv = Dotenv.configure().ignoreIfMissing().load()

            val port = readValue("APP_PORT", dotenv)?.toIntOrNull() ?: 8080
            val host = readValue("APP_HOST", dotenv) ?: "0.0.0.0"

            val publicBackendBaseUrl = normalizeBaseUrl(
                readValue("PUBLIC_BACKEND_BASE_URL", dotenv) ?: "http://localhost:$port"
            )

            val frontendBaseUrl = normalizeBaseUrl(
                readValue("FRONTEND_BASE_URL", dotenv) ?: "http://localhost:5173"
            )

            val googleClientId = requireValue("GOOGLE_CLIENT_ID", dotenv)
            val googleClientSecret = requireValue("GOOGLE_CLIENT_SECRET", dotenv)
            val jwtSecret = requireValue("JWT_SECRET", dotenv)

            val jwtIssuer = readValue("JWT_ISSUER", dotenv)?.takeIf { it.isNotBlank() } ?: publicBackendBaseUrl
            val jwtAudience = readValue("JWT_AUDIENCE", dotenv)?.takeIf { it.isNotBlank() } ?: "pna-clients"
            val jwtTtlSeconds = readValue("JWT_TTL_SECONDS", dotenv)?.toLongOrNull() ?: 900L

            val authCookieSecure = readValue("AUTH_COOKIE_SECURE", dotenv)?.toBooleanStrictOrNull()
                ?: frontendBaseUrl.startsWith("https://")

            val authCookieSameSite = normalizeSameSite(
                readValue("AUTH_COOKIE_SAME_SITE", dotenv) ?: "Lax"
            )

            val googleOauthStateTtlSeconds = readValue("GOOGLE_OAUTH_STATE_TTL_SECONDS", dotenv)?.toIntOrNull() ?: 600
            val redirectContextTtlSeconds = readValue("REDIRECT_CONTEXT_TTL_SECONDS", dotenv)?.toIntOrNull() ?: 600

            val oauthFlowCookieSameSite = normalizeSameSite(
                readValue("OAUTH_FLOW_COOKIE_SAME_SITE", dotenv) ?: "Lax"
            )

            val allowedOrigins = parseOrigins(
                originsRaw = readValue("CORS_ALLOWED_ORIGINS", dotenv),
                frontendBaseUrl = frontendBaseUrl
            )

            return AppConfig(
                port = port,
                host = host,
                googleClientId = googleClientId,
                googleClientSecret = googleClientSecret,
                publicBackendBaseUrl = publicBackendBaseUrl,
                frontendBaseUrl = frontendBaseUrl,
                allowedOrigins = allowedOrigins,
                jwtSecret = jwtSecret,
                jwtIssuer = jwtIssuer,
                jwtAudience = jwtAudience,
                jwtTtlSeconds = jwtTtlSeconds,
                authCookieSecure = authCookieSecure,
                authCookieSameSite = authCookieSameSite,
                googleOauthStateTtlSeconds = googleOauthStateTtlSeconds,
                redirectContextTtlSeconds = redirectContextTtlSeconds,
                oauthFlowCookieSameSite = oauthFlowCookieSameSite
            )
        }

        private fun readValue(key: String, dotenv: Dotenv): String? {
            return System.getenv(key) ?: dotenv[key]
        }

        private fun requireValue(key: String, dotenv: Dotenv): String {
            return readValue(key, dotenv)?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("$key must be configured")
        }

        internal fun normalizeBaseUrl(value: String): String {
            return value.trim().trimEnd('/')
        }

        internal fun normalizeSameSite(value: String): String {
            return when (value.trim().lowercase()) {
                "strict" -> "Strict"
                "none" -> "None"
                else -> "Lax"
            }
        }

        internal fun parseOrigins(
            originsRaw: String?,
            frontendBaseUrl: String
        ): List<CorsOrigin> {
            val values = buildList {
                add(frontendBaseUrl)
                originsRaw
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotBlank() }
                    ?.let { addAll(it) }
            }.distinct()

            return values.map { origin ->
                if (origin.contains("://")) {
                    val uri = URI(origin)
                    CorsOrigin(
                        host = uri.authority,
                        schemes = listOfNotNull(uri.scheme)
                    )
                } else {
                    CorsOrigin(
                        host = origin,
                        schemes = listOf("http", "https")
                    )
                }
            }
        }

        fun isAllowedOrigin(originRaw: String?, allowedOrigins: List<CorsOrigin>): Boolean {
            if (originRaw.isNullOrBlank()) {
                return false
            }

            val uri = runCatching { URI(originRaw.trim()) }.getOrNull() ?: return false
            val host = uri.authority ?: return false
            val scheme = uri.scheme ?: return false

            return allowedOrigins.any { origin ->
                origin.host.equals(host, ignoreCase = true) &&
                        origin.schemes.any { it.equals(scheme, ignoreCase = true) }
            }
        }
    }
}
