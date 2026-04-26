package domain.auth.request

import kotlinx.serialization.Serializable

@Serializable
data class AndroidGoogleLoginRequest(
    val idToken: String
)