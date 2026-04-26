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
    val googleClientSecret: String?,
    val publicBackendBaseUrl: String,
    val frontendBaseUrl: String,
    val allowAnyHost: Boolean,
    val allowedOrigins: List<CorsOrigin>,
    val sessionTtlSeconds: Long,
    val authCookieSecure: Boolean,
    val authCookieSameSite: String
) {
    companion object {
        fun load(): AppConfig {
            val dotenv = Dotenv.configure().ignoreIfMissing().load()

            val host = readValue("APP_HOST", dotenv) ?: "0.0.0.0"
            val port = (readValue("APP_PORT", dotenv) ?: "8080").toIntOrNull() ?: 8080
            val googleClientId = readValue("GOOGLE_CLIENT_ID", dotenv)?.takeIf { it.isNotBlank() }
            val googleClientSecret = readValue("GOOGLE_CLIENT_SECRET", dotenv)?.takeIf { it.isNotBlank() }
            val publicBackendBaseUrl = normalizeBaseUrl(
                readValue("PUBLIC_BACKEND_BASE_URL", dotenv) ?: "http://localhost:$port",
                "PUBLIC_BACKEND_BASE_URL"
            )
            val frontendBaseUrl = (readValue("FRONTEND_BASE_URL", dotenv) ?: "http://localhost:5173")
                .let { normalizeBaseUrl(it, "FRONTEND_BASE_URL") }
            val allowAnyHost = (readValue("CORS_ALLOW_ANY_HOST", dotenv) ?: "false").toBooleanStrictOrNull() ?: false
            val sessionTtlSeconds = (readValue("AUTH_SESSION_TTL_SECONDS", dotenv) ?: "3600").toLongOrNull()
                ?.takeIf { it > 0 }
                ?: 3600L
            val authCookieSecure = readValue("AUTH_COOKIE_SECURE", dotenv)?.toBooleanStrictOrNull()
                ?: frontendBaseUrl.startsWith("https://")
            val authCookieSameSite = normalizeSameSite(readValue("AUTH_COOKIE_SAME_SITE", dotenv) ?: "Lax")

            validateCookieAndCorsConfig(
                allowAnyHost = allowAnyHost,
                authCookieSecure = authCookieSecure,
                authCookieSameSite = authCookieSameSite
            )

            val allowedOrigins = parseOrigins(readValue("CORS_ALLOWED_ORIGINS", dotenv), frontendBaseUrl, allowAnyHost)

            return AppConfig(
                host = host,
                port = port,
                googleClientId = googleClientId,
                googleClientSecret = googleClientSecret,
                publicBackendBaseUrl = publicBackendBaseUrl,
                frontendBaseUrl = frontendBaseUrl,
                allowAnyHost = allowAnyHost,
                allowedOrigins = allowedOrigins,
                sessionTtlSeconds = sessionTtlSeconds,
                authCookieSecure = authCookieSecure,
                authCookieSameSite = authCookieSameSite
            )
        }

        private fun readValue(key: String, dotenv: Dotenv): String? {
            return System.getenv(key) ?: dotenv[key]
        }

        internal fun parseOrigins(originsRaw: String?, frontendBaseUrl: String, allowAnyHost: Boolean): List<CorsOrigin> {
            val configuredOrigins = originsRaw
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            val effectiveOrigins = buildList {
                add(frontendBaseUrl)

                if (!allowAnyHost) {
                    addAll(configuredOrigins)
                }
            }

            return effectiveOrigins
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .map { origin ->
                    if ("://" in origin) {
                        val uri = runCatching { URI(origin) }.getOrElse {
                            throw IllegalArgumentException("Invalid CORS origin: $origin")
                        }
                        require(uri.path.isNullOrEmpty() || uri.path == "/") {
                            "CORS origin must not include a path: $origin"
                        }
                        require(uri.query.isNullOrEmpty()) {
                            "CORS origin must not include a query string: $origin"
                        }
                        require(uri.fragment.isNullOrEmpty()) {
                            "CORS origin must not include a fragment: $origin"
                        }
                        require(uri.userInfo.isNullOrEmpty()) {
                            "CORS origin must not include user info: $origin"
                        }
                        val host = uri.authority ?: throw IllegalArgumentException("Invalid CORS origin host: $origin")
                        val scheme = uri.scheme ?: throw IllegalArgumentException("Invalid CORS origin scheme: $origin")
                        CorsOrigin(host = host, schemes = listOf(scheme))
                    } else {
                        CorsOrigin(host = origin, schemes = listOf("http", "https"))
                    }
                }

            require(effectiveOrigins.isNotEmpty()) {
                "CORS_ALLOWED_ORIGINS must include at least one explicit origin when cookie auth is enabled"
            }
        }

        internal fun isAllowedOrigin(originRaw: String?, allowedOrigins: List<CorsOrigin>): Boolean {
            if (originRaw.isNullOrBlank()) {
                return false
            }

            val uri = runCatching { URI(originRaw.trim()) }.getOrNull() ?: return false
            if (!uri.query.isNullOrEmpty() || !uri.fragment.isNullOrEmpty() || !uri.userInfo.isNullOrEmpty()) {
                return false
            }
            if (!uri.path.isNullOrEmpty() && uri.path != "/") {
                return false
            }

            val host = uri.authority ?: return false
            val scheme = uri.scheme ?: return false

            return allowedOrigins.any { origin ->
                origin.host.equals(host, ignoreCase = true) &&
                    origin.schemes.any { allowedScheme -> allowedScheme.equals(scheme, ignoreCase = true) }
            }
        }

        internal fun normalizeSameSite(value: String): String {
            return when (value.trim().lowercase()) {
                "lax" -> "Lax"
                "strict" -> "Strict"
                "none" -> "None"
                else -> throw IllegalArgumentException("AUTH_COOKIE_SAME_SITE must be one of: Lax, Strict, None")
            }
        }

        internal fun normalizeBaseUrl(value: String, configKey: String): String {
            val normalizedValue = value.trim().trimEnd('/')
            require(normalizedValue.isNotBlank()) {
                "$configKey must not be blank"
            }

            val uri = runCatching { URI(normalizedValue) }.getOrElse {
                throw IllegalArgumentException("Invalid $configKey: $normalizedValue")
            }

            require(uri.scheme != null && uri.authority != null) {
                "$configKey must be an absolute URL: $normalizedValue"
            }
            require(uri.path.isNullOrEmpty() || uri.path == "/") {
                "$configKey must not include a path: $normalizedValue"
            }
            require(uri.query.isNullOrEmpty()) {
                "$configKey must not include a query string: $normalizedValue"
            }
            require(uri.fragment.isNullOrEmpty()) {
                "$configKey must not include a fragment: $normalizedValue"
            }
            require(uri.userInfo.isNullOrEmpty()) {
                "$configKey must not include user info: $normalizedValue"
            }

            return normalizedValue
        }

        internal fun validateCookieAndCorsConfig(
            allowAnyHost: Boolean,
            authCookieSecure: Boolean,
            authCookieSameSite: String
        ) {
            require(!allowAnyHost) {
                "CORS_ALLOW_ANY_HOST=true is not supported with credentialed cookie auth; use explicit CORS_ALLOWED_ORIGINS"
            }

            require(authCookieSameSite != "None" || authCookieSecure) {
                "AUTH_COOKIE_SAME_SITE=None requires AUTH_COOKIE_SECURE=true"
            }
        }
    }
}
