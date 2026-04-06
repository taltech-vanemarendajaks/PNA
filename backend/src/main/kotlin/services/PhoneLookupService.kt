package com.pna.backend.services

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper
import com.google.i18n.phonenumbers.PhoneNumberToTimeZonesMapper
import com.google.i18n.phonenumbers.PhoneNumberUtil
import java.util.Locale

class PhoneLookupService(
    private val defaultRegion: String = System.getenv("PHONE_LOOKUP_DEFAULT_REGION")
        ?.trim()
        ?.ifBlank { null }
        ?.uppercase()
        ?: "EE"
) {
    private val phoneNumberUtil = PhoneNumberUtil.getInstance()
    private val carrierMapper = PhoneNumberToCarrierMapper.getInstance()
    private val timeZonesMapper = PhoneNumberToTimeZonesMapper.getInstance()

    fun lookup(phoneNumber: String): String {
        val input = phoneNumber.trim()
        if (input.isBlank()) {
            return formatSummary(
                country = null,
                countryCode = null,
                regionCode = null,
                numberType = null,
                internationalFormat = null,
                carrier = null,
                timeZones = null
            )
        }

        val parsed = try {
            phoneNumberUtil.parse(input, defaultRegion)
        } catch (_: NumberParseException) {
            return formatSummary(
                country = null,
                countryCode = null,
                regionCode = null,
                numberType = null,
                internationalFormat = null,
                carrier = null,
                timeZones = null
            )
        }

        val regionCode = phoneNumberUtil.getRegionCodeForNumber(parsed)
            ?.takeIf { it.isNotBlank() }

        val country = regionCode?.let {
            Locale.Builder().setRegion(it).build().getDisplayCountry(Locale.ENGLISH).ifBlank { it }
        }

        val countryCode = parsed.countryCode
        val numberType = phoneNumberUtil.getNumberType(parsed)
            .name
            .lowercase()
            .replace('_', ' ')
            .replaceFirstChar { character -> character.titlecase(Locale.ENGLISH) }
        val internationalFormat = phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
        val carrier = carrierMapper.getNameForNumber(parsed, Locale.ENGLISH)
            .takeIf { it.isNotBlank() }
        val timeZones = timeZonesMapper.getTimeZonesForNumber(parsed)
            .filter { it.isNotBlank() }
            .takeIf { it.isNotEmpty() }
            ?.joinToString(", ")

        return formatSummary(
            country = country,
            countryCode = countryCode.toString(),
            regionCode = regionCode,
            numberType = numberType,
            internationalFormat = internationalFormat,
            carrier = carrier,
            timeZones = timeZones
        )
    }

    private fun formatSummary(
        country: String?,
        countryCode: String?,
        regionCode: String?,
        numberType: String?,
        internationalFormat: String?,
        carrier: String?,
        timeZones: String?
    ): String {
        return listOf(
            "country=${country ?: "unknown"}",
            "countryCode=${countryCode ?: "unknown"}",
            "regionCode=${regionCode ?: "unknown"}",
            "numberType=${numberType ?: "unknown"}",
            "internationalFormat=${internationalFormat ?: "unknown"}",
            "carrier=${carrier ?: "unknown"}",
            "timeZones=${timeZones ?: "unknown"}"
        ).joinToString(", ")
    }
}
