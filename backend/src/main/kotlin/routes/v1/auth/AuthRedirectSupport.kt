package com.pna.backend.routes.v1.auth

import com.pna.backend.config.AppConfig
import com.pna.backend.config.CorsOrigin
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal data class FrontendRedirectContext(
    val returnPath: String?,
    val redirectBaseUrl: String
)

internal suspend fun ApplicationCall.respondRedirectError(
    redirectContext: FrontendRedirectContext,
    error: String
) {
    respondFrontendRedirect(
        buildFrontendRedirect(
            frontendBaseUrl = redirectContext.redirectBaseUrl,
            returnPath = redirectContext.returnPath,
            error = error
        )
    )
}

internal suspend fun ApplicationCall.respondFrontendRedirect(location: String) {
    response.headers.append(HttpHeaders.Location, location)
    respondText(
        text = buildFrontendRedirectPage(location),
        contentType = ContentType.Text.Html,
        status = HttpStatusCode.OK
    )
}

internal fun ApplicationCall.readRedirectContextFromQuery(
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

internal fun ApplicationCall.readRedirectContextFromCookies(
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

private fun buildFrontendRedirectPage(location: String): String {
    return """
        <!doctype html>
        <html lang="en">
          <head>
            <meta charset="utf-8" />
            <meta http-equiv="refresh" content="0;url=${escapeHtmlAttribute(location)}" />
            <title>Redirecting…</title>
          </head>
          <body>
            <script>
              window.location.replace(${location.toJsStringLiteral()});
            </script>
            <p>Redirecting… <a href="${escapeHtmlAttribute(location)}">Continue</a></p>
          </body>
        </html>
    """.trimIndent()
}

internal fun buildFrontendRedirect(
    frontendBaseUrl: String,
    returnPath: String? = null,
    error: String? = null,
    accessToken: String? = null
): String {
    val safePath = sanitizeReturnPath(returnPath)
    val baseRedirectUrl = "$frontendBaseUrl$safePath"

    if (error.isNullOrBlank()) {
        return if (accessToken.isNullOrBlank()) {
            baseRedirectUrl
        } else {
            "$baseRedirectUrl#accessToken=${urlEncode(accessToken)}"
        }
    }

    val separator = if ('?' in safePath) '&' else '?'
    return "$baseRedirectUrl${separator}authError=${urlEncode(error)}"
}

private fun sanitizeReturnPath(returnPath: String?): String {
    if (returnPath.isNullOrBlank()) {
        return "/"
    }

    val trimmed = returnPath.trim()
    return if (trimmed.startsWith("/") && !trimmed.startsWith("//")) trimmed else "/"
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

internal fun urlEncode(value: String): String =
    java.net.URLEncoder.encode(value, StandardCharsets.UTF_8)
