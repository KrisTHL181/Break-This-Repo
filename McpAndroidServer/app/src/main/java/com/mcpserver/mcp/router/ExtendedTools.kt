package com.mcpserver.mcp.router

import android.content.Context
import android.util.Log
import com.mcpserver.platform.browser.BrowserEngine
import com.mcpserver.platform.shizuku.ShizukuManager
import com.mcpserver.platform.shell.PrivilegedShell
import com.mcpserver.platform.web.WebToolkit
import com.mcpserver.util.PublicFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "ExtendedTools"

/**
 * Adds the web / headless-browser / shell tools on top of the built-ins and
 * replaces the stubbed shell_command with a real implementation.
 */
fun CommandRegistry.registerExtendedTools(context: Context, shizukuManager: ShizukuManager) {
    val appContext = context.applicationContext
    val browser = BrowserEngine(appContext)

    registerShellCommand(shizukuManager)
    registerWebTools()
    registerBrowserTools(browser, appContext)
    registerBrowserInteractionTools(browser, appContext)
    registerBrowserExtractTools(browser)
    registerBrowserAdvancedTools(browser, appContext)
    registerBrowserViewportTools(browser, appContext)
    registerBrowserObservationTools(browser, appContext)
    registerShizukuTools(shizukuManager)
    registerSaveTextFile(appContext)

    Log.i(TAG, "Extended tools registered (total=${toolCount})")
}

// -- shell --------------------------------------------------------------------

private fun CommandRegistry.registerShellCommand(shizukuManager: ShizukuManager) {
    register(
        ToolDefinition(
            name = "shell_command",
            description = "Execute a shell command on the device. Uses Shizuku (shell user) when available, otherwise this app's own process.",
            inputSchema = schema(
                "command" to prop("string", "Shell command to execute"),
                "timeout_ms" to prop("integer", "Timeout in milliseconds (default 30000, max 300000)"),
                required = listOf("command"),
            ),
            permissionLevel = PermissionLevel.ELEVATED,
            handler = { args ->
                val command = args.string("command") ?: throw IllegalArgumentException("Missing 'command' parameter")
                val timeout = (args.int("timeout_ms") ?: 30_000).toLong().coerceIn(1_000L, 300_000L)
                val result = PrivilegedShell.exec(command, timeout)
                buildJsonObject {
                    put("success", result.exitCode == 0)
                    put("executor", result.executor)
                    put("exit_code", result.exitCode)
                    put("duration_ms", result.durationMs)
                    put("stdout", result.stdout.take(200_000))
                    put("stderr", result.stderr.take(50_000))
                    put("shizuku", shizukuManager.refresh().toString())
                    put("output", result.stdout.ifBlank { result.stderr })
                }
            },
        )
    )
}

// -- web ----------------------------------------------------------------------

private fun CommandRegistry.registerWebTools() {
    register(
        ToolDefinition(
            name = "web_fetch",
            description = "Fetch a URL over HTTP(S) and return its readable text (HTML is converted to plain text).",
            inputSchema = schema(
                "url" to prop("string", "Absolute URL, e.g. https://example.com"),
                "max_chars" to prop("integer", "Maximum characters returned (default 20000)"),
                required = listOf("url"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val url = args.string("url") ?: throw IllegalArgumentException("Missing 'url' parameter")
                val maxChars = (args.int("max_chars") ?: 20_000).coerceIn(500, 500_000)
                withContext(Dispatchers.IO) {
                    val result = WebToolkit.fetch(url, maxChars)
                    buildJsonObject {
                        put("success", result.status in 200..299)
                        put("url", result.url)
                        put("status", result.status)
                        put("content_type", result.contentType ?: "")
                        put("title", result.title ?: "")
                        put("truncated", result.truncated)
                        put("text", result.text)
                    }
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "web_search",
            description = "Search the web (DuckDuckGo) and return titles, URLs and snippets.",
            inputSchema = schema(
                "query" to prop("string", "Search keywords"),
                "limit" to prop("integer", "Maximum results (default 8)"),
                required = listOf("query"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val query = args.string("query") ?: throw IllegalArgumentException("Missing 'query' parameter")
                val limit = (args.int("limit") ?: 8).coerceIn(1, 25)
                withContext(Dispatchers.IO) {
                    val hits = WebToolkit.search(query, limit)
                    buildJsonObject {
                        put("success", hits.isNotEmpty())
                        put("query", query)
                        put("count", hits.size)
                        put(
                            "results",
                            buildJsonArray {
                                hits.forEach { hit ->
                                    add(
                                        buildJsonObject {
                                            put("title", hit.title)
                                            put("url", hit.url)
                                            put("snippet", hit.snippet)
                                        }
                                    )
                                }
                            },
                        )
                    }
                }
            },
        )
    )
}

// -- headless browser ---------------------------------------------------------

private fun CommandRegistry.registerBrowserTools(browser: BrowserEngine, context: Context) {
    register(
        ToolDefinition(
            name = "browser_open",
            description = "Load a URL in the built-in headless WebView (JavaScript enabled) and report the outcome.",
            inputSchema = schema(
                "url" to prop("string", "URL to load"),
                "timeout_ms" to prop("integer", "Load timeout in milliseconds (default 30000)"),
                required = listOf("url"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val url = args.string("url") ?: throw IllegalArgumentException("Missing 'url' parameter")
                val timeout = (args.int("timeout_ms") ?: 30_000).toLong().coerceIn(1_000L, 120_000L)
                val status = browser.open(url, timeout)
                val title = runCatching { browser.pageTitle() }.getOrDefault("")
                val preview = runCatching { browser.pageText(1_000) }.getOrDefault("")
                buildJsonObject {
                    put("success", status == "loaded")
                    put("status", status)
                    put("url", runCatching { browser.currentUrl() }.getOrDefault(url))
                    put("title", title)
                    put("text_preview", preview)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_content",
            description = "Return the rendered text of the page currently loaded in the headless browser.",
            inputSchema = schema(
                "max_chars" to prop("integer", "Maximum characters returned (default 20000)"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val maxChars = (args.int("max_chars") ?: 20_000).coerceIn(500, 500_000)
                buildJsonObject {
                    put("success", browser.isOpen())
                    put("url", runCatching { browser.currentUrl() }.getOrDefault(""))
                    put("title", runCatching { browser.pageTitle() }.getOrDefault(""))
                    put("text", runCatching { browser.pageText(maxChars) }.getOrDefault(""))
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_eval",
            description = "Execute JavaScript in the loaded page and return the result.",
            inputSchema = schema(
                "script" to prop("string", "JavaScript expression or IIFE to evaluate"),
                required = listOf("script"),
            ),
            permissionLevel = PermissionLevel.ELEVATED,
            handler = { args ->
                val script = args.string("script") ?: throw IllegalArgumentException("Missing 'script' parameter")
                val result = browser.evaluate(
                    "(function(){try{return String(eval(" + quote(script) + "));}catch(e){return 'ERROR: '+e;}})()"
                )
                buildJsonObject {
                    put("success", !result.startsWith("ERROR:"))
                    put("result", result)
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_screenshot",
            description = "Render the loaded page to a PNG and save it into the public Download folder.",
            inputSchema = schema(
                "file_name" to prop("string", "Optional file name (default mcp_browser_<timestamp>.png)"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val bytes = browser.screenshot()
                val name = args.string("file_name")?.takeIf { it.isNotBlank() }
                    ?: "mcp_browser_${timestamp()}.png"
                val path = PublicFiles.saveToDownloads(context, name, "image/png", bytes)
                buildJsonObject {
                    put("success", path != null)
                    put("path", path ?: "")
                    put("bytes", bytes.size)
                    put("url", runCatching { browser.currentUrl() }.getOrDefault(""))
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "browser_close",
            description = "Destroy the headless WebView and free its memory.",
            inputSchema = schema(),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = {
                browser.close()
                buildJsonObject {
                    put("success", true)
                    put("open", browser.isOpen())
                }
            },
        )
    )
}

// -- shizuku ------------------------------------------------------------------

private fun CommandRegistry.registerShizukuTools(shizukuManager: ShizukuManager) {
    register(
        ToolDefinition(
            name = "shizuku_status",
            description = "Report whether Shizuku is installed, running and permitted, and whether shell commands run elevated.",
            inputSchema = schema(),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = {
                val detail = shizukuManager.describe()
                buildJsonObject {
                    put("success", detail["granted"] == true)
                    put("state", detail["state"].toString())
                    put("installed", detail["installed"] == true)
                    put("binder_alive", detail["binder_alive"] == true)
                    put("pre_v11", detail["pre_v11"] == true)
                    put("granted", detail["granted"] == true)
                    put("shizuku_version", PrivilegedShell.shizukuVersion())
                    put("root_available", PrivilegedShell.isRootAvailable())
                    put("active_executor", PrivilegedShell.bestExecutor())
                    put("hint", "granted=false 且 binder_alive=false 表示 Shizuku 应用未运行；root_available=true 时命令会走 su")
                }
            },
        )
    )

    register(
        ToolDefinition(
            name = "shizuku_request_permission",
            description = "Ask Shizuku for shell permission (the Shizuku app shows a confirmation dialog).",
            inputSchema = schema(
                "request_code" to prop("integer", "Optional request code (default 1001)"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val code = args.int("request_code") ?: 1001
                val requested = shizukuManager.requestPermission(code)
                val detail = shizukuManager.describe()
                buildJsonObject {
                    put("success", requested)
                    put("request_code", code)
                    put("state", detail["state"].toString())
                    put("installed", detail["installed"] == true)
                    put("binder_alive", detail["binder_alive"] == true)
                    put("deferred_until_binder", detail["pending_request_code"]?.toString()?.toIntOrNull() ?: -1)
                    put("root_available", PrivilegedShell.isRootAvailable())
                    put(
                        "hint",
                        when {
                            requested -> "已发送请求，请在 Shizuku 弹窗中允许"
                            detail["installed"] != true -> "未安装 Shizuku，无法申请；可用 root 通道"
                            detail["binder_alive"] != true -> "Shizuku 应用未运行，已登记为待申请，Shizuku 启动后会自动弹出授权"
                            else -> "Shizuku 已运行但申请未发出，请重试"
                        },
                    )
                }
            },
        )
    )
}

// -- files --------------------------------------------------------------------

private fun CommandRegistry.registerSaveTextFile(context: Context) {
    register(
        ToolDefinition(
            name = "save_text_file",
            description = "Write a UTF-8 text file into the public Download folder and return its path.",
            inputSchema = schema(
                "file_name" to prop("string", "Target file name, e.g. notes.txt"),
                "content" to prop("string", "Full file content"),
                required = listOf("file_name", "content"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val name = args.string("file_name") ?: throw IllegalArgumentException("Missing 'file_name' parameter")
                val content = args.string("content") ?: throw IllegalArgumentException("Missing 'content' parameter")
                val bytes = content.toByteArray(Charsets.UTF_8)
                val path = PublicFiles.saveToDownloads(context, name, "text/plain", bytes)
                buildJsonObject {
                    put("success", path != null)
                    put("path", path ?: "")
                    put("bytes", bytes.size)
                }
            },
        )
    )
}

// -- helpers ------------------------------------------------------------------

private fun JsonObject?.string(name: String): String? = (this?.get(name) as? JsonPrimitive)?.contentOrNull

private fun JsonObject?.int(name: String): Int? = (this?.get(name) as? JsonPrimitive)?.intOrNull

private fun prop(type: String, description: String) = buildJsonObject {
    put("type", type)
    put("description", description)
}

private fun schema(vararg properties: Pair<String, JsonObject>, required: List<String> = emptyList()) = buildJsonObject {
    put("type", "object")
    put(
        "properties",
        buildJsonObject {
            properties.forEach { (name, definition) -> put(name, definition) }
        },
    )
    if (required.isNotEmpty()) {
        put(
            "required",
            buildJsonArray { required.forEach { add(it) } },
        )
    }
}

private fun quote(value: String): String = buildString {
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

private fun timestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
