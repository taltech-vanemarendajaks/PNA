package com.pna.backend.services

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import domain.auth.GoogleUser
import java.time.Clock
import java.util.Date

class AppJwtService(
    private val issuer: String,
    private val audience: String,
    secret: String,
    private val ttlSeconds: Int,
    private val clock: Clock = Clock.systemUTC()
) {
    private val algorithm = Algorithm.HMAC256(secret)
    private val verifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withAudience(audience)
        .build()

    fun issueAccessToken(user: GoogleUser): String {
        val now = clock.instant()
        val expiresAt = now.plusSeconds(ttlSeconds.toLong())

        return JWT.create()
            .withIssuer(issuer)
            .withAudience(audience)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiresAt))
            .withSubject(user.subject)
            .withClaim("email", user.email)
            .withClaim("name", user.name)
            .withClaim("givenName", user.givenName)
            .sign(algorithm)
    }

    fun verify(token: String): GoogleUser? {
        return runCatching {
            val jwt = verifier.verify(token)

            GoogleUser(
                subject = jwt.subject,
                email = jwt.getClaim("email").asString(),
                name = jwt.getClaim("name").asString(),
                givenName = jwt.getClaim("givenName").asString()
            )
        }.getOrNull()
    }

    fun algorithm(): Algorithm = algorithm
}
