package com.pna.backend.config

data class AppConfig(
    val host: String,
    val port: Int,
    val publicBackendBaseUrl: String,
    val frontendBaseUrl: String,
    val allowedOrigins: List<String>,
    val allowedOriginsMapped: List<CorsOrigin>,
    val authCookieSecure: Boolean,
    val authCookieSameSite: String,
    val oauthFlowCookieSameSite: String,
    val googleOauthStateTtlSeconds: Int,
    val redirectContextTtlSeconds: Int,
    val phoneLookupDefaultRegion: String
)

data class KtorConfig(
    val deployment: KtorDeploymentConfig
)

data class KtorDeploymentConfig(
    val host: String,
    val port: Int
)

data class JwtConfig(
    val issuer: String,
    val audience: String,
    val secret: String,
    val ttlSeconds: Int,
    val refreshTokenTtlSeconds: Int
)

data class DatabaseConfig(
    val jdbcUrl: String,
    val username: String,
    val password: String,
    val maximumPoolSize: Int,
    val minimumIdle: Int,
    val connectionTimeoutMs: Long,
    val idleTimeoutMs: Long,
    val maxLifetimeMs: Long,
    val autoCommit: Boolean,
    val poolName: String
)

data class GoogleConfig(
    val clientId: String,
    val clientSecret: String
)
