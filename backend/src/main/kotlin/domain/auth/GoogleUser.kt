package domain.auth

data class GoogleUser(
    val subject: String,
    val email: String?,
    val name: String?,
    val givenName: String?
)
