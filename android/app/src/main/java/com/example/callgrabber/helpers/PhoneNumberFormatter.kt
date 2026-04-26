package com.example.callgrabber.helpers

import com.example.callgrabber.PhoneNumberLookupResult

fun PhoneNumberLookupResult.toDisplayText(fallbackNumber: String): String {
    val number = internationalFormat ?: fallbackNumber

    val titleParts = listOfNotNull(
        number,
        country,
        carrier
    )

    val details = buildList {
        if (!numberType.isNullOrBlank()) {
            add("Type: $numberType")
        }

        if (!regionCode.isNullOrBlank()) {
            add("Region: $regionCode")
        }

        if (!timeZones.isNullOrEmpty()) {
            add("Time zone: ${timeZones.joinToString(", ")}")
        }
    }

    return buildString {
        append(titleParts.joinToString(" • "))

        if (details.isNotEmpty()) {
            append("\n")
            append(details.joinToString("\n"))
        }
    }
}

fun PhoneNumberLookupResult.toCallerTitle(fallbackNumber: String): String {
    val number = internationalFormat ?: fallbackNumber

    return listOfNotNull(
        carrier?.takeIf { it.isNotBlank() },
        country?.takeIf { it.isNotBlank() },
        number
    ).joinToString(" • ")
}