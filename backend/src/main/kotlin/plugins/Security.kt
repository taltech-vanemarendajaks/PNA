package com.pna.backend.plugins

import com.auth0.jwt.JWT
import com.pna.backend.services.AppJwtService
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*

fun Application.configureSecurity(
    accessTokenService: AppJwtService,
    issuer: String,
    audience: String
) {
    install(Authentication) {
        jwt("auth-jwt") {
            realm = "api"
            verifier(
                JWT
                    .require(accessTokenService.algorithm())
                    .withIssuer(issuer)
                    .withAudience(audience)
                    .build()
            )
            validate { credential ->
                val subject = credential.payload.subject
                if (subject.isNullOrBlank()) {
                    null
                } else {
                    JWTPrincipal(credential.payload)
                }
            }
        }
    }
}