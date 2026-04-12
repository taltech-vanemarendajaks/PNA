package com.pna.backend.domain.auth.response

import kotlinx.serialization.Serializable

@Serializable
data class SavedNumberSearchResponse(
    val id: Long,
    val number: String,
    val result: PhoneNumberLookupResult,
    val createdAt: String,
)
