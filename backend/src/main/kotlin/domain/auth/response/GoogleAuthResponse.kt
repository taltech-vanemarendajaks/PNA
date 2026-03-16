package domain.auth.response

import kotlinx.serialization.Serializable

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
