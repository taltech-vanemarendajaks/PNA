package com.pna.backend.routes.v1.auth

import com.pna.backend.config.RootConfig
import com.pna.backend.routes.v1.hasAllowedOrigin
import com.pna.backend.routes.v1.respondPrivateNoStore
import com.pna.backend.services.AppJwtService
import com.pna.backend.services.GoogleAuthCodeService
import com.pna.backend.services.GoogleTokenVerifierService
import com.pna.backend.services.RefreshTokenService
import com.pna.backend.services.SessionClientMetadata
import domain.auth.response.GoogleAuthResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.googleAuthRoutes(
    rootConfig: RootConfig,
    accessTokenService: AppJwtService,
    refreshTokenService: RefreshTokenService,
    googleTokenVerifierService: GoogleTokenVerifierService,
    googleAuthCodeService: GoogleAuthCodeService
) {
    route("/api/v1/auth") {
        get("/google/redirect") {
            call.handleGoogleRedirectRequest(
                rootConfig,
                accessTokenService,
                googleAuthCodeService,
                googleTokenVerifierService,
                refreshTokenService
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
                rootConfig,
                refreshTokenService,
                accessTokenService
            )
        }

        authenticate("auth-jwt") {
            post("/logout") {
                call.handleLogout(
                    rootConfig,
                    refreshTokenService
                )
            }
        }
    }
}

private suspend fun ApplicationCall.handleRefresh(
    rootConfig: RootConfig,
    refreshTokenService: RefreshTokenService,
    accessTokenService: AppJwtService
) {
    if (!ensureAllowedOrigin(rootConfig.app.allowedOriginsMapped)) {
        return
    }

    val rotation = refreshTokenService.rotateRefreshToken(
        request.cookies[REFRESH_TOKEN_COOKIE_NAME] ?: "",
        readSessionClientMetadata()
    )

    if (rotation == null) {
        clearAuthAccessCookie(rootConfig)
        clearRefreshTokenCookie(rootConfig)
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Refresh token is invalid or expired"))
        return
    }

    appendAuthAccessCookie(accessTokenService.issueAccessToken(rotation.user), rootConfig)
    appendRefreshTokenCookie(rotation.refreshToken, rootConfig)
    respond(HttpStatusCode.NoContent)
}

private suspend fun ApplicationCall.handleLogout(
    rootConfig: RootConfig,
    refreshTokenService: RefreshTokenService
) {
    if (!ensureAllowedOrigin(rootConfig.app.allowedOriginsMapped)) {
        return
    }

    refreshTokenService.revokeRefreshToken(request.cookies[REFRESH_TOKEN_COOKIE_NAME])
    clearAuthAccessCookie(rootConfig)
    clearFrontendRedirectContextCookies(rootConfig)
    clearGoogleOauthStateCookie(rootConfig)
    clearRefreshTokenCookie(rootConfig)

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

internal fun ApplicationCall.readSessionClientMetadata(): SessionClientMetadata {
    val remoteHost = request.local.remoteHost
        .takeIf { it.isNotBlank() && !it.equals("unknown", ignoreCase = true) }

    return SessionClientMetadata(
        userAgent = request.headers[HttpHeaders.UserAgent]?.takeIf { it.isNotBlank() },
        ipAddress = remoteHost
    )
}
