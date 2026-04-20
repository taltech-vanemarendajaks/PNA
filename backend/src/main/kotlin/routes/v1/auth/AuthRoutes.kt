package com.pna.backend.routes.v1.auth

import com.pna.backend.config.AppConfig
import com.pna.backend.routes.v1.hasAllowedOrigin
import com.pna.backend.routes.v1.respondPrivateNoStore
import com.pna.backend.services.AppJwtService
import com.pna.backend.services.GoogleAuthCodeService
import com.pna.backend.services.GoogleTokenVerifierService
import com.pna.backend.services.RefreshTokenService
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
    refreshTokenService: RefreshTokenService,
    googleTokenVerifierService: GoogleTokenVerifierService,
    googleAuthCodeService: GoogleAuthCodeService
) {
    route("/api/v1/auth") {
        get("/google/redirect") {
            call.handleGoogleRedirectRequest(
                appConfig = appConfig,
                accessTokenService = accessTokenService,
                googleAuthCodeService = googleAuthCodeService,
                googleTokenVerifierService = googleTokenVerifierService,
                refreshTokenService = refreshTokenService
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

        post("/refresh") {
            call.handleRefresh(
                appConfig = appConfig,
                refreshTokenService = refreshTokenService,
                accessTokenService = accessTokenService
            )
        }

        authenticate("auth-jwt") {
            post("/logout") {
                call.handleLogout(
                    appConfig = appConfig,
                    refreshTokenService = refreshTokenService
                )
            }
        }
    }
}

private suspend fun ApplicationCall.handleRefresh(
    appConfig: AppConfig,
    refreshTokenService: RefreshTokenService,
    accessTokenService: AppJwtService
) {
    if (!ensureAllowedOrigin(appConfig.allowedOrigins)) {
        return
    }

    val rotation = refreshTokenService.rotateRefreshToken(
        request.cookies[REFRESH_TOKEN_COOKIE_NAME] ?: ""
    )

    if (rotation == null) {
        clearAuthAccessCookie(appConfig)
        clearRefreshTokenCookie(appConfig)
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Refresh token is invalid or expired"))
        return
    }

    appendAuthAccessCookie(accessTokenService.issueAccessToken(rotation.user), appConfig)
    appendRefreshTokenCookie(rotation.refreshToken, appConfig)
    respond(HttpStatusCode.NoContent)
}

private suspend fun ApplicationCall.handleLogout(
    appConfig: AppConfig,
    refreshTokenService: RefreshTokenService
) {
    if (!ensureAllowedOrigin(appConfig.allowedOrigins)) {
        return
    }

    refreshTokenService.revokeRefreshToken(request.cookies[REFRESH_TOKEN_COOKIE_NAME])
    clearAuthAccessCookie(appConfig)
    clearFrontendRedirectContextCookies(appConfig)
    clearGoogleOauthStateCookie(appConfig)
    clearRefreshTokenCookie(appConfig)

    respond(HttpStatusCode.NoContent)
}

private fun JWTPrincipal.toGoogleAuthResponse(): GoogleAuthResponse {
    return GoogleAuthResponse(
        subject = payload.subject,
        email = payload.getClaim("email").asString(),
        name = payload.getClaim("name").asString(),
        givenName = payload.getClaim("givenName").asString()
    )
}
