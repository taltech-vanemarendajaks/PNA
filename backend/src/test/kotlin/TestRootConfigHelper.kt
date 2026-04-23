import com.pna.backend.config.AppConfig
import com.pna.backend.config.CorsOrigin

import com.pna.backend.config.DatabaseConfig
import com.pna.backend.config.GoogleConfig
import com.pna.backend.config.JwtConfig
import com.pna.backend.config.KtorConfig
import com.pna.backend.config.KtorDeploymentConfig
import com.pna.backend.config.RootConfig
import com.pna.backend.services.AppJwtService

fun testRootConfig(
    authCookieSecure: Boolean = false,
    authCookieSameSite: String = "Lax",
    oauthFlowCookieSameSite: String = "Lax",
    frontendBaseUrl: String = "http://localhost:5173",
    allowedOriginsMapped: List<CorsOrigin> = listOf(CorsOrigin("localhost:5173", listOf("http"))),
    allowedOrigins: List<String> = listOf(frontendBaseUrl),
): RootConfig {
    return RootConfig(
        KtorConfig(
            deployment = KtorDeploymentConfig("0.0.0.1", 8081)
        ),
        AppConfig(
            allowedOriginsMapped = allowedOriginsMapped,
            authCookieSecure = authCookieSecure,
            host = "0.0.0.0",
            port = 8080,
            publicBackendBaseUrl = "http://localhost:8080",
            frontendBaseUrl = frontendBaseUrl,
            allowedOrigins = allowedOrigins,
            authCookieSameSite = authCookieSameSite,
            oauthFlowCookieSameSite = oauthFlowCookieSameSite,
            googleOauthStateTtlSeconds = 600,
            redirectContextTtlSeconds = 600,
            phoneLookupDefaultRegion = "EE",
        ),
        JwtConfig(
            issuer = "test-issuer",
            audience = "test-audience",
            secret = "test-secret",
            ttlSeconds = 600,
            refreshTokenTtlSeconds = 600,
        ),
        DatabaseConfig(
            jdbcUrl = "urldb",
            username = "user",
            password = "password",
            maximumPoolSize = 1,
            numberSearchPath = "number-searches.db",
            refreshTokenPath = "refresh-tokens.db"
        ),
        GoogleConfig(
            clientId = "google-client-id",
            clientSecret = "google-client-secret",
            callbackUrl = "call-back"
        )
    )
}

fun testJwtService(): AppJwtService {
    val testConfig = testRootConfig()
    return AppJwtService(
        issuer = testConfig.jwt.issuer,
        audience =  testConfig.jwt.audience,
        secret =  testConfig.jwt.secret,
        ttlSeconds = testConfig.jwt.ttlSeconds
    )
}
