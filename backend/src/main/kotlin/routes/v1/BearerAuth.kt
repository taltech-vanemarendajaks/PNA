package com.pna.backend.routes.v1

import domain.auth.GoogleUser
import io.ktor.http.*
import io.ktor.server.application.*

fun ApplicationCall.readAuthenticatedUser(
    verifyAccessToken: (String) -> GoogleUser?
): GoogleUser? {
    val authHeader = request.headers[HttpHeaders.Authorization] ?: return null
    val token = extractBearerToken(authHeader)
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
