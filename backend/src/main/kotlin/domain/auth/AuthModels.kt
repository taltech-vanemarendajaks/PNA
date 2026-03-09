package auth

import kotlinx.serialization.Serializable

@Serializable
data class GoogleAuthRequest(
    val idToken: String
)

@Serializable
data class GoogleAuthResponse(
    val subject: String,
    val email: String?,
    val emailVerified: Boolean?,
    val name: String?,
    val picture: String?,
    val givenName: String?,
    val familyName: String?
)

data class GoogleUser(
    val subject: String,
    val email: String?,
    val emailVerified: Boolean?,
    val name: String?,
    val picture: String?,
    val givenName: String?,
    val familyName: String?
)
