package com.pna.backend.crawler

import org.jsoup.Jsoup
import java.net.URLDecoder
import java.net.URLEncoder

data class SearchResult(
    val title: String,
    val url: String
)

fun main() {
    val query = "6173001"
    val keywords = listOf(
        "telefon", "telephone", "phone", "tel", "kontakt", "caller", "telefoninumber"
    )

    val results = searchDuckDuckGo(query, "ee-et")
    println("Found ${results.size} search results")
    val filtered = results.filter { pageLooksRelevant(it.url, keywords, query, 8) }
    println("Filtered to ${filtered.size} relevant results")

    filtered.forEachIndexed { index, result ->
        println("${index + 1}. ${result.title}")
        println("   ${result.url}")
    }
}

fun searchDuckDuckGo(query: String, region: String): List<SearchResult> {
    val encodedQuery = URLEncoder.encode(query, Charsets.UTF_8)
    val searchUrl = "https://html.duckduckgo.com/html/?q=$encodedQuery&kl=$region"

    val doc = Jsoup.connect(searchUrl)
        .userAgent("Mozilla/5.0")
        .referrer("https://duckduckgo.com/")
        .timeout(10_000)
        .get()

    return doc.select("a.result__a")
        .map { element ->
            SearchResult(
                title = element.text(),
                url = unwrapDuckDuckGoUrl(element.attr("href"))
            )
        }
        .distinctBy { it.url }
}

fun pageLooksRelevant(
    url: String,
    keywords: List<String>,
    query: String,
    maxDistance: Int
): Boolean {
    return try {
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0")
            .referrer("https://duckduckgo.com/")
            .timeout(10_000)
            .followRedirects(true)
            .get()

        val text = doc.body().text()
        hasKeywordNearNumber(text, keywords, query, maxDistance)
    } catch (_: Exception) {
        false
    }
}

fun hasKeywordNearNumber(
    text: String,
    keywords: List<String>,
    query: String,
    maxDistance: Int
): Boolean {
    val escapedKeywords = keywords.joinToString("|") { "${Regex.escape(it)}\\p{L}*" }
    val digitPattern = query
        .filter { it.isDigit() }
        .map { Regex.escape(it.toString()) }
        .joinToString("""[\s\-()]*""")

    val regex = Regex(
        """(?:\b(?:$escapedKeywords)\b[\s\p{Punct}]{0,$maxDistance}$digitPattern|$digitPattern[\s\p{Punct}]{0,$maxDistance}\b(?:$escapedKeywords)\b)""",
        setOf(RegexOption.IGNORE_CASE)
    )

    return regex.containsMatchIn(text)
}

fun unwrapDuckDuckGoUrl(href: String): String {
    val absolute = if (href.startsWith("//")) "https:$href" else href
    val marker = "uddg="
    val start = absolute.indexOf(marker)

    if (start == -1) return absolute

    val valueStart = start + marker.length
    val valueEnd = absolute.indexOf("&", valueStart)
        .let { if (it == -1) absolute.length else it }

    val encodedTarget = absolute.substring(valueStart, valueEnd)
    return URLDecoder.decode(encodedTarget, Charsets.UTF_8)
}
