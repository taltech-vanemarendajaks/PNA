package com.pna.backend.routes.v1.number

import com.pna.backend.domain.auth.request.SearchNumberRequest
import com.pna.backend.domain.auth.response.SearchNumberResponse
import com.pna.backend.routes.v1.readAuthenticatedUser
import com.pna.backend.services.NumberSearchService
import com.pna.backend.services.PhoneLookupService
import domain.auth.GoogleUser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.numberRoutes(
    verifyAccessToken: (String) -> GoogleUser?,
    lookupService: PhoneLookupService,
    numberSearchService: NumberSearchService
) {
    route("/api/v1/number") {
        post("/search") {
            val user = call.readAuthenticatedUser(verifyAccessToken)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                return@post
            }

            val request = runCatching { call.receive<SearchNumberRequest>() }.getOrNull()
            if (request == null || request.number.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Request must include a non-empty number"))
                return@post
            }

            val result = numberSearchService.getOrLookup(request.number) { number ->
                lookupService.lookup(number)
            }
            call.respond(HttpStatusCode.OK, SearchNumberResponse(result = result))
        }

        get("/all") {
            val user = call.readAuthenticatedUser(verifyAccessToken)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                return@get
            }

            call.respond(HttpStatusCode.OK, numberSearchService.getAll())
        }
    }

}
