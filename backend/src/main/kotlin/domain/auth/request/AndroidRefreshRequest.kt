package domain.auth.request

import kotlinx.serialization.Serializable

@Serializable
data class AndroidRefreshRequest(
    val refreshToken: String
)