package com.pna.backend.routes.v1.auth

import com.pna.backend.config.AppConfig
import com.pna.backend.config.CorsOrigin
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
    appConfig: AppConfig
) {
    appendCookie(
        name = AUTH_ACCESS_COOKIE_NAME,
        value = accessToken,
        path = AUTH_COOKIE_PATH,
        maxAge = appConfig.jwtTtlSeconds.toInt(),
        appConfig = appConfig
    )
}

internal fun ApplicationCall.clearAuthAccessCookie(appConfig: AppConfig) {
    clearCookie(
        name = AUTH_ACCESS_COOKIE_NAME,
        path = AUTH_COOKIE_PATH,
        appConfig = appConfig
    )
}

internal fun ApplicationCall.appendGoogleOauthStateCookie(
    state: String,
    appConfig: AppConfig
) {
    appendCookie(
        name = GOOGLE_OAUTH_STATE_COOKIE_NAME,
        value = state,
        path = GOOGLE_REDIRECT_PATH,
        maxAge = appConfig.googleOauthStateTtlSeconds,
        appConfig = appConfig,
        sameSite = appConfig.oauthFlowCookieSameSite
    )
}

internal fun ApplicationCall.appendRefreshTokenCookie(
    refreshToken: String,
    appConfig: AppConfig
) {
    appendCookie(
        name = REFRESH_TOKEN_COOKIE_NAME,
        value = refreshToken,
        path = REFRESH_TOKEN_COOKIE_PATH,
        maxAge = appConfig.refreshTokenTtlSeconds.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        appConfig = appConfig
    )
}

internal fun ApplicationCall.clearRefreshTokenCookie(appConfig: AppConfig) {
    clearCookie(
        name = REFRESH_TOKEN_COOKIE_NAME,
        path = REFRESH_TOKEN_COOKIE_PATH,
        appConfig = appConfig
    )
}

internal fun ApplicationCall.clearGoogleOauthStateCookie(appConfig: AppConfig) {
    clearCookie(
        name = GOOGLE_OAUTH_STATE_COOKIE_NAME,
        path = GOOGLE_REDIRECT_PATH,
        appConfig = appConfig,
        sameSite = appConfig.oauthFlowCookieSameSite
    )
}

internal fun ApplicationCall.appendFrontendRedirectContextCookies(
    redirectContext: FrontendRedirectContext,
    appConfig: AppConfig
) {
    appendCookie(
        name = FRONTEND_ORIGIN_COOKIE_NAME,
        value = urlEncode(redirectContext.redirectBaseUrl),
        path = GOOGLE_REDIRECT_PATH,
        maxAge = appConfig.redirectContextTtlSeconds,
        appConfig = appConfig,
        sameSite = appConfig.oauthFlowCookieSameSite
    )
    appendCookie(
        name = RETURN_PATH_COOKIE_NAME,
        value = urlEncode(redirectContext.returnPath ?: "/"),
        path = GOOGLE_REDIRECT_PATH,
        maxAge = appConfig.redirectContextTtlSeconds,
        appConfig = appConfig,
        sameSite = appConfig.oauthFlowCookieSameSite
    )
}

internal fun ApplicationCall.clearFrontendRedirectContextCookies(appConfig: AppConfig) {
    clearCookie(
        name = FRONTEND_ORIGIN_COOKIE_NAME,
        path = GOOGLE_REDIRECT_PATH,
        appConfig = appConfig,
        sameSite = appConfig.oauthFlowCookieSameSite
    )
    clearCookie(
        name = RETURN_PATH_COOKIE_NAME,
        path = GOOGLE_REDIRECT_PATH,
        appConfig = appConfig,
        sameSite = appConfig.oauthFlowCookieSameSite
    )
}

internal suspend fun ApplicationCall.ensureAllowedOrigin(allowedOrigins: List<CorsOrigin>): Boolean {
    if (AppConfig.isAllowedOrigin(request.headers[HttpHeaders.Origin], allowedOrigins)) {
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
    appConfig: AppConfig,
    sameSite: String = appConfig.authCookieSameSite
) {
    response.cookies.append(
        Cookie(
            name = name,
            value = value,
            path = path,
            maxAge = maxAge,
            secure = appConfig.authCookieSecure,
            httpOnly = true,
            extensions = mapOf("SameSite" to sameSite)
        )
    )
}

private fun ApplicationCall.clearCookie(
    name: String,
    path: String,
    appConfig: AppConfig,
    sameSite: String = appConfig.authCookieSameSite
) {
    response.cookies.append(
        Cookie(
            name = name,
            value = "",
            path = path,
            maxAge = 0,
            expires = GMTDate.START,
            secure = appConfig.authCookieSecure,
            httpOnly = true,
            extensions = mapOf("SameSite" to sameSite)
        )
    )
}
