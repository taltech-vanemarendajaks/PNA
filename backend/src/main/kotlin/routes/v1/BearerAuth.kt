package com.pna.backend.routes.v1

import com.pna.backend.routes.v1.auth.AUTH_ACCESS_COOKIE_NAME
import domain.auth.GoogleUser
import io.ktor.http.*
import io.ktor.server.application.*

fun ApplicationCall.readAuthenticatedUser(
    verifyAccessToken: (String) -> GoogleUser?
): GoogleUser? {
    val token = request.cookies[AUTH_ACCESS_COOKIE_NAME]
        ?.takeIf { it.isNotBlank() }
        ?: request.headers[HttpHeaders.Authorization]?.let(::extractBearerToken)
        ?: return null

    if (token.isBlank()) {
        return null
    }

    return runCatching { verifyAccessToken(token) }.getOrNull()
}

private fun extractBearerToken(authHeader: String): String {
    if (!authHeader.startsWith("Bearer ", ignoreCase = true)) {
        return ""
    }

    return authHeader.substringAfter(' ', "").trim()
}
