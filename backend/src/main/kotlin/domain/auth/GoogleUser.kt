package domain.auth

data class GoogleUser(
    val subject: String,
    val email: String?,
    val emailVerified: Boolean?,
    val name: String?,
    val picture: String?,
    val givenName: String?,
    val familyName: String?
)
