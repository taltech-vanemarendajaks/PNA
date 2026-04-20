package com.pna.backend.routes.v1

import io.ktor.http.*
import io.ktor.server.application.*

internal fun ApplicationCall.respondPrivateNoStore() {
    response.headers.append(HttpHeaders.CacheControl, "private, no-store")
}
