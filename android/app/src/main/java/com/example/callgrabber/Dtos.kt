package com.example.callgrabber

data class GoogleLoginRequest(
    val idToken: String
)

data class AuthResponse(
    val token: String,
    val refreshToken: String,
    val displayName: String?
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class RefreshTokenResponse(
    val token: String,
    val refreshToken: String
)

data class SearchNumberRequest(
    val number: String
)

data class SearchNumberResponse(
    val result: PhoneNumberLookupResult?
)

data class PhoneNumberLookupResult(
    val country: String?,
    val countryCode: Int?,
    val regionCode: String?,
    val numberType: String?,
    val internationalFormat: String?,
    val carrier: String?,
    val timeZones: List<String>?
)
