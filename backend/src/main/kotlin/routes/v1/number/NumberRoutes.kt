package com.pna.backend.routes.v1.number

import com.pna.backend.domain.auth.request.SearchNumberRequest
import com.pna.backend.domain.auth.response.SearchNumberResponse
import com.pna.backend.routes.v1.auth.AUTH_SESSION_COOKIE_NAME
import com.pna.backend.services.AuthSessionService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.numberRoutes(authSessionService: AuthSessionService) {
    route("/api/v1/number") {
        post("/search") {
            val user = authSessionService.get(call.request.cookies[AUTH_SESSION_COOKIE_NAME])
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                return@post
            }

            val request = runCatching { call.receive<SearchNumberRequest>() }.getOrNull()
            if (request == null || request.number.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Request must include a non-empty number"))
                return@post
            }

            call.respond(
                HttpStatusCode.OK,
                SearchNumberResponse(message = "search was successful")
            )
        }
    }

}
