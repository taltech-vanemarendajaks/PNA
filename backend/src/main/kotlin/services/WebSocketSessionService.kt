package com.pna.backend.services

import domain.auth.GoogleUser
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

class WebSocketSessionService {
    private val sessionsByUser =
        ConcurrentHashMap<String, MutableSet<DefaultWebSocketServerSession>>()

    private fun userKey(user: GoogleUser): String = user.subject

    fun add(user: GoogleUser, session: DefaultWebSocketServerSession) {
        val key = userKey(user)
        sessionsByUser.computeIfAbsent(key) { ConcurrentHashMap.newKeySet() }.add(session)
        println("WS connected for user=$key, sessions=${sessionsByUser[key]?.size ?: 0}")
    }

    fun remove(user: GoogleUser, session: DefaultWebSocketServerSession) {
        val key = userKey(user)
        sessionsByUser[key]?.remove(session)

        if (sessionsByUser[key].isNullOrEmpty()) {
            sessionsByUser.remove(key)
        }

        println("WS disconnected for user=$key, sessions=${sessionsByUser[key]?.size ?: 0}")
    }

    suspend fun broadcastToUser(user: GoogleUser, message: String) {
        val key = userKey(user)
        val snapshot = sessionsByUser[key]?.toList().orEmpty()

        println("Broadcasting to ${snapshot.size} session(s) for user=$key")

        snapshot.forEach { session ->
            runCatching {
                session.send(Frame.Text(message))
            }.onFailure {
                remove(user, session)
            }
        }
    }
}