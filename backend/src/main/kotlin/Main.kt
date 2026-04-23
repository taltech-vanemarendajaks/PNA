package com.pna.backend

import com.pna.backend.config.RootConfig
import com.pna.backend.dal.repositories.NumberSearchRepository
import com.pna.backend.dal.repositories.RefreshTokenRepository
import com.pna.backend.plugins.configureHttp
import com.pna.backend.plugins.configureRouting
import com.pna.backend.plugins.configureSecurity
import com.pna.backend.services.AppJwtService
import com.pna.backend.services.GoogleAuthCodeService
import com.pna.backend.services.GoogleTokenVerifierService
import com.pna.backend.services.NumberSearchService
import com.pna.backend.services.PhoneLookupService
import com.pna.backend.services.RefreshTokenService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*

fun main() {
    val rootConfig = RootConfig.load()

    embeddedServer(Netty, rootConfig.app.port, rootConfig.app.host) {
        module(rootConfig)
    }
        .start(wait = true)
}

fun Application.module(rootConfig: RootConfig = RootConfig.load()) {
    val accessTokenService = AppJwtService(
        rootConfig.jwt.issuer,
        rootConfig.jwt.audience,
        rootConfig.jwt.secret,
        rootConfig.jwt.ttlSeconds
    )

    val googleTokenVerifierService = GoogleTokenVerifierService(
        rootConfig.google.clientId
    )

    val googleAuthCodeService = GoogleAuthCodeService(
        rootConfig.google.clientId,
        rootConfig.google.clientSecret
    )

    val refreshTokenRepository = RefreshTokenRepository(rootConfig.database.refreshTokenPath)
    val refreshTokenService = RefreshTokenService(
        refreshTokenRepository,
        rootConfig.jwt.refreshTokenTtlSeconds,
    )

    val lookupService = PhoneLookupService(rootConfig.app.phoneLookupDefaultRegion)
    val numberSearchRepository = NumberSearchRepository(rootConfig.database.numberSearchPath)
    val numberSearchService = NumberSearchService(numberSearchRepository)

    install(CallLogging)

    install(ContentNegotiation) {
        json()
    }

    configureHttp(rootConfig)

    configureSecurity(
        accessTokenService,
        rootConfig.jwt.issuer,
        rootConfig.jwt.audience
    )

    configureRouting(
        rootConfig,
        accessTokenService,
        refreshTokenService,
        googleTokenVerifierService,
        googleAuthCodeService,
        lookupService,
        numberSearchService
    )
}
