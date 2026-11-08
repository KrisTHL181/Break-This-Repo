package com.mcpserver.mcp.router

import android.content.Context
import com.mcpserver.platform.browser.BrowserEngine
import com.mcpserver.platform.shell.PrivilegedShell
import com.mcpserver.util.PublicFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Observation tools: what did the page actually do?
 *
 *  - browser_eval      : awaits promises and returns structured JSON
 *  - browser_wait      : waits on selector / condition / URL / navigation
 *  - browser_request   : performs a request with the page's session cookies and
 *                        returns the raw body, status and redirect chain
 *  - browser_network   : records fetch/XHR traffic triggered by the page
 *  - browser_cookies   : read/write/export/import the WebView cookie jar
 */
fun CommandRegistry.registerBrowserObservationTools(browser: BrowserEngine, context: Context) {

    // ── evaluation that understands await and objects ───────────────────────

    register(
        ToolDefinition(
            name = "browser_eval",
            description = "Run JavaScript in the page, awaiting promises, and return the value as structured JSON.",
            inputSchema = oSchema(
                "script" to oProp("string", "Expression or IIFE; may return a promise"),
                "timeout_ms" to oProp("integer", "How long to wait for the promise (default 20000)"),
            ),
            permissionLevel = PermissionLevel.ELEVATED,
            handler = { args ->
                val script = args.oStr("script") ?: throw IllegalArgumentException("Missing 'script'")
                val timeout = (args.oInt("timeout_ms") ?: 20_000).coerceIn(200, 120_000)
                val envelope = browser.evaluateAwait(script, timeout.toLong())
                val parsed = runCatching { JSONObject(envelope) }.getOrNull()
                val status = parsed?.optString("status") ?: "ERROR"
                buildJsonObject {
                    put("success", status == "OK")
                    put("status", status)
                    put("type", parsed?.optString("type") ?: "")
                    if (status == "ERROR") put("error", parsed?.optString("error") ?: envelope)
                    parsed?.optString("text")?.takeIf { it.isNotBlank() && it != "null" }?.let { put("text", it) }
                    parsed?.optString("json")?.let { json ->
                        if (json.isNotBlank() && json != "null") put("json", json)
                    }
                }
            },
        )
    )

    // ── waiting on anything ─────────────────────────────────────────────────

    register(
        ToolDefinition(
            name = "browser_wait",
            description = "Wait for a selector, a JS condition, a URL change, a URL fragment, page text or a plain delay.",
            inputSchema = oSchema(
                "selector" to oProp("string", "Wait until this CSS selector exists"),
                "condition" to oProp("string", "Wait until this JS expression is truthy"),
                "url_contains" to oProp("string", "Wait until location.href contains this"),
                "url_changed" to oProp("boolean", "Wait until the URL differs from the one at call time"),
                "text" to oProp("string", "Wait until the page text contains this"),
                "ready_state" to oProp("string", "Wait for this document.readyState, e.g. 'complete'"),
                "ms" to oProp("integer", "Plain delay when no condition is given"),
                "timeout_ms" to oProp("integer", "Timeout (default 20000)"),
                "poll_ms" to oProp("integer", "Polling interval (default 250)"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val selector = args.oStr("selector")
                val condition = args.oStr("condition")
                val urlContains = args.oStr("url_contains")
                val urlChanged = args.oBool("url_changed") == true
                val text = args.oStr("text")
                val readyState = args.oStr("ready_state")
                val timeout = (args.oInt("timeout_ms") ?: 20_000).coerceIn(100, 180_000)
                val poll = (args.oInt("poll_ms") ?: 250).coerceIn(50, 5_000).toLong()
                val hasCondition = listOf(selector, condition, urlContains, text, readyState).any { !it.isNullOrBlank() } || urlChanged

                val started = System.currentTimeMillis()
                if (!hasCondition) {
                    val ms = (args.oInt("ms") ?: 1_000).coerceIn(0, 180_000)
                    delay(ms.toLong())
                    return@ToolDefinition buildJsonObject {
                        put("success", true)
                        put("matched_on", "delay")
                        put("elapsed_ms", System.currentTimeMillis() - started)
                    }
                }

                val baselineUrl = runCatching { browser.evaluate("location.href", 5_000L) }.getOrDefault("")
                var matchedOn = ""
                var matched = false
                val deadline = System.currentTimeMillis() + timeout
                while (System.currentTimeMillis() < deadline) {
                    val checks = buildList {
                        if (!selector.isNullOrBlank()) {
                            add("selector" to "(function(){ try { return !!document.querySelector(${jsQuote(selector)}); } catch(e){ return false; } })()")
                        }
                        if (!condition.isNullOrBlank()) {
                            add("condition" to "(function(){ try { return !!($condition); } catch(e){ return false; } })()")
                        }
                        if (!urlContains.isNullOrBlank()) {
                            add("url_contains" to "(function(){ return (location.href||'').indexOf(${jsQuote(urlContains)}) >= 0; })()")
                        }
                        if (urlChanged) {
                            add("url_changed" to "(function(){ return (location.href||'') !== ${jsQuote(baselineUrl)}; })()")
                        }
                        if (!text.isNullOrBlank()) {
                            add("text" to "(function(){ var b=document.body?document.body.innerText:''; return b.indexOf(${jsQuote(text)}) >= 0; })()")
                        }
                        if (!readyState.isNullOrBlank()) {
                            add("ready_state" to "(function(){ return document.readyState === ${jsQuote(readyState)}; })()")
                        }
                    }
                    for ((name, expression) in checks) {
                        val result = runCatching { browser.evaluate(expression, 5_000L) }.getOrDefault("false")
                        if (result.contains("true")) {
                            matched = true
                            matchedOn = name
                            break
                        }
                    }
                    if (matched) break
                    delay(poll)
                }

                buildJsonObject {
                    put("success", matched)
                    put("matched_on", matchedOn)
                    put("elapsed_ms", System.currentTimeMillis() - started)
                    put("url", runCatching { browser.evaluate("location.href", 5_000L) }.getOrDefault(""))
                    put("ready_state", runCatching { browser.evaluate("document.readyState", 5_000L) }.getOrDefault(""))
                }
            },
        )
    )

    // ── requests with the page session ──────────────────────────────────────

    register(
        ToolDefinition(
            name = "browser_request",
            description = "HTTP request from inside the page (carries its cookies): raw body, status, headers, redirect chain.",
            inputSchema = oSchema(
                "url" to oProp("string", "Request URL (relative URLs resolve against the page)"),
                "method" to oProp("string", "GET/POST/... (default GET)"),
                "headers" to oProp("object", "Extra request headers"),
                "body" to oProp("string", "Request body for non-GET methods"),
                "credentials" to oProp("string", "include (default) | omit | same-origin"),
                "follow_manual" to oProp("boolean", "Follow redirects hop by hop and report the chain"),
                "timeout_ms" to oProp("integer", "Timeout (default 30000)"),
                "max_chars" to oProp("integer", "Body length cap (default 200000)"),
                required = listOf("url"),
            ),
            permissionLevel = PermissionLevel.ELEVATED,
            handler = { args ->
                val url = args.oStr("url") ?: throw IllegalArgumentException("Missing 'url'")
                val method = (args.oStr("method") ?: "GET").uppercase()
                val body = args.oStr("body")
                val credentials = args.oStr("credentials") ?: "include"
                val manual = args.oBool("follow_manual") == true
                val timeout = (args.oInt("timeout_ms") ?: 30_000).coerceIn(500, 180_000)
                val maxChars = (args.oInt("max_chars") ?: 200_000).coerceIn(200, 4_000_000)

                val headersJs = (args?.get("headers") as? JsonObject)
                    ?.entries
                    ?.joinToString(prefix = "{", postfix = "}") { (key, value) ->
                        "${jsQuote(key)}:${jsQuote((value as? JsonPrimitive)?.contentOrNull.orEmpty())}"
                    } ?: "{}"

                val script = """
                    (async function(){
                      var opts = {method:${jsQuote(method)}, credentials:${jsQuote(credentials)}, headers:$headersJs};
                      ${if (body != null) "opts.body = ${jsQuote(body)};" else ""}
                      var chain = [];
                      var resp = null;
                      if ($manual) {
                        var current = new URL(${jsQuote(url)}, location.href).href;
                        for (var hop = 0; hop < 8; hop++) {
                          opts.redirect = 'manual';
                          resp = await fetch(current, opts);
                          var loc = '';
                          try { loc = resp.headers.get('location') || ''; } catch(e) { loc = ''; }
                          chain.push({url: current, status: resp.status, location: loc});
                          if (resp.status >= 300 && resp.status < 400 && loc) {
                            current = new URL(loc, current).href;
                            opts.method = 'GET';
                            delete opts.body;
                            continue;
                          }
                          break;
                        }
                      } else {
                        opts.redirect = 'follow';
                        resp = await fetch(new URL(${jsQuote(url)}, location.href).href, opts);
                        chain.push({url: resp.url, status: resp.status, location: ''});
                      }
                      var headers = {};
                      try { resp.headers.forEach(function(v,k){ headers[k]=v; }); } catch(e) {}
                      var text = '';
                      try { text = await resp.text(); } catch(e) { text = 'ERROR reading body: ' + e; }
                      return {status: resp.status, statusText: resp.statusText || '', final_url: resp.url || '',
                              redirected: !!resp.redirected, headers: headers, body: text, chain: chain};
                    })()
                """.trimIndent()

                val envelope = browser.evaluateAwait(script, timeout.toLong())
                val parsedEnvelope = runCatching { JSONObject(envelope) }.getOrNull()
                val payload = parsedEnvelope?.optString("json")?.takeIf { it.isNotBlank() && it != "null" }
                    ?.let { runCatching { JSONObject(it) }.getOrNull() }

                buildJsonObject {
                    put("success", payload != null && parsedEnvelope?.optString("status") == "OK")
                    put("eval_status", parsedEnvelope?.optString("status") ?: "ERROR")
                    if (payload == null) {
                        put("error", parsedEnvelope?.optString("error") ?: envelope.take(500))
                    } else {
                        put("status", payload.optInt("status"))
                        put("status_text", payload.optString("statusText"))
                        put("final_url", payload.optString("final_url"))
                        put("redirected", payload.optBoolean("redirected"))
                        put("content_type", payload.optJSONObject("headers")?.optString("content-type") ?: "")
                        put(
                            "headers",
                            buildJsonObject {
                                payload.optJSONObject("headers")?.keys()?.forEach { key ->
                                    put(key, payload.optJSONObject("headers")?.optString(key) ?: "")
                                }
                            },
                        )
                        val rawBody = payload.optString("body")
                        put("body_length", rawBody.length)
                        put("truncated", rawBody.length > maxChars)
                        put("body", rawBody.take(maxChars))
                        put(
                            "chain",
                            buildJsonArray {
                                val chain = payload.optJSONArray("chain")
                                if (chain != null) {
                                    for (i in 0 until chain.length()) {
                                        val hop = chain.optJSONObject(i) ?: continue
                                        add(
                                            buildJsonObject {
                                                put("url", hop.optString("url"))
                                                put("status", hop.optInt("status"))
                                                put("location", hop.optString("location"))
                                            }
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            },
        )
    )

    // ── network traffic recorder ────────────────────────────────────────────

    register(
        ToolDefinition(
            name = "browser_network",
            description = "Record the page's own fetch/XHR traffic: start, log, clear or stop.",
            inputSchema = oSchema(
                "action" to oProp("string", "start | log (default) | clear | stop | status"),
                "limit" to oProp("integer", "Maximum entries returned (default 100)"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                when ((args.oStr("action") ?: "log").lowercase()) {
                    "start" -> {
                        val result = browser.evaluate(NETWORK_PATCH_SCRIPT, 10_000L)
                        buildJsonObject {
                            put("success", result.contains("OK") || result.contains("ALREADY"))
                            put("installed", true)
                            put("detail", result)
                        }
                    }
                    "stop" -> {
                        val result = browser.evaluate(NETWORK_UNPATCH_SCRIPT, 10_000L)
                        buildJsonObject {
                            put("success", true)
                            put("installed", false)
                            put("detail", result)
                        }
                    }
                    "clear" -> {
                        browser.evaluate("(function(){ window.__mcpNet=[]; return 'OK'; })()", 8_000L)
                        buildJsonObject {
                            put("success", true)
                            put("cleared", true)
                        }
                    }
                    "status" -> {
                        val result = browser.evaluate(
                            "(function(){ return JSON.stringify({installed:!!window.__mcpNetInstalled, count:(window.__mcpNet||[]).length}); })()",
                            8_000L,
                        )
                        buildJsonObject {
                            put("success", true)
                            put("data", result)
                        }
                    }
                    else -> {
                        val limit = (args.oInt("limit") ?: 100).coerceIn(1, 500)
                        val raw = browser.evaluate(
                            "(function(){ var l=(window.__mcpNet||[]); return JSON.stringify({installed:!!window.__mcpNetInstalled, count:l.length, entries:l.slice(-$limit)}); })()",
                            10_000L,
                        )
                        buildJsonObject {
                            put("success", raw.startsWith("{"))
                            put("data", raw)
                        }
                    }
                }
            },
        )
    )

    // ── cookie jar bridging ─────────────────────────────────────────────────

    register(
        ToolDefinition(
            name = "browser_cookies",
            description = "Read, write, clear, export or import the WebView cookie jar (export gives a curl-ready header).",
            inputSchema = oSchema(
                "action" to oProp("string", "get (default) | set | clear | export | import | header"),
                "url" to oProp("string", "Cookie scope; defaults to the current page"),
                "cookie" to oProp("string", "Cookie string for action=set, e.g. 'a=1; b=2'"),
                "name" to oProp("string", "Single cookie name (with value)"),
                "value" to oProp("string", "Single cookie value"),
                "path" to oProp("string", "File path for export/import (Download folder by default)"),
                "format" to oProp("string", "json (default) | netscape | header"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val action = (args.oStr("action") ?: "get").lowercase()
                val currentUrl = runCatching { browser.currentUrl() }.getOrDefault("")
                val url = args.oStr("url") ?: currentUrl
                val scope = url.ifBlank { "https://localhost" }

                when (action) {
                    "set" -> {
                        val cookie = args.oStr("cookie") ?: run {
                            val name = args.oStr("name") ?: throw IllegalArgumentException("Provide 'cookie' or 'name'")
                            "$name=${args.oStr("value").orEmpty()}"
                        }
                        browser.setCookie(scope, cookie)
                        buildJsonObject {
                            put("success", true)
                            put("url", scope)
                            put("cookie", cookie)
                            put("jar", browser.cookies(scope))
                        }
                    }
                    "clear" -> {
                        browser.clearCookies()
                        buildJsonObject {
                            put("success", true)
                            put("cleared", true)
                        }
                    }
                    "header" -> {
                        val jar = browser.cookies(scope)
                        buildJsonObject {
                            put("success", true)
                            put("url", scope)
                            put("cookie_header", jar)
                            put("curl_hint", "curl -H ${jsQuote("Cookie: $jar")} '<url>'")
                        }
                    }
                    "export" -> {
                        val jar = browser.cookies(scope)
                        val format = (args.oStr("format") ?: "json").lowercase()
                        val pairs = jar.split(';').mapNotNull { part ->
                            val trimmed = part.trim()
                            if (trimmed.isEmpty() || !trimmed.contains('=')) null
                            else trimmed.substringBefore('=') to trimmed.substringAfter('=')
                        }
                        val host = runCatching { java.net.URI(scope).host }.getOrNull() ?: "localhost"
                        val content = when (format) {
                            "netscape" -> buildString {
                                appendLine("# Netscape HTTP Cookie File")
                                pairs.forEach { (name, value) ->
                                    appendLine("$host\tTRUE\t/\tFALSE\t0\t$name\t$value")
                                }
                            }
                            "header" -> jar
                            else -> buildString {
                                appendLine("[")
                                pairs.forEachIndexed { index, (name, value) ->
                                    append("  {\"name\":${jsQuote(name)},\"value\":${jsQuote(value)}}")
                                    if (index != pairs.lastIndex) append(',')
                                    appendLine()
                                }
                                appendLine("]")
                            }
                        }
                        val requestedPath = args.oStr("path")
                        val defaultName = "mcp_cookies_${oTimestamp()}." + when (format) {
                            "netscape" -> "txt"
                            "header" -> "txt"
                            else -> "json"
                        }
                        val saved = if (!requestedPath.isNullOrBlank() && File(requestedPath).parentFile?.canWrite() == true) {
                            runCatching {
                                File(requestedPath).writeText(content)
                                requestedPath
                            }.getOrNull()
                        } else {
                            PublicFiles.saveToDownloads(
                                context,
                                requestedPath?.let { File(it).name } ?: defaultName,
                                "text/plain",
                                content.toByteArray(Charsets.UTF_8),
                            )
                        }
                        buildJsonObject {
                            put("success", saved != null)
                            put("path", saved ?: "")
                            put("format", format)
                            put("count", pairs.size)
                            put("cookie_header", jar)
                            put("content_preview", content.take(1_500))
                        }
                    }
                    "import" -> {
                        val path = args.oStr("path") ?: throw IllegalArgumentException("Missing 'path'")
                        val text = withContext(Dispatchers.IO) {
                            val direct = runCatching { File(path).readText() }.getOrNull()
                            direct ?: PrivilegedShell.exec("cat ${jsQuoteShell(path)}", 20_000L).stdout
                        }
                        val entries = mutableListOf<Pair<String, String>>()
                        runCatching {
                            if (text.trimStart().startsWith("[")) {
                                val array = org.json.JSONArray(text)
                                for (i in 0 until array.length()) {
                                    val item = array.optJSONObject(i) ?: continue
                                    entries.add(item.optString("name") to item.optString("value"))
                                }
                            } else {
                                text.lineSequence().forEach { line ->
                                    val trimmed = line.trim()
                                    if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
                                    val parts = trimmed.split('\t')
                                    if (parts.size >= 7) entries.add(parts[5] to parts[6])
                                    else if (trimmed.contains('=')) entries.add(trimmed.substringBefore('=') to trimmed.substringAfter('='))
                                }
                            }
                        }
                        entries.forEach { (name, value) -> browser.setCookie(scope, "$name=$value") }
                        buildJsonObject {
                            put("success", entries.isNotEmpty())
                            put("imported", entries.size)
                            put("url", scope)
                            put("jar", browser.cookies(scope))
                        }
                    }
                    else -> buildJsonObject {
                        put("success", true)
                        put("url", scope)
                        put("cookie", browser.cookies(scope))
                        put("count", browser.cookies(scope).split(';').count { it.contains('=') })
                    }
                }
            },
        )
    )
}

// ── scripts ───────────────────────────────────────────────────────────────────

private const val NETWORK_PATCH_SCRIPT = """
(function(){
  if (window.__mcpNetInstalled) return 'ALREADY';
  window.__mcpNet = [];
  var push = function(entry){
    try { window.__mcpNet.push(entry); if (window.__mcpNet.length > 500) window.__mcpNet.shift(); } catch(e) {}
  };
  window.__mcpNetPush = push;
  var origFetch = window.fetch;
  if (origFetch) {
    window.fetch = function(input, init){
      var url = (typeof input === 'string') ? input : (input && input.url ? input.url : String(input));
      var method = (init && init.method) || (input && input.method) || 'GET';
      var t0 = Date.now();
      return origFetch.apply(this, arguments).then(function(r){
        push({kind:'fetch', method:method, url:url, status:r.status, ok:r.ok, ms:Date.now()-t0});
        return r;
      }).catch(function(e){
        push({kind:'fetch', method:method, url:url, status:0, error:''+e, ms:Date.now()-t0});
        throw e;
      });
    };
  }
  var origOpen = XMLHttpRequest.prototype.open;
  var origSend = XMLHttpRequest.prototype.send;
  XMLHttpRequest.prototype.open = function(m, u){
    this.__mcpMethod = m; this.__mcpUrl = u;
    return origOpen.apply(this, arguments);
  };
  XMLHttpRequest.prototype.send = function(){
    var self = this; var t0 = Date.now();
    this.addEventListener('loadend', function(){
      push({kind:'xhr', method:self.__mcpMethod||'', url:self.__mcpUrl||'', status:self.status, ms:Date.now()-t0});
    });
    return origSend.apply(this, arguments);
  };
  window.__mcpNetInstalled = true;
  return 'OK';
})()
"""

private const val NETWORK_UNPATCH_SCRIPT = """
(function(){
  window.__mcpNetInstalled = false;
  return 'OK';
})()
"""

// ── helpers ───────────────────────────────────────────────────────────────────

private fun oTimestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

private fun jsQuoteShell(value: String): String = "'" + value.replace("'", "'\\''") + "'"

private fun JsonObject?.oStr(name: String): String? = (this?.get(name) as? JsonPrimitive)?.contentOrNull

private fun JsonObject?.oInt(name: String): Int? = (this?.get(name) as? JsonPrimitive)?.intOrNull

private fun JsonObject?.oBool(name: String): Boolean? =
    (this?.get(name) as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull()

private fun oProp(type: String, description: String) = buildJsonObject {
    put("type", type)
    put("description", description)
}

private fun oSchema(vararg properties: Pair<String, JsonObject>, required: List<String> = emptyList()) = buildJsonObject {
    put("type", "object")
    put("properties", buildJsonObject { properties.forEach { (name, value) -> put(name, value) } })
    if (required.isNotEmpty()) {
        put("required", buildJsonArray { required.forEach { add(it) } })
    }
}
