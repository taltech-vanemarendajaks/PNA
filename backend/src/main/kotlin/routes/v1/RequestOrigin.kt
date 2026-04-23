package com.pna.backend.routes.v1

import com.pna.backend.config.RootConfig
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall

fun ApplicationCall.hasAllowedOrigin(rootConfig: RootConfig): Boolean {
    val origin = request.headers[HttpHeaders.Origin]?.trim() ?: return false

    if (origin.equals("null", ignoreCase = true)) {
        return false
    }

    return  RootConfig.isAllowedOrigin(origin, rootConfig.app.allowedOriginsMapped)
}