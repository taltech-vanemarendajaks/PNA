package com.pna.backend.routes.v1.websocket

import com.pna.backend.routes.v1.readAuthenticatedUser
import com.pna.backend.services.WebSocketSessionService
import domain.auth.GoogleUser
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.*

fun Route.realtimeRoutes(
    verifyAccessToken: (String) -> GoogleUser?,
    sessionManager: WebSocketSessionService
) {
    webSocket("/api/ws/number") {
        println("WS cookies: ${call.request.cookies.rawCookies}")
        println("WS auth header: ${call.request.headers["Authorization"]}")

        val user = call.readAuthenticatedUser(verifyAccessToken)
        println("WS authenticated user: $user")

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