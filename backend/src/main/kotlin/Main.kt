package com.pna.backend

import com.pna.backend.config.AppConfig
import com.pna.backend.dal.repositories.NumberSearchRepository
import com.pna.backend.plugins.configureHttp
import com.pna.backend.plugins.configureRouting
import com.pna.backend.plugins.configureSecurity
import com.pna.backend.services.AppJwtService
import com.pna.backend.services.GoogleAuthCodeService
import com.pna.backend.services.GoogleTokenVerifierService
import com.pna.backend.services.NumberSearchService
import com.pna.backend.services.PhoneLookupService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
    val appConfig = AppConfig.load()

    embeddedServer(Netty, port = appConfig.port, host = appConfig.host) {
        module(appConfig)
    }
        .start(wait = true)
}

fun Application.module(appConfig: AppConfig = AppConfig.load()) {
    val accessTokenService = AppJwtService(
        issuer = appConfig.jwtIssuer,
        audience = appConfig.jwtAudience,
        secret = appConfig.jwtSecret,
        ttlSeconds = appConfig.jwtTtlSeconds
    )

    val googleTokenVerifierService = GoogleTokenVerifierService(
        clientId = appConfig.googleClientId
    )

    val googleAuthCodeService = GoogleAuthCodeService(
        clientId = appConfig.googleClientId,
        clientSecret = appConfig.googleClientSecret
    )

    val lookupService = PhoneLookupService()
    val numberSearchRepository = NumberSearchRepository()
    val numberSearchService = NumberSearchService(numberSearchRepository)

    install(CallLogging)

    install(ContentNegotiation) {
        json()
    }

    configureHttp(appConfig)

    configureSecurity(
        accessTokenService = accessTokenService,
        issuer = appConfig.jwtIssuer,
        audience = appConfig.jwtAudience
    )

    configureRouting(
        appConfig = appConfig,
        accessTokenService = accessTokenService,
        googleTokenVerifierService = googleTokenVerifierService,
        googleAuthCodeService = googleAuthCodeService,
        lookupService = lookupService,
        numberSearchService = numberSearchService
    )
}
