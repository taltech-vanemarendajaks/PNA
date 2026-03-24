package com.pna.backend.routes.v1.number

import com.pna.backend.domain.auth.request.SearchNumberRequest
import com.pna.backend.domain.auth.response.SearchNumberResponse
import com.pna.backend.services.GoogleTokenVerifierService
import domain.auth.GoogleUser
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.numberRoutes(
    googleClientId: String?,
    verifyToken: ((String) -> GoogleUser?)? = googleClientId
        ?.takeIf { it.isNotBlank() }
        ?.let { GoogleTokenVerifierService(it) }
        ?.let { verifier -> { idToken -> verifier.verify(idToken) } }
) {

    route("/api/v1/number") {
        post("/search") {
            if (verifyToken == null) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "GOOGLE_CLIENT_ID is not configured"))
                return@post
            }

            val authHeader = call.request.headers[HttpHeaders.Authorization]
            val idToken = authHeader
                ?.takeIf { it.startsWith("Bearer ", ignoreCase = true) }
                ?.removePrefix("Bearer ")
                ?.trim()

            if (idToken.isNullOrBlank()) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Missing or invalid Authorization Bearer token"))
                return@post
            }

            val user = verifyToken(idToken)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid Google ID token"))
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
