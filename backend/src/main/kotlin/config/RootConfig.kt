package com.pna.backend.config

import com.sksamuel.hoplite.ConfigLoaderBuilder
import com.sksamuel.hoplite.addMapSource
import com.sksamuel.hoplite.addEnvironmentSource
import com.sksamuel.hoplite.addResourceSource
import io.github.cdimascio.dotenv.Dotenv
import java.net.URI

data class CorsOrigin(
    val host: String,
    val schemes: List<String>
)

data class RootConfig(
    val ktor: KtorConfig,
    val app: AppConfig,
    val jwt: JwtConfig,
    val database: DatabaseConfig,
    val google: GoogleConfig
) {
    companion object {
        fun load(): RootConfig {
            return loadConfigFile()
        }

        internal fun loadConfigFile(
            dotenv: Dotenv = Dotenv.configure().ignoreIfMissing().load(),
            propertyOverrides: Map<String, Any>? = null
        ): RootConfig {
            val builder = ConfigLoaderBuilder.default()

            if (propertyOverrides == null) {
                builder.addEnvironmentSource()
            } else {
                builder.addMapSource(propertyOverrides)
            }

            val loaded = builder
                .addResourceSource("/application-dev.yml", optional = true)
                .addResourceSource("/application.yml")
                .build()
                .loadConfigOrThrow<RootConfig>()

            return loaded
                .withDotenvFallback(dotenv)
                .normalize()
                .validateRequiredSecrets()
        }

        internal fun RootConfig.withDotenvFallback(dotenv: Dotenv): RootConfig {
            return copy(
                jwt = jwt.copy(
                    secret = jwt.secret.ifBlank { dotenv["JWT_SECRET"] ?: "" }
                ),
                google = google.copy(
                    clientId = google.clientId.ifBlank { dotenv["GOOGLE_CLIENT_ID"] ?: "" },
                    clientSecret = google.clientSecret.ifBlank { dotenv["GOOGLE_CLIENT_SECRET"] ?: "" }
                )
            )
        }

        internal fun RootConfig.validateRequiredSecrets(): RootConfig {
            check(jwt.secret.isNotBlank()) {
                "Missing JWT secret. Set JWT_SECRET in the environment or .env."
            }
            check(google.clientId.isNotBlank()) {
                "Missing Google client ID. Set GOOGLE_CLIENT_ID in the environment or .env."
            }
            check(google.clientSecret.isNotBlank()) {
                "Missing Google client secret. Set GOOGLE_CLIENT_SECRET in the environment or .env."
            }
            return this
        }


        internal fun RootConfig.normalize(): RootConfig {
            val normalizedApp = app.copy(
                publicBackendBaseUrl = normalizeBaseUrl(app.publicBackendBaseUrl),
                frontendBaseUrl = normalizeBaseUrl(app.frontendBaseUrl),
                authCookieSameSite = normalizeSameSite(app.authCookieSameSite),
                oauthFlowCookieSameSite = normalizeSameSite(app.oauthFlowCookieSameSite)
            )

            val resolvedAllowedOrigins = normalizedApp.allowedOriginsMapped.ifEmpty {
                parseOrigins(
                    originsRaws = normalizedApp.allowedOrigins,
                    frontendBaseUrl = normalizedApp.frontendBaseUrl
                )
            }

            val normalized = copy(
                app = normalizedApp.copy(
                    allowedOriginsMapped = resolvedAllowedOrigins
                )
            )

            validateCookieSettings(
                authCookieSecure = normalized.app.authCookieSecure,
                authCookieSameSite = normalized.app.authCookieSameSite
            )

            return normalized
        }


        internal fun normalizeBaseUrl(value: String): String {
            return value.trim().trimEnd('/')
        }

        internal fun normalizeSameSite(value: String): String {
            return when (value.trim().lowercase()) {
                "strict" -> "Strict"
                "none" -> "None"
                "lax" -> "Lax"
                else -> throw IllegalArgumentException("AUTH_COOKIE_SAME_SITE must be one of: Lax, Strict, None.")
            }
        }

        internal fun validateCookieSettings(
            authCookieSecure: Boolean,
            authCookieSameSite: String
        ) {
            require(authCookieSameSite != "None" || authCookieSecure) {
                "AUTH_COOKIE_SAME_SITE=None requires AUTH_COOKIE_SECURE=true"
            }
        }

        internal fun parseOrigins(
            originsRaws: List<String>,
            frontendBaseUrl: String
        ): List<CorsOrigin> {
            val values = buildList {
                add(frontendBaseUrl)
                addAll(originsRaws)
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

            val rawOrigin = originRaw.trim()

            val origin = runCatching {
                if (rawOrigin.contains("://")) {
                    val uri = URI(rawOrigin)
                    CorsOrigin(
                        host = uri.authority ?: return false,
                        schemes = listOfNotNull(uri.scheme)
                    )
                } else {
                    CorsOrigin(
                        host = rawOrigin,
                        schemes = listOf("http", "https")
                    )
                }
            }.getOrNull() ?: return false

            return allowedOrigins.any { allowed ->
                allowed.host.equals(origin.host, ignoreCase = true) &&
                        allowed.schemes.any { allowedScheme ->
                            origin.schemes.any { originScheme ->
                                allowedScheme.equals(originScheme, ignoreCase = true)
                            }
                        }
            }
        }
    }
}
