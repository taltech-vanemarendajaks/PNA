package com.pna.backend.domain.auth.request

import kotlinx.serialization.Serializable

@Serializable
data class SearchNumberRequest(
    val number: String
)
