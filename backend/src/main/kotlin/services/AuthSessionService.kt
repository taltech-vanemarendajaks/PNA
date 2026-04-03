package com.pna.backend.services

import domain.auth.GoogleUser
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class AuthSessionService(
    private val ttlSeconds: Long = 3600,
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    private data class SessionRecord(
        val user: GoogleUser,
        val expiresAtMillis: Long
    )

    private val sessions = ConcurrentHashMap<String, SessionRecord>()

    fun create(user: GoogleUser): String {
        removeExpiredSessions()

        val sessionId = UUID.randomUUID().toString()
        sessions[sessionId] = SessionRecord(
            user = user,
            expiresAtMillis = nowMillis() + (ttlSeconds * 1000)
        )
        return sessionId
    }

    fun get(sessionId: String?): GoogleUser? {
        if (sessionId.isNullOrBlank()) {
            return null
        }

        val session = sessions[sessionId] ?: return null
        if (session.expiresAtMillis <= nowMillis()) {
            sessions.remove(sessionId)
            return null
        }

        return session.user
    }

    fun clear(sessionId: String?) {
        if (sessionId.isNullOrBlank()) {
            return
        }

        sessions.remove(sessionId)
    }

    private fun removeExpiredSessions() {
        val now = nowMillis()
        sessions.entries.removeIf { (_, session) -> session.expiresAtMillis <= now }
    }
}
