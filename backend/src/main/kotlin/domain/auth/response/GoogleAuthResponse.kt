package domain.auth.response

import kotlinx.serialization.Serializable

@Serializable
data class GoogleAuthResponse(
    val subject: String,
    val email: String?,
    val name: String?,
    val givenName: String?
)
