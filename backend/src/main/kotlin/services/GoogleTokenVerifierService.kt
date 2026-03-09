package auth

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory

class GoogleTokenVerifierService(clientId: String) {
    private val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
        .setAudience(listOf(clientId))
        .build()

    fun verify(idToken: String): GoogleUser? {
        val token = verifier.verify(idToken) ?: return null
        val payload = token.payload

        return GoogleUser(
            subject = payload.subject,
            email = payload.email,
            emailVerified = payload.emailVerified,
            name = payload["name"] as? String,
            picture = payload["picture"] as? String,
            givenName = payload["given_name"] as? String,
            familyName = payload["family_name"] as? String
        )
    }
}
