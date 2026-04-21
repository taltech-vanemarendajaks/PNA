package com.pna.backend.routes.v1.auth

import com.pna.backend.config.CorsOrigin
import com.pna.backend.config.RootConfig
import io.ktor.http.Cookie
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.date.*

internal const val FRONTEND_ORIGIN_COOKIE_NAME = "pna_frontend_origin"
internal const val RETURN_PATH_COOKIE_NAME = "pna_return_path"
internal const val FRONTEND_ORIGIN_QUERY_PARAMETER = "frontendOrigin"
internal const val RETURN_PATH_QUERY_PARAMETER = "returnPath"
internal const val GOOGLE_OAUTH_STATE_COOKIE_NAME = "pna_google_oauth_state"
internal const val AUTH_ACCESS_COOKIE_NAME = "pna_access_token"
internal const val REFRESH_TOKEN_COOKIE_NAME = "pna_refresh_token"
internal const val REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth"
internal const val GOOGLE_AUTH_SCOPE = "openid email profile"
internal const val GOOGLE_REDIRECT_PATH = "/api/v1/auth/google/redirect"
private const val AUTH_COOKIE_PATH = "/api"

internal fun ApplicationCall.appendAuthAccessCookie(
    accessToken: String,
    rootConfig: RootConfig
) {
    appendCookie(
        AUTH_ACCESS_COOKIE_NAME,
        accessToken,
        AUTH_COOKIE_PATH,
        rootConfig.jwt.ttlSeconds,
        rootConfig
    )
}

internal fun ApplicationCall.clearAuthAccessCookie(rootConfig: RootConfig) {
    clearCookie(
        AUTH_ACCESS_COOKIE_NAME,
        AUTH_COOKIE_PATH,
        rootConfig
    )
}

internal fun ApplicationCall.appendGoogleOauthStateCookie(
    state: String,
    rootConfig: RootConfig
) {
    appendCookie(
        GOOGLE_OAUTH_STATE_COOKIE_NAME,
        state,
        GOOGLE_REDIRECT_PATH,
        rootConfig.app.googleOauthStateTtlSeconds,
        rootConfig,
        rootConfig.app.oauthFlowCookieSameSite
    )
}

internal fun ApplicationCall.appendRefreshTokenCookie(
    refreshToken: String,
    rootConfig: RootConfig
) {
    appendCookie(
        REFRESH_TOKEN_COOKIE_NAME,
        refreshToken,
        REFRESH_TOKEN_COOKIE_PATH,
        rootConfig.jwt.refreshTokenTtlSeconds.coerceAtMost(Int.MAX_VALUE),
        rootConfig
    )
}

internal fun ApplicationCall.clearRefreshTokenCookie(rootConfig: RootConfig) {
    clearCookie(
        REFRESH_TOKEN_COOKIE_NAME,
        REFRESH_TOKEN_COOKIE_PATH,
        rootConfig
    )
}

internal fun ApplicationCall.clearGoogleOauthStateCookie(rootConfig: RootConfig) {
    clearCookie(
        GOOGLE_OAUTH_STATE_COOKIE_NAME,
        GOOGLE_REDIRECT_PATH,
        rootConfig,
        rootConfig.app.oauthFlowCookieSameSite
    )
}

internal fun ApplicationCall.appendFrontendRedirectContextCookies(
    redirectContext: FrontendRedirectContext,
    rootConfig: RootConfig
) {
    appendCookie(
        FRONTEND_ORIGIN_COOKIE_NAME,
        urlEncode(redirectContext.redirectBaseUrl),
        GOOGLE_REDIRECT_PATH,
        rootConfig.app.redirectContextTtlSeconds,
        rootConfig,
        rootConfig.app.oauthFlowCookieSameSite
    )
    appendCookie(
        RETURN_PATH_COOKIE_NAME,
        urlEncode(redirectContext.returnPath ?: "/"),
        GOOGLE_REDIRECT_PATH,
        rootConfig.app.redirectContextTtlSeconds,
        rootConfig,
        rootConfig.app.oauthFlowCookieSameSite
    )
}

internal fun ApplicationCall.clearFrontendRedirectContextCookies(rootConfig: RootConfig) {
    clearCookie(
        FRONTEND_ORIGIN_COOKIE_NAME,
        GOOGLE_REDIRECT_PATH,
        rootConfig,
        rootConfig.app.oauthFlowCookieSameSite
    )
    clearCookie(
        RETURN_PATH_COOKIE_NAME,
        GOOGLE_REDIRECT_PATH,
        rootConfig,
        rootConfig.app.oauthFlowCookieSameSite
    )
}

internal suspend fun ApplicationCall.ensureAllowedOrigin(allowedOrigins: List<CorsOrigin>): Boolean {
    if (RootConfig.isAllowedOrigin(request.headers[HttpHeaders.Origin], allowedOrigins)) {
        return true
    }

    respond(HttpStatusCode.Forbidden, mapOf("error" to "Origin is not allowed"))
    return false
}

private fun ApplicationCall.appendCookie(
    name: String,
    value: String,
    path: String,
    maxAge: Int,
    rootConfig: RootConfig,
    sameSite: String = rootConfig.app.authCookieSameSite
) {
    response.cookies.append(
        Cookie(
            name = name,
            value = value,
            path = path,
            maxAge = maxAge,
            secure = rootConfig.app.authCookieSecure,
            httpOnly = true,
            extensions = mapOf("SameSite" to sameSite)
        )
    )
}

private fun ApplicationCall.clearCookie(
    name: String,
    path: String,
    rootConfig: RootConfig,
    sameSite: String = rootConfig.app.authCookieSameSite
) {
    response.cookies.append(
        Cookie(
            name = name,
            value = "",
            path = path,
            maxAge = 0,
            expires = GMTDate.START,
            secure = rootConfig.app.authCookieSecure,
            httpOnly = true,
            extensions = mapOf("SameSite" to sameSite)
        )
    )
}
