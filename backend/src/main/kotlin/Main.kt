package com.pna.backend

import com.pna.backend.config.AppConfig
import com.pna.backend.dal.repositories.NumberSearchRepository
import com.pna.backend.routes.v1.auth.googleAuthRoutes
import com.pna.backend.routes.v1.number.numberRoutes
import com.pna.backend.services.AuthSessionService
import com.pna.backend.services.NumberSearchService
import com.pna.backend.services.PhoneLookupService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main() {
    val appConfig = AppConfig.load()

    embeddedServer(Netty, port = appConfig.port, host = appConfig.host) {
        module(appConfig)
    }
        .start(wait = true)
}

fun Application.module(appConfig: AppConfig = AppConfig.load()) {
    val authSessionService = AuthSessionService(ttlSeconds = appConfig.sessionTtlSeconds)
    val lookupService = PhoneLookupService()
    val numberSearchRepository = NumberSearchRepository()
    val numberSearchService = NumberSearchService(numberSearchRepository)

    install(CallLogging)
    install(ContentNegotiation) {
        json()
    }
    install(CORS) {
        appConfig.allowedOrigins.forEach { origin ->
            allowHost(origin.host, schemes = origin.schemes)
        }
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Get)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowCredentials = true
    }

    routing {
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        get("/health") {
            call.respondText("ok")
        }
        googleAuthRoutes(
            googleClientId = appConfig.googleClientId,
            googleClientSecret = appConfig.googleClientSecret,
            publicBackendBaseUrl = appConfig.publicBackendBaseUrl,
            frontendBaseUrl = appConfig.frontendBaseUrl,
            allowedOrigins = appConfig.allowedOrigins,
            authSessionService = authSessionService,
            sessionTtlSeconds = appConfig.sessionTtlSeconds,
            authCookieSecure = appConfig.authCookieSecure,
            authCookieSameSite = appConfig.authCookieSameSite
        )
        numberRoutes(authSessionService, lookupService, numberSearchService)
    }
}
