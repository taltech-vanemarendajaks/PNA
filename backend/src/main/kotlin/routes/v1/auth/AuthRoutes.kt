package com.pna.backend.routes.v1.auth

import com.pna.backend.config.AppConfig
import com.pna.backend.routes.v1.hasAllowedOrigin
import com.pna.backend.routes.v1.respondPrivateNoStore
import com.pna.backend.services.AppJwtService
import com.pna.backend.services.GoogleAuthCodeService
import com.pna.backend.services.GoogleTokenVerifierService
import domain.auth.response.GoogleAuthResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.googleAuthRoutes(
    appConfig: AppConfig,
    accessTokenService: AppJwtService,
    googleTokenVerifierService: GoogleTokenVerifierService,
    googleAuthCodeService: GoogleAuthCodeService
) {
    route("/api/v1/auth") {
        get("/google/redirect") {
            call.handleGoogleRedirectRequest(
                appConfig = appConfig,
                accessTokenService = accessTokenService,
                googleAuthCodeService = googleAuthCodeService,
                googleTokenVerifierService = googleTokenVerifierService
            )
        }

        authenticate("auth-jwt") {
            get("/session") {
                val principal = call.principal<JWTPrincipal>()
                    ?: return@get call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                call.respondPrivateNoStore()
                call.respond(HttpStatusCode.OK, principal.toGoogleAuthResponse())
            }
        }

        post("/logout") {
            if (!call.hasAllowedOrigin(appConfig)) {
                call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Invalid origin"))
                return@post
            }

            call.clearAuthAccessCookie(appConfig)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun JWTPrincipal.toGoogleAuthResponse(): GoogleAuthResponse {
    return GoogleAuthResponse(
        subject = payload.subject,
        email = payload.getClaim("email").asString(),
        name = payload.getClaim("name").asString(),
        givenName = payload.getClaim("givenName").asString()
    )
}
