package com.pna.backend.services

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper
import com.google.i18n.phonenumbers.PhoneNumberToTimeZonesMapper
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.pna.backend.crawler.Crawler
import com.pna.backend.domain.auth.response.PhoneNumberLookupResult
import java.util.Locale

class PhoneLookupService(
    private val defaultRegion: String = "EE",
    private val crawler: Crawler? = null
) {
    private val phoneNumberUtil = PhoneNumberUtil.getInstance()
    private val carrierMapper = PhoneNumberToCarrierMapper.getInstance()
    private val timeZonesMapper = PhoneNumberToTimeZonesMapper.getInstance()

    fun crawl(phoneNumber: String): String =
        runCatching { crawler?.search(phoneNumber.trim()) }.getOrNull().orEmpty()

    fun lookup(phoneNumber: String): PhoneNumberLookupResult {
        val input = phoneNumber.trim()
        if (input.isBlank()) return PhoneNumberLookupResult()

        val parsed = try {
            phoneNumberUtil.parse(input, defaultRegion)
        } catch (_: NumberParseException) {
            return PhoneNumberLookupResult()
        }

        val regionCode = phoneNumberUtil.getRegionCodeForNumber(parsed)?.takeIf { it.isNotBlank() }
        return PhoneNumberLookupResult(
            country = regionCode?.let { Locale.Builder().setRegion(it).build().getDisplayCountry(Locale.ENGLISH).ifBlank { it } },
            countryCode = parsed.countryCode,
            numberType = phoneNumberUtil.getNumberType(parsed).name.lowercase().replace('_', ' ')
                .replaceFirstChar { it.titlecase(Locale.ENGLISH) },
            internationalFormat = phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL),
            carrier = carrierMapper.getNameForNumber(parsed, Locale.ENGLISH).takeIf { it.isNotBlank() },
            timeZones = timeZonesMapper.getTimeZonesForNumber(parsed).filter { it.isNotBlank() }.takeIf { it.isNotEmpty() }
        )
    }
}
