package com.pna.backend.services

import domain.auth.GoogleUser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AuthSessionServiceTest {
    @Test
    fun `returns active session before expiry`() {
        var now = 1_000L
        val service = AuthSessionService(ttlSeconds = 60, nowMillis = { now })
        val user = GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")

        val sessionId = service.create(user)

        now += 59_000L

        assertEquals(user, service.get(sessionId))
    }

    @Test
    fun `expired session returns null and is removed`() {
        var now = 1_000L
        val service = AuthSessionService(ttlSeconds = 1, nowMillis = { now })
        val user = GoogleUser("subject", "user@example.com", true, "Jane", null, "Jane", "Doe")

        val sessionId = service.create(user)

        now += 1_000L

        assertNull(service.get(sessionId))
        assertNull(service.get(sessionId))
    }
}
