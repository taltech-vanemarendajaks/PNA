package com.pna.backend.routes.v1.auth

import com.pna.backend.config.AppConfig
import com.pna.backend.config.CorsOrigin
import com.pna.backend.services.AuthSessionService
import com.pna.backend.services.GoogleAuthCodeService
import com.pna.backend.services.GoogleTokenVerifierService
import domain.auth.GoogleUser
import domain.auth.response.GoogleAuthResponse
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.*

private const val FRONTEND_ORIGIN_COOKIE_NAME = "pna_frontend_origin"
private const val RETURN_PATH_COOKIE_NAME = "pna_return_path"
private const val FRONTEND_ORIGIN_QUERY_PARAMETER = "frontendOrigin"
private const val RETURN_PATH_QUERY_PARAMETER = "returnPath"
private const val GOOGLE_OAUTH_STATE_COOKIE_NAME = "pna_google_oauth_state"
private const val GOOGLE_OAUTH_STATE_TTL_SECONDS = 600
private const val REDIRECT_CONTEXT_TTL_SECONDS = 600
private const val GOOGLE_AUTH_SCOPE = "openid email profile"
private const val OAUTH_FLOW_COOKIE_SAME_SITE = "Lax"

private data class FrontendRedirectContext(
    val returnPath: String?,
    val redirectBaseUrl: String
)

private data class AuthRouteSettings(
    val publicBackendBaseUrl: String,
    val frontendBaseUrl: String,
    val allowedOrigins: List<CorsOrigin>,
    val sessionTtlSeconds: Long,
    val authCookieSecure: Boolean,
    val authCookieSameSite: String
)

fun Route.googleAuthRoutes(
    googleClientId: String?,
    googleClientSecret: String?,
    publicBackendBaseUrl: String,
    frontendBaseUrl: String,
    allowedOrigins: List<CorsOrigin>,
    authSessionService: AuthSessionService,
    sessionTtlSeconds: Long,
    authCookieSecure: Boolean,
    authCookieSameSite: String,
    verifyGoogleCredential: ((String) -> GoogleUser?)? = null,
    exchangeGoogleAuthCode: ((String, String) -> String?)? = null
) {
    val tokenVerifier: ((String) -> GoogleUser?)? = verifyGoogleCredential ?: googleClientId
        ?.takeIf { it.isNotBlank() }
        ?.let { clientId ->
            val verifier = GoogleTokenVerifierService(clientId)
            fun verifyCredential(credential: String): GoogleUser? = verifier.verify(credential)
            ::verifyCredential
        }
    val authCodeExchanger: ((String, String) -> String?)? = exchangeGoogleAuthCode ?: run {
        if (googleClientId.isNullOrBlank() || googleClientSecret.isNullOrBlank()) {
            null
        } else {
            val service = GoogleAuthCodeService(googleClientId, googleClientSecret)
            fun exchangeCode(code: String, redirectUri: String): String? = service.exchangeCodeForIdToken(code, redirectUri)
            ::exchangeCode
        }
    }
    val settings = AuthRouteSettings(
        publicBackendBaseUrl = publicBackendBaseUrl,
        frontendBaseUrl = frontendBaseUrl,
        allowedOrigins = allowedOrigins,
        sessionTtlSeconds = sessionTtlSeconds,
        authCookieSecure = authCookieSecure,
        authCookieSameSite = authCookieSameSite
    )

    route("/api/v1/auth") {
        get("/google/redirect") { call.handleGoogleRedirect(authSessionService, settings, googleClientId, authCodeExchanger, tokenVerifier) }
        get("/session") { call.handleSession(authSessionService) }
        post("/logout") { call.handleLogout(authSessionService, settings) }
    }
}

private suspend fun io.ktor.server.application.ApplicationCall.handleGoogleRedirect(
    authSessionService: AuthSessionService,
    settings: AuthRouteSettings,
    googleClientId: String?,
    authCodeExchanger: ((String, String) -> String?)?,
    tokenVerifier: ((String) -> GoogleUser?)?
) {
    val callbackError = request.queryParameters["error"]
    val authorizationCode = request.queryParameters["code"]
    val callbackState = request.queryParameters["state"]

    if (!callbackError.isNullOrBlank() || !authorizationCode.isNullOrBlank() || !callbackState.isNullOrBlank()) {
        val redirectContext = readStoredFrontendRedirectContext(settings.frontendBaseUrl, settings.allowedOrigins)
        handleGoogleRedirectCallback(
            authSessionService = authSessionService,
            settings = settings,
            redirectContext = redirectContext,
            callbackError = callbackError,
            authorizationCode = authorizationCode,
            callbackState = callbackState,
            authCodeExchanger = authCodeExchanger,
            tokenVerifier = tokenVerifier
        )
        return
    }

    val redirectContext = readRequestedFrontendRedirectContext(settings.frontendBaseUrl, settings.allowedOrigins)

    val existingUser = authSessionService.get(request.cookies[AUTH_SESSION_COOKIE_NAME])

    if (existingUser != null) {
        respondRedirect(buildFrontendRedirect(redirectContext.redirectBaseUrl, returnPath = redirectContext.returnPath))
        return
    }

    if (googleClientId.isNullOrBlank()) {
        respondRedirectError(redirectContext, "GOOGLE_CLIENT_ID is not configured")
        return
    }

    if (authCodeExchanger == null || tokenVerifier == null) {
        respondRedirectError(redirectContext, "GOOGLE_CLIENT_SECRET is not configured")
        return
    }

    val state = generateGoogleOauthState()
    appendFrontendRedirectContextCookies(redirectContext, settings)
    appendGoogleOauthStateCookie(state, settings)
    respondRedirect(buildGoogleAuthorizationUrl(googleClientId, buildGoogleRedirectCallbackUri(settings), state))
}

private suspend fun io.ktor.server.application.ApplicationCall.handleGoogleRedirectCallback(
    authSessionService: AuthSessionService,
    settings: AuthRouteSettings,
    redirectContext: FrontendRedirectContext,
    callbackError: String?,
    authorizationCode: String?,
    callbackState: String?,
    authCodeExchanger: ((String, String) -> String?)?,
    tokenVerifier: ((String) -> GoogleUser?)?
) {
    val storedState = request.cookies[GOOGLE_OAUTH_STATE_COOKIE_NAME]
    clearFrontendRedirectContextCookies(settings)
    clearGoogleOauthStateCookie(settings)

    if (!callbackError.isNullOrBlank()) {
        respondRedirectError(redirectContext, "Google login failed: ${callbackError.replace('_', ' ')}")
        return
    }

    if (callbackState.isNullOrBlank() || storedState.isNullOrBlank() || callbackState != storedState) {
        respondRedirectError(redirectContext, "Google login state mismatch")
        return
    }

    if (authorizationCode.isNullOrBlank() || authCodeExchanger == null || tokenVerifier == null) {
        respondRedirectError(redirectContext, "Google login could not be completed")
        return
    }

    val idToken = authCodeExchanger(authorizationCode, buildGoogleRedirectCallbackUri(settings))
    if (idToken.isNullOrBlank()) {
        respondRedirectError(redirectContext, "Google login code exchange failed")
        return
    }

    val user = verifyGoogleUser(idToken, tokenVerifier)
    if (user == null) {
        respondRedirectError(redirectContext, "Invalid Google ID token")
        return
    }

    appendAuthSessionCookie(user, authSessionService, settings)
    respondFrontendRedirect(buildFrontendRedirect(redirectContext.redirectBaseUrl, returnPath = redirectContext.returnPath))
}

private suspend fun io.ktor.server.application.ApplicationCall.handleSession(
    authSessionService: AuthSessionService
) {
    val user = authSessionService.get(request.cookies[AUTH_SESSION_COOKIE_NAME])
    if (user == null) {
        respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
        return
    }

    respond(HttpStatusCode.OK, user.toResponse())
}

private suspend fun io.ktor.server.application.ApplicationCall.handleLogout(
    authSessionService: AuthSessionService,
    settings: AuthRouteSettings
) {
    authSessionService.clear(request.cookies[AUTH_SESSION_COOKIE_NAME])
    clearAuthSessionCookie(settings)
    respond(HttpStatusCode.NoContent)
}

private suspend fun io.ktor.server.application.ApplicationCall.respondRedirectError(
    redirectContext: FrontendRedirectContext,
    error: String
) {
    respondFrontendRedirect(
        buildFrontendRedirect(
            redirectContext.redirectBaseUrl,
            returnPath = redirectContext.returnPath,
            error = error
        )
    )
}

private suspend fun io.ktor.server.application.ApplicationCall.respondFrontendRedirect(location: String) {
    response.headers.append(HttpHeaders.Location, location)
    respondText(
        text = """
            <!doctype html>
            <html lang=\"en\">
              <head>
                <meta charset=\"utf-8\" />
                <meta http-equiv=\"refresh\" content=\"0;url=${escapeHtmlAttribute(location)}\" />
                <title>Redirecting…</title>
              </head>
              <body>
                <script>
                  window.location.replace(${location.toJsStringLiteral()});
                </script>
                <p>Redirecting… <a href=\"${escapeHtmlAttribute(location)}\">Continue</a></p>
              </body>
            </html>
        """.trimIndent(),
        contentType = ContentType.Text.Html,
        status = HttpStatusCode.OK
    )
}

private fun io.ktor.server.application.ApplicationCall.readRequestedFrontendRedirectContext(
    frontendBaseUrl: String,
    allowedOrigins: List<CorsOrigin>
): FrontendRedirectContext {
    val returnPath = request.queryParameters[RETURN_PATH_QUERY_PARAMETER]
    val frontendOrigin = request.queryParameters[FRONTEND_ORIGIN_QUERY_PARAMETER]

    return FrontendRedirectContext(
        returnPath = returnPath,
        redirectBaseUrl = resolveFrontendBaseUrl(frontendBaseUrl, frontendOrigin, allowedOrigins)
    )
}

private fun io.ktor.server.application.ApplicationCall.readStoredFrontendRedirectContext(
    frontendBaseUrl: String,
    allowedOrigins: List<CorsOrigin>
): FrontendRedirectContext {
    val returnPath = readCookieValue(request.cookies[RETURN_PATH_COOKIE_NAME])
    val frontendOrigin = readCookieValue(request.cookies[FRONTEND_ORIGIN_COOKIE_NAME])

    return FrontendRedirectContext(
        returnPath = returnPath,
        redirectBaseUrl = resolveFrontendBaseUrl(frontendBaseUrl, frontendOrigin, allowedOrigins)
    )
}

private fun verifyGoogleUser(
    credential: String,
    tokenVerifier: (String) -> GoogleUser?
): GoogleUser? = runCatching { tokenVerifier(credential) }.getOrNull()

private fun io.ktor.server.application.ApplicationCall.appendAuthSessionCookie(
    user: GoogleUser,
    authSessionService: AuthSessionService,
    settings: AuthRouteSettings
) {
    val sessionId = authSessionService.create(user)
    appendCookie(
        name = AUTH_SESSION_COOKIE_NAME,
        value = sessionId,
        path = "/",
        maxAge = settings.sessionTtlSeconds.toInt(),
        settings = settings
    )
}

private fun io.ktor.server.application.ApplicationCall.clearAuthSessionCookie(
    settings: AuthRouteSettings
) {
    clearCookie(
        name = AUTH_SESSION_COOKIE_NAME,
        path = "/",
        settings = settings
    )
}

private fun io.ktor.server.application.ApplicationCall.appendGoogleOauthStateCookie(
    state: String,
    settings: AuthRouteSettings
) {
    appendCookie(
        name = GOOGLE_OAUTH_STATE_COOKIE_NAME,
        value = state,
        path = "/api/v1/auth/google/redirect",
        maxAge = GOOGLE_OAUTH_STATE_TTL_SECONDS,
        settings = settings,
        sameSite = OAUTH_FLOW_COOKIE_SAME_SITE
    )
}

private fun io.ktor.server.application.ApplicationCall.clearGoogleOauthStateCookie(
    settings: AuthRouteSettings
) {
    clearCookie(
        name = GOOGLE_OAUTH_STATE_COOKIE_NAME,
        path = "/api/v1/auth/google/redirect",
        settings = settings,
        sameSite = OAUTH_FLOW_COOKIE_SAME_SITE
    )
}

private fun io.ktor.server.application.ApplicationCall.appendFrontendRedirectContextCookies(
    redirectContext: FrontendRedirectContext,
    settings: AuthRouteSettings
) {
    appendCookie(
        name = FRONTEND_ORIGIN_COOKIE_NAME,
        value = urlEncode(redirectContext.redirectBaseUrl),
        path = "/api/v1/auth/google/redirect",
        maxAge = REDIRECT_CONTEXT_TTL_SECONDS,
        settings = settings,
        sameSite = OAUTH_FLOW_COOKIE_SAME_SITE
    )
    appendCookie(
        name = RETURN_PATH_COOKIE_NAME,
        value = urlEncode(redirectContext.returnPath ?: "/"),
        path = "/api/v1/auth/google/redirect",
        maxAge = REDIRECT_CONTEXT_TTL_SECONDS,
        settings = settings,
        sameSite = OAUTH_FLOW_COOKIE_SAME_SITE
    )
}

private fun io.ktor.server.application.ApplicationCall.clearFrontendRedirectContextCookies(
    settings: AuthRouteSettings
) {
    clearCookie(
        name = FRONTEND_ORIGIN_COOKIE_NAME,
        path = "/api/v1/auth/google/redirect",
        settings = settings,
        sameSite = OAUTH_FLOW_COOKIE_SAME_SITE
    )
    clearCookie(
        name = RETURN_PATH_COOKIE_NAME,
        path = "/api/v1/auth/google/redirect",
        settings = settings,
        sameSite = OAUTH_FLOW_COOKIE_SAME_SITE
    )
}

private fun io.ktor.server.application.ApplicationCall.appendCookie(
    name: String,
    value: String,
    path: String,
    maxAge: Int,
    settings: AuthRouteSettings,
    sameSite: String = settings.authCookieSameSite
) {
    response.cookies.append(
        Cookie(
            name = name,
            value = value,
            path = path,
            maxAge = maxAge,
            secure = settings.authCookieSecure,
            httpOnly = true,
            extensions = mapOf("SameSite" to sameSite)
        )
    )
}

private fun io.ktor.server.application.ApplicationCall.clearCookie(
    name: String,
    path: String,
    settings: AuthRouteSettings,
    sameSite: String = settings.authCookieSameSite
) {
    response.cookies.append(
        Cookie(
            name = name,
            value = "",
            path = path,
            maxAge = 0,
            expires = GMTDate.START,
            secure = settings.authCookieSecure,
            httpOnly = true,
            extensions = mapOf("SameSite" to sameSite)
        )
    )
}

private fun buildGoogleRedirectCallbackUri(settings: AuthRouteSettings): String =
    "${settings.publicBackendBaseUrl}/api/v1/auth/google/redirect"

private fun buildGoogleAuthorizationUrl(
    clientId: String,
    redirectUri: String,
    state: String
): String {
    val query = listOf(
        "client_id" to clientId,
        "redirect_uri" to redirectUri,
        "response_type" to "code",
        "scope" to GOOGLE_AUTH_SCOPE,
        "state" to state,
        "prompt" to "select_account"
    ).joinToString("&") { (key, value) ->
        "$key=${urlEncode(value)}"
    }

    return "https://accounts.google.com/o/oauth2/v2/auth?$query"
}

private fun generateGoogleOauthState(): String {
    val bytes = ByteArray(24)
    SecureRandom().nextBytes(bytes)
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}

private fun resolveFrontendBaseUrl(
    configuredFrontendBaseUrl: String,
    requestedFrontendOrigin: String?,
    allowedOrigins: List<CorsOrigin>
): String {
    return requestedFrontendOrigin
        ?.trim()
        ?.trimEnd('/')
        ?.takeIf { AppConfig.isAllowedOrigin(it, allowedOrigins) }
        ?: configuredFrontendBaseUrl
}

private fun readCookieValue(value: String?): String? {
    if (value.isNullOrBlank()) {
        return null
    }

    return runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8) }
        .getOrNull()
        ?.takeIf { it.isNotBlank() }
}

private fun escapeHtmlAttribute(value: String): String = buildString(value.length) {
    value.forEach { character ->
        when (character) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(character)
        }
    }
}

private fun String.toJsStringLiteral(): String = buildString(length + 2) {
    append('"')
    this@toJsStringLiteral.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '<' -> append("\\u003C")
            '>' -> append("\\u003E")
            '&' -> append("\\u0026")
            else -> append(character)
        }
    }
    append('"')
}

private fun buildFrontendRedirect(frontendBaseUrl: String, returnPath: String? = null, error: String? = null): String {
    val safePath = sanitizeReturnPath(returnPath)
    val baseRedirectUrl = "$frontendBaseUrl$safePath"

    if (error.isNullOrBlank()) {
        return baseRedirectUrl
    }

    val separator = if ('?' in safePath) '&' else '?'
    return "$baseRedirectUrl$separator authError=${urlEncode(error)}".replace("$separator ", "$separator")
}

private fun sanitizeReturnPath(returnPath: String?): String {
    if (returnPath.isNullOrBlank()) {
        return "/"
    }

    val trimmed = returnPath.trim()
    return if (trimmed.startsWith("/") && !trimmed.startsWith("//")) trimmed else "/"
}

private fun domain.auth.GoogleUser.toResponse(): GoogleAuthResponse = GoogleAuthResponse(
    subject = subject,
    email = email,
    emailVerified = emailVerified,
    name = name,
    picture = picture,
    givenName = givenName,
    familyName = familyName
)

private fun urlEncode(value: String): String = java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8)
