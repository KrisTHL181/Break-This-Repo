package com.mcpserver.platform.web

import android.util.Log
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.zip.GZIPInputStream

/**
 * Plain-HTTP web access: fetch a page and turn it into readable text, or run a
 * keyword search. No WebView involved, so it works from any background thread.
 */
object WebToolkit {

    private const val TAG = "WebToolkit"
    private const val UA =
        "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/126.0.0.0 Mobile Safari/537.36 McpAndroidServer/1.1"

    data class FetchResult(
        val url: String,
        val status: Int,
        val contentType: String?,
        val title: String?,
        val text: String,
        val truncated: Boolean,
    )

    data class SearchHit(val title: String, val url: String, val snippet: String)

    // -- Fetch ---------------------------------------------------------------

    fun fetch(url: String, maxChars: Int = 20_000, timeoutMs: Int = 20_000): FetchResult {
        val response = get(url, timeoutMs)
        return try {
            val contentType = response.connection.contentType
            val body = String(response.body, charsetOf(contentType))
            val isHtml = contentType?.contains("html", ignoreCase = true) == true
            val text = if (isHtml) htmlToText(body) else body
            val truncated = text.length > maxChars
            FetchResult(
                url = response.connection.url.toString(),
                status = response.connection.responseCode,
                contentType = contentType,
                title = if (isHtml) titleOf(body) else null,
                text = if (truncated) text.take(maxChars) else text,
                truncated = truncated,
            )
        } finally {
            runCatching { response.connection.disconnect() }
        }
    }

    /** Raw body without HTML processing (used by the search parser). */
    fun fetchRaw(url: String, timeoutMs: Int = 20_000, maxBytes: Int = 1_000_000): String {
        val response = get(url, timeoutMs)
        return try {
            String(
                response.body.copyOf(minOf(response.body.size, maxBytes)),
                charsetOf(response.connection.contentType)
            )
        } finally {
            runCatching { response.connection.disconnect() }
        }
    }

    // -- Search --------------------------------------------------------------

    /**
     * Search engines are tried in order because availability is region
     * dependent: DuckDuckGo is unreachable on some networks while Bing works.
     */
    fun search(query: String, limit: Int = 8, timeoutMs: Int = 20_000): List<SearchHit> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val attempts: List<Pair<String, (String) -> List<SearchHit>>> = listOf(
            "https://cn.bing.com/search?q=$encoded&ensearch=0" to ::parseBing,
            "https://www.bing.com/search?q=$encoded" to ::parseBing,
            "https://html.duckduckgo.com/html/?q=$encoded" to { html -> parseDuckDuckGo(html).ifEmpty { parseGenericAnchors(html) } },
            "https://lite.duckduckgo.com/lite/?q=$encoded" to { html -> parseDuckDuckGo(html).ifEmpty { parseGenericAnchors(html) } },
        )
        for ((endpoint, parser) in attempts) {
            val hits = runCatching { parser(fetchRaw(endpoint, timeoutMs)) }
                .onFailure { Log.w(TAG, "search failed for $endpoint", it) }
                .getOrDefault(emptyList())
            if (hits.isNotEmpty()) return hits.take(limit)
        }
        return emptyList()
    }

    private val bingBlock = Regex(
        "<li class=\"b_algo\".*?(?=<li class=\"b_algo\"|</ol>)",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val bingTitle = Regex("<h2[^>]*>(.*?)</h2>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val bingHref = Regex("href=\"(https?://[^\"]+)\"", RegexOption.IGNORE_CASE)
    private val bingSnippet = Regex(
        "<p[^>]*class=\"[^\"]*b_lineclamp[^\"]*\"[^>]*>(.*?)</p>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    /** Bing result pages are a list of <li class="b_algo"> blocks. */
    private fun parseBing(html: String): List<SearchHit> {
        if (!html.contains("b_algo")) return emptyList()
        return bingBlock.findAll(html).mapNotNull { block ->
            val chunk = block.value
            val href = bingHref.find(chunk)?.groupValues?.get(1) ?: return@mapNotNull null
            val title = bingTitle.find(chunk)?.groupValues?.get(1)?.let { htmlToText(it) }.orEmpty()
            val snippet = bingSnippet.find(chunk)?.groupValues?.get(1)?.let { htmlToText(it) }.orEmpty()
            if (href.contains("bing.com") || href.contains("microsoft.com/")) return@mapNotNull null
            SearchHit(title.ifBlank { href }, decodeEntities(href), snippet.take(300))
        }.distinctBy { it.url }.toList()
    }

    private val ddgResult = Regex(
        "<a[^>]+class=\"[^\"]*result__a[^\"]*\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val ddgSnippet = Regex(
        "<a[^>]+class=\"[^\"]*result__snippet[^\"]*\"[^>]*>(.*?)</a>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val genericAnchor = Regex(
        "<a[^>]+href=\"(https?://[^\"]+)\"[^>]*>(.*?)</a>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    private fun parseDuckDuckGo(html: String): List<SearchHit> {
        val snippets = ddgSnippet.findAll(html).map { htmlToText(it.groupValues[1]) }.toList()
        return ddgResult.findAll(html).mapIndexed { index, match ->
            SearchHit(
                title = htmlToText(match.groupValues[2]).ifBlank { match.groupValues[1] },
                url = unwrapDuckDuckGoUrl(decodeEntities(match.groupValues[1])),
                snippet = snippets.getOrElse(index) { "" }.take(300),
            )
        }.filter { it.url.startsWith("http") }.toList()
    }

    private fun parseGenericAnchors(html: String): List<SearchHit> =
        genericAnchor.findAll(html).mapNotNull { match ->
            val url = decodeEntities(match.groupValues[1])
            val title = htmlToText(match.groupValues[2])
            if (title.length < 3 || url.contains("duckduckgo.com")) null else SearchHit(title, url, "")
        }.distinctBy { it.url }.toList()

    private fun unwrapDuckDuckGoUrl(href: String): String {
        val normalized = if (href.startsWith("//")) "https:$href" else href
        val marker = "uddg="
        val index = normalized.indexOf(marker)
        if (index < 0) return normalized
        val raw = normalized.substring(index + marker.length).substringBefore('&')
        return runCatching { URLDecoder.decode(raw, "UTF-8") }.getOrDefault(normalized)
    }

    // -- HTTP plumbing -------------------------------------------------------

    private class Response(val connection: HttpURLConnection, val body: ByteArray)

    private fun get(url: String, timeoutMs: Int): Response {
        var current = url
        var redirects = 0
        while (true) {
            val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("User-Agent", UA)
                setRequestProperty(
                    "Accept",
                    "text/html,application/xhtml+xml,application/json;q=0.9,text/plain;q=0.8,*/*;q=0.5"
                )
                setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                setRequestProperty("Accept-Encoding", "gzip")
            }
            val code = connection.responseCode
            if (code in 300..399) {
                val location = connection.getHeaderField("Location")
                if (location.isNullOrBlank()) return Response(connection, readBody(connection))
                connection.disconnect()
                redirects++
                if (redirects > 5) throw java.io.IOException("Too many redirects for $url")
                current = if (location.startsWith("http")) location else URL(URL(current), location).toString()
                continue
            }
            return Response(connection, readBody(connection))
        }
    }

    private fun readBody(connection: HttpURLConnection): ByteArray {
        val stream = connection.errorStream ?: connection.inputStream
        val decoded: InputStream =
            if (connection.contentEncoding?.contains("gzip", ignoreCase = true) == true) {
                GZIPInputStream(stream)
            } else {
                stream
            }
        return decoded.use { input ->
            val buffer = ByteArrayOutputStream()
            val chunk = ByteArray(16 * 1024)
            var total = 0
            while (true) {
                val read = input.read(chunk)
                if (read <= 0) break
                total += read
                if (total > 4 * 1024 * 1024) break
                buffer.write(chunk, 0, read)
            }
            buffer.toByteArray()
        }
    }

    private fun charsetOf(contentType: String?): java.nio.charset.Charset {
        val match = contentType?.let { Regex("charset=([\\w\\-]+)", RegexOption.IGNORE_CASE).find(it) }
        val name = match?.groupValues?.get(1) ?: return Charsets.UTF_8
        return runCatching { java.nio.charset.Charset.forName(name) }.getOrDefault(Charsets.UTF_8)
    }

    // -- HTML -> text --------------------------------------------------------

    fun htmlToText(html: String): String {
        var text = html
        text = Regex("<script[^>]*>.*?</script>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).replace(text, " ")
        text = Regex("<style[^>]*>.*?</style>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).replace(text, " ")
        text = Regex("<noscript[^>]*>.*?</noscript>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)).replace(text, " ")
        text = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL).replace(text, " ")
        text = Regex("<(br|/p|/div|/li|/h[1-6]|/tr|/section|/article)[^>]*>", RegexOption.IGNORE_CASE).replace(text, "\n")
        text = Regex("<[^>]+>").replace(text, " ")
        text = decodeEntities(text)
        text = text.replace(Regex("[ \\t\\x0B\\f\\r]+"), " ")
        text = text.replace(Regex(" *\\n *"), "\n")
        text = text.replace(Regex("\\n{3,}"), "\n\n")
        return tidySpacing(text).trim()
    }

    private val cjkClass = "[\\u3400-\\u4dbf\\u4e00-\\u9fff\\uf900-\\ufaff\\u3000-\\u303f\\uff01-\\uff60]"

    /**
     * HTML tags turn into spaces, which is wrong for CJK: "测 速 网" must become
     * "测速网". Spaces between CJK characters and around CJK punctuation are
     * removed, ASCII spacing is left untouched.
     */
    fun tidySpacing(input: String): String {
        var text = input
        val betweenCjk = Regex("($cjkClass) +($cjkClass)")
        val beforePunctuation = Regex("($cjkClass) +([,.;:!?)\\]])")
        val afterOpen = Regex("([(\\[]) +($cjkClass)")
        repeat(3) {
            text = betweenCjk.replace(text) { "${it.groupValues[1]}${it.groupValues[2]}" }
            text = beforePunctuation.replace(text) { "${it.groupValues[1]}${it.groupValues[2]}" }
            text = afterOpen.replace(text) { "${it.groupValues[1]}${it.groupValues[2]}" }
        }
        return text
    }

    fun titleOf(html: String): String? =
        Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.get(1)?.let { htmlToText(it) }?.takeIf { it.isNotBlank() }

    fun decodeEntities(input: String): String {
        var text = input
        text = text.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
            .replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
            .replace("&#39;", "'").replace("&#x27;", "'").replace("&hellip;", "...").replace("&mdash;", "--")
        text = Regex("&#(\\d+);").replace(text) { match ->
            match.groupValues[1].toIntOrNull()?.let { code -> String(Character.toChars(code)) } ?: match.value
        }
        text = Regex("&#x([0-9a-fA-F]+);").replace(text) { match ->
            match.groupValues[1].toIntOrNull(16)?.let { code -> String(Character.toChars(code)) } ?: match.value
        }
        return text
    }
}
