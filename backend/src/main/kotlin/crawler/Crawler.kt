package com.pna.backend.crawler

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

private data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String
)

class Crawler(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build(),
    private val maxResults: Int = 8,
    private val searchBaseUrl: String = "https://html.duckduckgo.com/html/",
    private val defaultRegion: String = "EE"
) {
    private data class ParsedNumber(
        val localDigits: String,
        val countryCode: String?
    )

    private val phoneNumberUtil = PhoneNumberUtil.getInstance()

    fun search(phoneNumber: String): String {
        val parsedNumber = parseNumber(phoneNumber) ?: return ""
        val variants = variants(parsedNumber)
        val results = searchResults(searchQueries(parsedNumber, variants)).take(maxResults)
        if (results.isEmpty()) {
            return NO_MATCH_FOUND
        }

        for (result in results) {
            if (matches(result.title, variants) || matches(result.snippet, variants)) {
                return formatResult(result.title, result.url)
            }

            val html = fetch(result.url) ?: continue
            if (matches(html, variants)) {
                val title = Jsoup.parse(html).title().ifBlank { result.title.ifBlank { result.url } }
                return formatResult(title, result.url)
            }
        }

        return NO_MATCH_FOUND
    }

    private fun parseNumber(input: String): ParsedNumber? {
        val trimmedInput = input.trim()
        if (trimmedInput.isBlank()) {
            return null
        }

        return try {
            val parsed = phoneNumberUtil.parse(trimmedInput, defaultRegion)
            val localDigits = parsed.nationalNumber.toString().ifBlank { return null }
            ParsedNumber(localDigits = localDigits, countryCode = parsed.countryCode.toString())
        } catch (_: NumberParseException) {
            val digits = trimmedInput.filter(Char::isDigit).ifBlank { return null }
            ParsedNumber(localDigits = digits, countryCode = null)
        }
    }

    private fun variants(number: ParsedNumber): List<String> {
        val digits = number.localDigits
        val localVariants = listOf(
            digits,
            digits.chunked(3).joinToString(" "),
            digits.chunked(2).joinToString(" "),
            digits.chunked(3).joinToString("-"),
            digits.chunked(2).joinToString("-")
        )
        val countryCode = number.countryCode
        if (countryCode == null) {
            return localVariants.distinct()
        }

        val internationalDigits = "$countryCode$digits"
        val internationalVariants = listOf(
            internationalDigits,
            "00$internationalDigits",
            "+$internationalDigits",
            "+$countryCode $digits",
            "+$countryCode-${digits.chunked(3).joinToString("-")}",
            "+$countryCode ${digits.chunked(3).joinToString("-")}",
            "+$countryCode ${digits.chunked(3).joinToString(" ")}",
            "+$countryCode ${digits.chunked(2).joinToString(" ")}",
            "$countryCode ${digits.chunked(3).joinToString(" ")}",
            "$countryCode-${digits.chunked(3).joinToString("-")}"
        )

        return (localVariants + internationalVariants).distinct()
    }

    private fun searchQueries(number: ParsedNumber, variants: List<String>): List<String> {
        val localDigits = number.localDigits
        val countryCode = number.countryCode
        val internationalDigits = countryCode?.let { "$it$localDigits" }
        val quotedAndInternational = buildList {
            if (countryCode != null) {
                add("\"+$countryCode ${localDigits.chunked(3).joinToString(" ")}\"")
                add("\"+$countryCode$localDigits\"")
                add("+$countryCode ${localDigits.chunked(3).joinToString(" ")}")
                add("+$countryCode$localDigits")
            }
            if (internationalDigits != null) {
                add("\"$internationalDigits\"")
                add(internationalDigits)
            }
        }

        return (quotedAndInternational + listOf(
            "\"$localDigits\"",
            localDigits,
            localDigits.takeLast(6),
            localDigits.takeLast(5)
        ) + variants).distinct()
    }

    private fun searchResults(queries: List<String>): List<SearchResult> {
        return queries.flatMap { query ->
            val html = fetch("$searchBaseUrl?q=${URLEncoder.encode(query, "UTF-8")}") ?: return@flatMap emptyList()
            val document = Jsoup.parse(html)
            document.select(".result").mapNotNull { element ->
                val link = element.selectFirst("a.result__a") ?: return@mapNotNull null
                val url = unwrap(link.attr("href")) ?: return@mapNotNull null
                val title = link.text().ifBlank { url }
                val snippet = element.selectFirst(".result__snippet")?.text().orEmpty()
                SearchResult(title = title, url = url, snippet = snippet)
            }
        }.distinctBy { it.url }
    }

    private fun matches(html: String, variants: List<String>): Boolean {
        val normalizedHaystack = html.normalizePhoneDigits()
        val digitVariants = variants.map { it.filter(Char::isDigit) }.filter { it.length >= 7 }
        if (digitVariants.any { it in normalizedHaystack }) {
            return true
        }

        if (digitVariants.any { normalizedHaystack.contains(it.takeLast(6)) || normalizedHaystack.contains(it.takeLast(5)) }) {
            return true
        }

        val text = Jsoup.parse(html).text()
        return variants.any { it in html || it in text }
    }

    private fun formatResult(title: String, url: String): String {
        return "${title.ifBlank { url }} — $url"
    }

    private fun unwrap(href: String): String? {
        val absolute = if (href.startsWith("//")) "https:$href" else href
        val marker = "uddg="
        if (marker !in absolute) return absolute.takeIf { it.startsWith("http") }
        val target = absolute.substringAfter(marker).substringBefore("&")
        return runCatching { URLDecoder.decode(target, "UTF-8") }.getOrNull()
    }

    private fun fetch(url: String): String? = runCatching {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (compatible; PNA-Crawler/1.0)")
            .build()
        http.newCall(request).execute().use { it.body?.string() }
    }.getOrNull()

    private fun String.normalizePhoneDigits(): String = filter(Char::isDigit)

    companion object {
        const val NO_MATCH_FOUND: String = "No match found"
    }
}
