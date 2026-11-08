package com.mcpserver.mcp.router

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mcpserver.platform.browser.BrowserEngine
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

/**
 * Interaction tools for the headless browser plus a bridge to the device's real
 * browser. Enough to script a check-in flow: open, fill, click, wait, verify.
 */
fun CommandRegistry.registerBrowserInteractionTools(browser: BrowserEngine, context: Context) {

    register(
        ToolDefinition(
            name = "browser_click",
            description = "Click inside the headless browser by CSS selector or by visible text.",
            inputSchema = bSchema(
                "selector" to bProp("string", "CSS selector, e.g. #submit or button.login"),
                "text" to bProp("string", "Click the first link/button whose text contains this"),
                "index" to bProp("integer", "Which match to click when using text (default 0)"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val selector = args.bStr("selector")
                val text = args.bStr("text")
                val index = args.bInt("index") ?: 0
                val script = when {
                    !selector.isNullOrBlank() -> """
                        (function(){
                          var list = document.querySelectorAll(${jsQuote(selector)});
                          if(!list || list.length === 0) return 'NOT_FOUND';
                          var el = list[${index}] || list[0];
                          el.scrollIntoView({block:'center'});
                          el.click();
                          return 'CLICKED:' + (el.tagName||'') + ':' + ((el.innerText||el.value||'')+'').slice(0,80);
                        })()
                    """.trimIndent()
                    !text.isNullOrBlank() -> """
                        (function(){
                          var want = ${jsQuote(text)};
                          var all = document.querySelectorAll('a,button,input[type=submit],input[type=button],[role=button],div[onclick]');
                          var hits = [];
                          for (var i=0;i<all.length;i++){
                            var label = (all[i].innerText || all[i].value || all[i].getAttribute('aria-label') || '')+'';
                            if (label.indexOf(want) >= 0) hits.push(all[i]);
                          }
                          if (hits.length === 0) return 'NOT_FOUND';
                          var el = hits[${index}] || hits[0];
                          el.scrollIntoView({block:'center'});
                          el.click();
                          return 'CLICKED:' + (el.tagName||'') + ':' + ((el.innerText||el.value||'')+'').slice(0,80);
                        })()
                    """.trimIndent()
                    else -> throw IllegalArgumentException("Provide 'selector' or 'text'")
                }
                val result = browser.evaluate(script)
                buildJsonObject {
                    put("success", result.startsWith("CLICKED"))
                    put("result", result)
                    put("url", runCatching { browser.currentUrl() }.getOrDefault(""))
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_type",
            description = "Fill an input inside the headless browser and fire input/change events.",
            inputSchema = bSchema(
                "selector" to bProp("string", "CSS selector of the input"),
                "text" to bProp("string", "Text to set"),
                "submit" to bProp("boolean", "Submit the closest form afterwards (default false)"),
                required = listOf("selector", "text"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val selector = args.bStr("selector") ?: throw IllegalArgumentException("Missing 'selector'")
                val text = args.bStr("text") ?: throw IllegalArgumentException("Missing 'text'")
                val submit = args.bBool("submit") == true
                val script = """
                    (function(){
                      var el = document.querySelector(${jsQuote(selector)});
                      if(!el) return 'NOT_FOUND';
                      el.focus();
                      el.value = ${jsQuote(text)};
                      el.dispatchEvent(new Event('input',{bubbles:true}));
                      el.dispatchEvent(new Event('change',{bubbles:true}));
                      ${if (submit) "if (el.form) { el.form.submit(); return 'SUBMITTED'; }" else ""}
                      return 'TYPED';
                    })()
                """.trimIndent()
                val result = browser.evaluate(script)
                buildJsonObject {
                    put("success", result == "TYPED" || result == "SUBMITTED")
                    put("result", result)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_tap",
            description = "Synthesize a real touch inside the headless browser viewport (page coordinates).",
            inputSchema = bSchema(
                "x" to bProp("integer", "X in the WebView viewport"),
                "y" to bProp("integer", "Y in the WebView viewport"),
                required = listOf("x", "y"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val x = (args.bInt("x") ?: throw IllegalArgumentException("Missing 'x'")).toFloat()
                val y = (args.bInt("y") ?: throw IllegalArgumentException("Missing 'y'")).toFloat()
                browser.tapAt(x, y)
                buildJsonObject {
                    put("success", true)
                    put("x", x.toInt())
                    put("y", y.toInt())
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_swipe",
            description = "Synthesize a real swipe gesture inside the headless browser viewport.",
            inputSchema = bSchema(
                "x1" to bProp("integer", "Start X"),
                "y1" to bProp("integer", "Start Y"),
                "x2" to bProp("integer", "End X"),
                "y2" to bProp("integer", "End Y"),
                "duration_ms" to bProp("integer", "Gesture duration (default 400)"),
                required = listOf("x1", "y1", "x2", "y2"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val x1 = (args.bInt("x1") ?: throw IllegalArgumentException("Missing 'x1'")).toFloat()
                val y1 = (args.bInt("y1") ?: throw IllegalArgumentException("Missing 'y1'")).toFloat()
                val x2 = (args.bInt("x2") ?: throw IllegalArgumentException("Missing 'x2'")).toFloat()
                val y2 = (args.bInt("y2") ?: throw IllegalArgumentException("Missing 'y2'")).toFloat()
                val duration = (args.bInt("duration_ms") ?: 400).coerceIn(50, 10_000)
                browser.swipe(x1, y1, x2, y2, duration)
                buildJsonObject {
                    put("success", true)
                    put("duration_ms", duration)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_scroll",
            description = "Scroll the headless browser by pixels, or jump to coordinates.",
            inputSchema = bSchema(
                "dx" to bProp("integer", "Horizontal delta"),
                "dy" to bProp("integer", "Vertical delta (positive scrolls down)"),
                "x" to bProp("integer", "Absolute scroll X"),
                "y" to bProp("integer", "Absolute scroll Y"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val x = args.bInt("x")
                val y = args.bInt("y")
                val dx = args.bInt("dx")
                val dy = args.bInt("dy")
                val script = if (x != null || y != null) {
                    "window.scrollTo(${x ?: 0}, ${y ?: 0}); 'OK:' + window.scrollY"
                } else {
                    "window.scrollBy(${dx ?: 0}, ${dy ?: 300}); 'OK:' + window.scrollY"
                }
                val result = browser.evaluate(script)
                buildJsonObject {
                    put("success", result.startsWith("OK"))
                    put("result", result)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_wait",
            description = "Wait for a CSS selector to appear, or simply wait a fixed time.",
            inputSchema = bSchema(
                "selector" to bProp("string", "CSS selector to wait for"),
                "timeout_ms" to bProp("integer", "Timeout in milliseconds (default 15000)"),
                "ms" to bProp("integer", "Plain delay when no selector is given"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val selector = args.bStr("selector")
                val timeout = (args.bInt("timeout_ms") ?: 15_000).coerceIn(100, 120_000)
                if (selector.isNullOrBlank()) {
                    val ms = (args.bInt("ms") ?: 1_000).coerceIn(0, 120_000)
                    browser.waitForSelector("", ms.toLong())
                    buildJsonObject {
                        put("success", true)
                        put("waited_ms", ms)
                    }
                } else {
                    val found = browser.waitForSelector(selector, timeout.toLong())
                    buildJsonObject {
                        put("success", found)
                        put("selector", selector)
                        put("timeout_ms", timeout)
                    }
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_query",
            description = "Inspect elements in the headless browser: text, attributes and count.",
            inputSchema = bSchema(
                "selector" to bProp("string", "CSS selector"),
                required = listOf("selector"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val selector = args.bStr("selector") ?: throw IllegalArgumentException("Missing 'selector'")
                val script = """
                    (function(){
                      var list = document.querySelectorAll(${jsQuote(selector)});
                      var out = [];
                      for (var i=0;i<list.length && i<50;i++){
                        var el = list[i];
                        out.push({
                          tag: el.tagName,
                          text: ((el.innerText||el.value||'')+'').slice(0,300),
                          href: el.getAttribute('href') || '',
                          id: el.id || '',
                          cls: el.className ? (el.className+'') : ''
                        });
                      }
                      return JSON.stringify({count:list.length, items:out});
                    })()
                """.trimIndent()
                val raw = browser.evaluate(script)
                buildJsonObject {
                    put("success", raw.isNotBlank() && raw != "null")
                    put("selector", selector)
                    put("data", raw)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_html",
            description = "Return the rendered DOM HTML of the loaded page.",
            inputSchema = bSchema(
                "max_chars" to bProp("integer", "Maximum characters (default 50000)"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val maxChars = (args.bInt("max_chars") ?: 50_000).coerceIn(500, 2_000_000)
                val html = browser.html(maxChars)
                buildJsonObject {
                    put("success", html.isNotBlank())
                    put("length", html.length)
                    put("html", html)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_cookies",
            description = "Read, set or clear cookies — needed for keeping a check-in session logged in.",
            inputSchema = bSchema(
                "action" to bProp("string", "get (default) | set | clear"),
                "url" to bProp("string", "Cookie URL scope (defaults to the current page)"),
                "cookie" to bProp("string", "Cookie string for action=set, e.g. a=1; b=2"),
                "value" to bProp("string", "Convenience: sets a single name=value pair"),
                "name" to bProp("string", "Cookie name used together with 'value'"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val action = (args.bStr("action") ?: "get").lowercase()
                val url = args.bStr("url") ?: runCatching { browser.currentUrl() }.getOrDefault("")
                when (action) {
                    "set" -> {
                        val cookie = args.bStr("cookie")
                            ?: runCatching {
                                val name = args.bStr("name") ?: throw IllegalArgumentException("Provide 'cookie' or 'name'")
                                "$name=${args.bStr("value").orEmpty()}"
                            }.getOrThrow()
                        browser.setCookie(url, cookie)
                        buildJsonObject {
                            put("success", true)
                            put("url", url)
                            put("cookie", cookie)
                        }
                    }
                    "clear" -> {
                        browser.clearCookies()
                        buildJsonObject {
                            put("success", true)
                            put("cleared", true)
                        }
                    }
                    else -> buildJsonObject {
                        put("success", true)
                        put("url", url)
                        put("cookie", browser.cookies(url))
                    }
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_user_agent",
            description = "Get or override the headless browser User-Agent (desktop/mobile switching).",
            inputSchema = bSchema(
                "ua" to bProp("string", "New User-Agent; omit to read the current one"),
                "preset" to bProp("string", "'mobile' | 'desktop' | 'android'"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val preset = args.bStr("preset")?.lowercase()
                val ua = args.bStr("ua") ?: when (preset) {
                    "desktop" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
                    "mobile" -> "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"
                    "android" -> "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                    null -> null
                    else -> null
                }
                if (ua != null) browser.setUserAgent(ua)
                buildJsonObject {
                    put("success", true)
                    put("changed", ua != null)
                    put("user_agent", browser.userAgent())
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_history",
            description = "Navigate the headless browser history or reload the page.",
            inputSchema = bSchema(
                "action" to bProp("string", "back | forward | reload"),
                required = listOf("action"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val action = (args.bStr("action") ?: "").lowercase()
                when (action) {
                    "back" -> browser.goBack()
                    "forward" -> browser.goForward()
                    "reload" -> browser.reload()
                    else -> throw IllegalArgumentException("action must be back | forward | reload")
                }
                buildJsonObject {
                    put("success", true)
                    put("action", action)
                    put("url", runCatching { browser.currentUrl() }.getOrDefault(""))
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_open_system",
            description = "Force the device's real browser (or the chooser) to open a URL.",
            inputSchema = bSchema(
                "url" to bProp("string", "URL to open"),
                "package_name" to bProp("string", "Optional target browser package"),
                required = listOf("url"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val url = args.bStr("url") ?: throw IllegalArgumentException("Missing 'url'")
                val pkg = args.bStr("package_name")
                val normalized = if (url.startsWith("http")) url else "https://$url"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(normalized)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    if (!pkg.isNullOrBlank()) setPackage(pkg)
                }
                val error = runCatching { context.startActivity(intent) }.exceptionOrNull()
                buildJsonObject {
                    put("success", error == null)
                    put("url", normalized)
                    put("package", pkg ?: "")
                    put("error", error?.message ?: "")
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_status",
            description = "Report whether the headless browser is open, where it is and its ready state.",
            inputSchema = bSchema(),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = {
                val open = browser.isOpen()
                buildJsonObject {
                    put("success", true)
                    put("open", open)
                    if (open) {
                        put("url", runCatching { browser.currentUrl() }.getOrDefault(""))
                        put("title", runCatching { browser.pageTitle() }.getOrDefault(""))
                        put("ready_state", browser.readyState())
                        put("user_agent", browser.userAgent())
                    }
                }
            },
        )
    )
}

private fun JsonObject?.bStr(name: String): String? = (this?.get(name) as? JsonPrimitive)?.contentOrNull

private fun JsonObject?.bInt(name: String): Int? = (this?.get(name) as? JsonPrimitive)?.intOrNull

private fun JsonObject?.bBool(name: String): Boolean? = (this?.get(name) as? JsonPrimitive)?.booleanOrNull

private fun bProp(type: String, description: String) = buildJsonObject {
    put("type", type)
    put("description", description)
}

private fun bSchema(vararg properties: Pair<String, JsonObject>, required: List<String> = emptyList()) = buildJsonObject {
    put("type", "object")
    put("properties", buildJsonObject { properties.forEach { (name, value) -> put(name, value) } })
    if (required.isNotEmpty()) {
        put("required", buildJsonArray { required.forEach { add(it) } })
    }
}

internal fun jsQuote(value: String): String = buildString {
    append('"')
    value.forEach { character ->
        when (character) {
            '\\' -> append("\\\\")
            '"' -> append("\\\"")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (character < ' ') append("\\u%04x".format(character.code)) else append(character)
        }
    }
    append('"')
}
