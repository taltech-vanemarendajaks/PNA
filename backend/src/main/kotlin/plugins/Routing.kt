package com.pna.backend.plugins

import com.pna.backend.config.AppConfig
import com.pna.backend.routes.v1.auth.googleAuthRoutes
import com.pna.backend.routes.v1.number.numberRoutes
import com.pna.backend.services.AppJwtService
import com.pna.backend.services.GoogleAuthCodeService
import com.pna.backend.services.GoogleTokenVerifierService
import com.pna.backend.services.NumberSearchService
import com.pna.backend.services.PhoneLookupService
import com.pna.backend.services.RefreshTokenService
import io.ktor.server.application.*
import io.ktor.server.plugins.openapi.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting(
    appConfig: AppConfig,
    accessTokenService: AppJwtService,
    refreshTokenService: RefreshTokenService,
    googleTokenVerifierService: GoogleTokenVerifierService,
    googleAuthCodeService: GoogleAuthCodeService,
    lookupService: PhoneLookupService,
    numberSearchService: NumberSearchService
) {
    routing {
        openAPI(path = "openapi", swaggerFile = "openapi/documentation.yaml")
        swaggerUI(path = "swagger", swaggerFile = "openapi/documentation.yaml")

        get("/health") {
            call.respondText("ok")
        }

        googleAuthRoutes(
            appConfig,
            accessTokenService,
            refreshTokenService,
            googleTokenVerifierService,
            googleAuthCodeService
        )

        numberRoutes(
            appConfig,
            accessTokenService::verify,
            lookupService,
            numberSearchService
        )
    }
}
