package com.pna.backend.domain.auth.response

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class PhoneNumberLookupResult(
    val country: String? = null,
    val countryCode: Int? = null,
    val numberType: String? = null,
    val internationalFormat: String? = null,
    val carrier: String? = null,
    val timeZones: List<String>? = null,
    @SerialName("singlecrawler_result")
    val singleCrawlerResult: String = "",
)
