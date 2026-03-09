package auth

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.googleAuthRoutes() {
    val googleClientId = System.getenv("GOOGLE_CLIENT_ID")
    val verifier = googleClientId?.takeIf { it.isNotBlank() }?.let { GoogleTokenVerifierService(it) }

    route("/api/v1/auth") {
        post("/google") {
            if (verifier == null) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "GOOGLE_CLIENT_ID is not configured"))
                return@post
            }

            val request = runCatching { call.receive<GoogleAuthRequest>() }.getOrNull()
            if (request == null || request.idToken.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Request must include a non-empty idToken"))
                return@post
            }

            val user = verifier.verify(request.idToken)
            if (user == null) {
                call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "Invalid Google ID token"))
                return@post
            }

            call.respond(
                HttpStatusCode.OK,
                GoogleAuthResponse(
                    subject = user.subject,
                    email = user.email,
                    emailVerified = user.emailVerified,
                    name = user.name,
                    picture = user.picture,
                    givenName = user.givenName,
                    familyName = user.familyName
                )
            )
        }
    }
}
