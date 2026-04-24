package com.pna.backend.domain.auth.response

import kotlinx.serialization.Serializable

@Serializable
data class SavedNumberSearchResponse(
    val id: String,
    val number: String,
    val results: List<SavedPhoneNumberLookupResult>,
    val createdAt: String,
)

@Serializable
data class SavedPhoneNumberLookupResult(
    val country: String? = null,
    val countryCode: Int? = null,
    val regionCode: String? = null,
    val numberType: String? = null,
    val internationalFormat: String? = null,
    val carrier: String? = null,
    val timeZones: List<String>? = null,
    val createdAt: String,
)
