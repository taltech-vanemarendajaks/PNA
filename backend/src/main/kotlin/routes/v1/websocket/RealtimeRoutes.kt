package com.pna.backend.routes.v1.websocket

import com.pna.backend.config.RootConfig
import com.pna.backend.routes.v1.hasAllowedOrigin
import com.pna.backend.routes.v1.readAuthenticatedUser
import com.pna.backend.services.WebSocketSessionService
import domain.auth.GoogleUser
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.*

fun Route.realtimeRoutes(
    rootConfig: RootConfig,
    verifyAccessToken: (String) -> GoogleUser?,
    sessionManager: WebSocketSessionService
) {
    webSocket("/api/ws/number") {
        if (!call.hasAllowedOrigin(rootConfig)) {
            call.respond(HttpStatusCode.Forbidden, mapOf("error" to "Invalid origin"))
            return@webSocket
        }

        val user = call.readAuthenticatedUser(verifyAccessToken)

        if (user == null) {
            close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Not authenticated"))
            return@webSocket
        }

        sessionManager.add(user, this)

        try {
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> Unit
                    else -> Unit
                }
            }
        } finally {
            sessionManager.remove(user, this)
        }
    }
}