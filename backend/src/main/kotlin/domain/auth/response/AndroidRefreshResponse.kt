package domain.auth.response

import kotlinx.serialization.Serializable

@Serializable
data class AndroidRefreshResponse(
    val token: String,
    val refreshToken: String
)