package com.pna.backend.domain.auth.request

import kotlinx.serialization.Serializable

@Serializable
data class PhoneNumberLookupRequest(
    val phoneNumber: String
)