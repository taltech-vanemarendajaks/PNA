package com.pna.backend.routes.v1

import com.pna.backend.config.AppConfig
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import java.net.URI

fun ApplicationCall.hasAllowedOrigin(appConfig: AppConfig): Boolean {
    val origin = request.headers[HttpHeaders.Origin]?.trim() ?: return false

    if (origin.equals("null", ignoreCase = true)) {
        return false
    }

    val parsed = runCatching {
        if (origin.contains("://")) {
            val uri = URI(origin)
            Pair(
                uri.authority ?: return false,
                listOfNotNull(uri.scheme)
            )
        } else {
            Pair(
                origin,
                listOf("http", "https")
            )
        }
    }.getOrNull() ?: return false

    val (host, schemes) = parsed

    return appConfig.allowedOrigins.any { allowed ->
        allowed.host.equals(host, ignoreCase = true) &&
                allowed.schemes.any { allowedScheme ->
                    schemes.any { originScheme ->
                        allowedScheme.equals(originScheme, ignoreCase = true)
                    }
                }
    }
}