package com.pna.backend.routes.v1.auth


import com.pna.backend.config.RootConfig
import com.pna.backend.services.AppJwtService
import com.pna.backend.services.GoogleAuthCodeService
import com.pna.backend.services.GoogleTokenVerifierService
import com.pna.backend.services.RefreshTokenService
import domain.auth.GoogleUser
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.security.SecureRandom
import java.util.Base64

internal data class GoogleRedirectCallbackParams(
    val error: String?,
    val code: String?,
    val state: String?
)

private enum class GoogleLoginFailure {
    CODE_EXCHANGE_FAILED,
    INVALID_ID_TOKEN
}

private data class GoogleLoginResult(
    val user: GoogleUser? = null,
    val failure: GoogleLoginFailure? = null
)

internal suspend fun ApplicationCall.handleGoogleRedirectRequest(
    rootConfig: RootConfig,
    accessTokenService: AppJwtService,
    googleAuthCodeService: GoogleAuthCodeService,
    googleTokenVerifierService: GoogleTokenVerifierService,
    refreshTokenService: RefreshTokenService
) {
    val callbackParams = readGoogleRedirectCallbackParams()
    if (callbackParams != null) {
        handleGoogleRedirectCallback(
            rootConfig,
            readRedirectContextFromCookies(
                rootConfig.app.frontendBaseUrl,
                rootConfig.app.allowedOriginsMapped
            ),
            callbackParams,
            accessTokenService,
            googleAuthCodeService,
            googleTokenVerifierService,
            refreshTokenService
        )
        return
    }

    val redirectContext = readRedirectContextFromQuery(
        rootConfig.app.frontendBaseUrl,
        rootConfig.app.allowedOriginsMapped
    )

    startGoogleOauthFlow(rootConfig, redirectContext)
}

private suspend fun ApplicationCall.handleGoogleRedirectCallback(
    rootConfig: RootConfig,
    redirectContext: FrontendRedirectContext,
    callbackParams: GoogleRedirectCallbackParams,
    accessTokenService: AppJwtService,
    googleAuthCodeService: GoogleAuthCodeService,
    googleTokenVerifierService: GoogleTokenVerifierService,
    refreshTokenService: RefreshTokenService
) {
    val storedState = request.cookies[GOOGLE_OAUTH_STATE_COOKIE_NAME]

    clearFrontendRedirectContextCookies(rootConfig)
    clearGoogleOauthStateCookie(rootConfig)

    suspend fun fail(message: String) {
        respondRedirectError(redirectContext, message)
    }

    if (!callbackParams.error.isNullOrBlank()) {
        fail("Google login failed: ${callbackParams.error.replace("_", " ")}")
        return
    }

    if (callbackParams.state.isNullOrBlank() || storedState.isNullOrBlank() || callbackParams.state != storedState) {
        fail("Google login state mismatch")
        return
    }

    if (callbackParams.code.isNullOrBlank()) {
        fail("Google login could not be completed")
        return
    }

    val loginResult = exchangeAndVerifyGoogleUser(
        rootConfig,
        callbackParams.code,
        googleAuthCodeService,
        googleTokenVerifierService
    )

    val user = loginResult.user ?: run {
        val message = when (loginResult.failure) {
            GoogleLoginFailure.CODE_EXCHANGE_FAILED -> "Google login code exchange failed"
            GoogleLoginFailure.INVALID_ID_TOKEN, null -> "Invalid Google ID token"
        }
        fail(message)
        return
    }

    val accessToken = accessTokenService.issueAccessToken(user)
    appendAuthAccessCookie(accessToken, rootConfig)
    val refreshToken = refreshTokenService.createRefreshToken(user)
    appendRefreshTokenCookie(refreshToken, rootConfig)
    respondFrontendRedirect(buildFrontendRedirect(redirectContext.redirectBaseUrl, redirectContext.returnPath))
}

private fun ApplicationCall.readGoogleRedirectCallbackParams(): GoogleRedirectCallbackParams? {
    val error = request.queryParameters["error"]
    val code = request.queryParameters["code"]
    val state = request.queryParameters["state"]

    return if (!error.isNullOrBlank() || !code.isNullOrBlank() || !state.isNullOrBlank()) {
        GoogleRedirectCallbackParams(error = error, code = code, state = state)
    } else {
        null
    }
}

private suspend fun ApplicationCall.startGoogleOauthFlow(
    rootConfig: RootConfig,
    redirectContext: FrontendRedirectContext
) {
    val state = generateGoogleOauthState()
    appendFrontendRedirectContextCookies(redirectContext, rootConfig)
    appendGoogleOauthStateCookie(state, rootConfig)

    respondRedirect(
        buildGoogleAuthorizationUrl(
            rootConfig.google.clientId,
            buildGoogleRedirectCallbackUri(rootConfig),
            state
        )
    )
}

private fun exchangeAndVerifyGoogleUser(
    rootConfig: RootConfig,
    authorizationCode: String,
    googleAuthCodeService: GoogleAuthCodeService,
    googleTokenVerifierService: GoogleTokenVerifierService
): GoogleLoginResult {
    val idToken = googleAuthCodeService.exchangeCodeForIdToken(
        code = authorizationCode,
        redirectUri = buildGoogleRedirectCallbackUri(rootConfig)
    )

    if (idToken.isNullOrBlank()) {
        return GoogleLoginResult(failure = GoogleLoginFailure.CODE_EXCHANGE_FAILED)
    }

    val user = runCatching { googleTokenVerifierService.verify(idToken) }.getOrNull()
        ?: return GoogleLoginResult(failure = GoogleLoginFailure.INVALID_ID_TOKEN)

    return GoogleLoginResult(user = user)
}

private fun buildGoogleRedirectCallbackUri(rootConfig: RootConfig): String =
    "${rootConfig.app.publicBackendBaseUrl}$GOOGLE_REDIRECT_PATH"

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
