package com.pna.backend.routes.v1.number

import com.pna.backend.config.RootConfig
import com.pna.backend.domain.auth.request.SearchNumberRequest
import com.pna.backend.domain.auth.response.SearchNumberResponse
import com.pna.backend.routes.v1.hasAllowedOrigin
import com.pna.backend.routes.v1.respondPrivateNoStore
import com.pna.backend.routes.v1.readAuthenticatedUser
import com.pna.backend.services.NumberSearchService
import com.pna.backend.services.PhoneLookupService
import domain.auth.GoogleUser
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.numberRoutes(
    rootConfig: RootConfig,
    verifyAccessToken: (String) -> GoogleUser?,
    lookupService: PhoneLookupService,
    numberSearchService: NumberSearchService
) {
    route("/api/v1/number") {
        authenticate("auth-jwt") {
            post("/search") {
                if (!call.hasAllowedOrigin(rootConfig)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Invalid origin"))
                    return@post
                }

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

                val result = numberSearchService.getOrLookup(user, request.number) { number ->
                    lookupService.lookup(number)
                }
                call.respond(HttpStatusCode.OK, SearchNumberResponse(result = result))
            }

            post("/android-search") {

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
        }

        authenticate("auth-jwt") {
            get("/all") {
                if (!call.hasAllowedOrigin(rootConfig)) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Invalid origin"))
                    return@get
                }

                val user = call.readAuthenticatedUser(verifyAccessToken)
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Not authenticated"))
                    return@get
                }

                call.respondPrivateNoStore()
                call.respond(HttpStatusCode.OK, numberSearchService.getAll(user))
            }
        }
    }
}
