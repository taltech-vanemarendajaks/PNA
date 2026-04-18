package domain.auth.response

import kotlinx.serialization.Serializable

@Serializable
data class AccessTokenResponse(
    val accessToken: String
)
