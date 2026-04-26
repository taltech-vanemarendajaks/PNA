package domain.auth.response

import kotlinx.serialization.Serializable

@Serializable
data class AndroidAuthResponse(
    val token: String,
    val refreshToken: String,
    val displayName: String?
)