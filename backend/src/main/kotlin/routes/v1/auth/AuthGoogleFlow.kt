package com.pna.backend.routes.v1.auth

import com.pna.backend.config.AppConfig
import com.pna.backend.services.AppJwtService
import com.pna.backend.services.GoogleAuthCodeService
import com.pna.backend.services.GoogleTokenVerifierService
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
    appConfig: AppConfig,
    accessTokenService: AppJwtService,
    googleAuthCodeService: GoogleAuthCodeService,
    googleTokenVerifierService: GoogleTokenVerifierService
) {
    val callbackParams = readGoogleRedirectCallbackParams()
    if (callbackParams != null) {
        handleGoogleRedirectCallback(
            appConfig = appConfig,
            redirectContext = readRedirectContextFromCookies(
                frontendBaseUrl = appConfig.frontendBaseUrl,
                allowedOrigins = appConfig.allowedOrigins
            ),
            accessTokenService = accessTokenService,
            callbackParams = callbackParams,
            googleAuthCodeService = googleAuthCodeService,
            googleTokenVerifierService = googleTokenVerifierService
        )
        return
    }

    val redirectContext = readRedirectContextFromQuery(
        frontendBaseUrl = appConfig.frontendBaseUrl,
        allowedOrigins = appConfig.allowedOrigins
    )

    startGoogleOauthFlow(appConfig, redirectContext)
}

private suspend fun ApplicationCall.handleGoogleRedirectCallback(
    appConfig: AppConfig,
    redirectContext: FrontendRedirectContext,
    accessTokenService: AppJwtService,
    callbackParams: GoogleRedirectCallbackParams,
    googleAuthCodeService: GoogleAuthCodeService,
    googleTokenVerifierService: GoogleTokenVerifierService
) {
    val storedState = request.cookies[GOOGLE_OAUTH_STATE_COOKIE_NAME]

    clearFrontendRedirectContextCookies(appConfig)
    clearGoogleOauthStateCookie(appConfig)

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
        appConfig = appConfig,
        authorizationCode = callbackParams.code,
        googleAuthCodeService = googleAuthCodeService,
        googleTokenVerifierService = googleTokenVerifierService
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
    respondFrontendRedirect(
        buildFrontendRedirect(
            frontendBaseUrl = redirectContext.redirectBaseUrl,
            returnPath = redirectContext.returnPath,
            accessToken = accessToken
        )
    )
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
    appConfig: AppConfig,
    redirectContext: FrontendRedirectContext
) {
    val state = generateGoogleOauthState()
    appendFrontendRedirectContextCookies(redirectContext, appConfig)
    appendGoogleOauthStateCookie(state, appConfig)

    respondRedirect(
        buildGoogleAuthorizationUrl(
            clientId = appConfig.googleClientId,
            redirectUri = buildGoogleRedirectCallbackUri(appConfig),
            state = state
        )
    )
}

private fun exchangeAndVerifyGoogleUser(
    appConfig: AppConfig,
    authorizationCode: String,
    googleAuthCodeService: GoogleAuthCodeService,
    googleTokenVerifierService: GoogleTokenVerifierService
): GoogleLoginResult {
    val idToken = googleAuthCodeService.exchangeCodeForIdToken(
        code = authorizationCode,
        redirectUri = buildGoogleRedirectCallbackUri(appConfig)
    )

    if (idToken.isNullOrBlank()) {
        return GoogleLoginResult(failure = GoogleLoginFailure.CODE_EXCHANGE_FAILED)
    }

    val user = runCatching { googleTokenVerifierService.verify(idToken) }.getOrNull()
        ?: return GoogleLoginResult(failure = GoogleLoginFailure.INVALID_ID_TOKEN)

    return GoogleLoginResult(user = user)
}

private fun buildGoogleRedirectCallbackUri(appConfig: AppConfig): String =
    "${appConfig.publicBackendBaseUrl}$GOOGLE_REDIRECT_PATH"

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
