package com.mcpserver.mcp.router

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.mcpserver.platform.shell.PrivilegedShell
import com.mcpserver.util.PublicFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import java.io.File
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

private const val TAG = "DeviceTools"
private const val MAX_LIST_ENTRIES = 2_000

/**
 * Real implementations for the built-in Android tools. They are registered with
 * the same names as the placeholders, and registration overwrites by name.
 */
fun CommandRegistry.registerDeviceTools(context: Context) {
    val appContext = context.applicationContext

    registerInstallApp()
    registerListPackages()
    registerStartApp()
    registerStopApp()
    registerUninstallApp()
    registerScreenshot()
    registerInputText()
    registerTapScreen()
    registerSwipeScreen()
    registerGetDeviceInfo()
    registerListFiles()
    registerReadFile()
    registerWriteFile(appContext)
    registerGetBatteryInfo(appContext)
    registerGetWifiInfo(appContext)
    registerGetSettings()
    registerPutSettings()
    registerBroadcastIntent()
    registerStartActivity()

    Log.i(TAG, "Real device tools registered (total=${toolCount})")
}

// ── package management ────────────────────────────────────────────────────────

private fun CommandRegistry.registerInstallApp() {
    register(
        ToolDefinition(
            name = "install_app",
            description = "Install an APK file on the device via pm install (needs Shizuku or root).",
            inputSchema = dSchema(
                "apk_path" to dProp("string", "Absolute path of the APK, e.g. /sdcard/Download/app.apk"),
                "reinstall" to dProp("boolean", "Keep existing data and reinstall (default true)"),
                required = listOf("apk_path"),
            ),
            permissionLevel = PermissionLevel.DANGEROUS,
            handler = { args ->
                val path = args.dStr("apk_path") ?: throw IllegalArgumentException("Missing 'apk_path' parameter")
                val flags = if (args.dBool("reinstall") == false) "" else "-r "
                val result = PrivilegedShell.exec("pm install ${flags}-g \"$path\"", 180_000L)
                val ok = result.exitCode == 0 && result.stdout.contains("Success", ignoreCase = true)
                buildJsonObject {
                    put("success", ok)
                    put("executor", result.executor)
                    put("exit_code", result.exitCode)
                    put("stdout", result.stdout.take(20_000))
                    put("stderr", result.stderr.take(10_000))
                }
            },
        )
    )
}

private fun CommandRegistry.registerListPackages() {
    register(
        ToolDefinition(
            name = "list_packages",
            description = "List installed packages ('system', 'third_party' or a plain search string).",
            inputSchema = dSchema(
                "filter" to dProp("string", "'system', 'third_party', or a search substring"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val filter = args.dStr("filter")?.trim().orEmpty()
                val command = when (filter.lowercase()) {
                    "system" -> "pm list packages -s"
                    "third_party", "user" -> "pm list packages -3"
                    "" -> "pm list packages"
                    else -> "pm list packages | grep -i ${shellQuote(filter)}"
                }
                val result = PrivilegedShell.exec(command, 30_000L)
                val packages = result.stdout.lineSequence()
                    .map { it.trim().removePrefix("package:") }
                    .filter { it.isNotBlank() }
                    .take(MAX_LIST_ENTRIES)
                    .toList()
                buildJsonObject {
                    put("success", result.exitCode == 0)
                    put("count", packages.size)
                    put("packages", buildJsonArray { packages.forEach { add(it) } })
                }
            },
        )
    )
}

private fun CommandRegistry.registerStartApp() {
    register(
        ToolDefinition(
            name = "start_app",
            description = "Launch an application by package name.",
            inputSchema = dSchema(
                "package_name" to dProp("string", "Package name, e.g. com.tencent.mm"),
                required = listOf("package_name"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val pkg = args.dStr("package_name") ?: throw IllegalArgumentException("Missing 'package_name' parameter")
                val result = PrivilegedShell.exec(
                    "monkey -p ${shellQuote(pkg)} -c android.intent.category.LAUNCHER 1",
                    30_000L,
                )
                buildJsonObject {
                    put("success", result.exitCode == 0)
                    put("output", result.stdout.take(4_000).ifBlank { result.stderr.take(4_000) })
                }
            },
        )
    )
}

private fun CommandRegistry.registerStopApp() {
    register(
        ToolDefinition(
            name = "stop_app",
            description = "Force stop an application.",
            inputSchema = dSchema(
                "package_name" to dProp("string", "Package name to stop"),
                required = listOf("package_name"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val pkg = args.dStr("package_name") ?: throw IllegalArgumentException("Missing 'package_name' parameter")
                val result = PrivilegedShell.exec("am force-stop ${shellQuote(pkg)}", 30_000L)
                buildJsonObject {
                    put("success", result.exitCode == 0)
                    put("output", result.stdout.take(2_000).ifBlank { result.stderr.take(2_000) })
                }
            },
        )
    )
}

private fun CommandRegistry.registerUninstallApp() {
    register(
        ToolDefinition(
            name = "uninstall_app",
            description = "Uninstall an application by package name.",
            inputSchema = dSchema(
                "package_name" to dProp("string", "Package name to uninstall"),
                "keep_data" to dProp("boolean", "Keep app data (default false)"),
                required = listOf("package_name"),
            ),
            permissionLevel = PermissionLevel.DANGEROUS,
            handler = { args ->
                val pkg = args.dStr("package_name") ?: throw IllegalArgumentException("Missing 'package_name' parameter")
                val keep = if (args.dBool("keep_data") == true) "-k " else ""
                val result = PrivilegedShell.exec("pm uninstall $keep${shellQuote(pkg)}", 120_000L)
                buildJsonObject {
                    put("success", result.stdout.contains("Success", ignoreCase = true))
                    put("stdout", result.stdout.take(4_000))
                    put("stderr", result.stderr.take(4_000))
                }
            },
        )
    )
}

// ── screen control ────────────────────────────────────────────────────────────

private fun CommandRegistry.registerScreenshot() {
    register(
        ToolDefinition(
            name = "screenshot",
            description = "Capture the screen into the public Download folder (needs Shizuku or root).",
            inputSchema = dSchema(
                "file_name" to dProp("string", "Optional file name, default mcp_screen_<timestamp>.png"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val name = args.dStr("file_name")?.takeIf { it.isNotBlank() }
                    ?: "mcp_screen_${dTimestamp()}.png"
                val path = "/sdcard/Download/$name"
                val result = PrivilegedShell.exec("screencap -p ${shellQuote(path)}", 30_000L)
                val check = PrivilegedShell.exec("ls -l ${shellQuote(path)}", 10_000L)
                buildJsonObject {
                    put("success", check.exitCode == 0)
                    put("path", path)
                    put("executor", result.executor)
                    put("detail", check.stdout.trim().ifBlank { result.stderr.take(2_000) })
                }
            },
        )
    )
}

private fun CommandRegistry.registerInputText() {
    register(
        ToolDefinition(
            name = "input_text",
            description = "Type text into the focused input field (needs Shizuku or root).",
            inputSchema = dSchema(
                "text" to dProp("string", "Text to type"),
                required = listOf("text"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val text = args.dStr("text") ?: throw IllegalArgumentException("Missing 'text' parameter")
                // `input text` treats spaces as argument separators.
                val escaped = text.replace(" ", "%s")
                val result = PrivilegedShell.exec("input text ${shellQuote(escaped)}", 30_000L)
                buildJsonObject {
                    put("success", result.exitCode == 0)
                    put("executor", result.executor)
                    put("stderr", result.stderr.take(2_000))
                }
            },
        )
    )
}

private fun CommandRegistry.registerTapScreen() {
    register(
        ToolDefinition(
            name = "tap_screen",
            description = "Tap the screen at coordinates (needs Shizuku or root).",
            inputSchema = dSchema(
                "x" to dProp("integer", "X coordinate"),
                "y" to dProp("integer", "Y coordinate"),
                required = listOf("x", "y"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val x = args.dInt("x") ?: throw IllegalArgumentException("Missing 'x' parameter")
                val y = args.dInt("y") ?: throw IllegalArgumentException("Missing 'y' parameter")
                val result = PrivilegedShell.exec("input tap $x $y", 20_000L)
                buildJsonObject {
                    put("success", result.exitCode == 0)
                    put("executor", result.executor)
                }
            },
        )
    )
}

private fun CommandRegistry.registerSwipeScreen() {
    register(
        ToolDefinition(
            name = "swipe_screen",
            description = "Swipe between two points (needs Shizuku or root).",
            inputSchema = dSchema(
                "x1" to dProp("integer", "Start X"),
                "y1" to dProp("integer", "Start Y"),
                "x2" to dProp("integer", "End X"),
                "y2" to dProp("integer", "End Y"),
                "duration_ms" to dProp("integer", "Duration in milliseconds (default 300)"),
                required = listOf("x1", "y1", "x2", "y2"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val x1 = args.dInt("x1") ?: throw IllegalArgumentException("Missing 'x1'")
                val y1 = args.dInt("y1") ?: throw IllegalArgumentException("Missing 'y1'")
                val x2 = args.dInt("x2") ?: throw IllegalArgumentException("Missing 'x2'")
                val y2 = args.dInt("y2") ?: throw IllegalArgumentException("Missing 'y2'")
                val duration = (args.dInt("duration_ms") ?: 300).coerceIn(10, 20_000)
                val result = PrivilegedShell.exec("input swipe $x1 $y1 $x2 $y2 $duration", 20_000L)
                buildJsonObject {
                    put("success", result.exitCode == 0)
                    put("executor", result.executor)
                }
            },
        )
    )
}

// ── device / system info ──────────────────────────────────────────────────────

private fun CommandRegistry.registerGetDeviceInfo() {
    register(
        ToolDefinition(
            name = "get_device_info",
            description = "Real device information (model, Android version, SDK, screen, CPU ABI, uptime, IP).",
            inputSchema = dSchema(),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = {
                val size = PrivilegedShell.exec("wm size", 10_000L).stdout.trim()
                val density = PrivilegedShell.exec("wm density", 10_000L).stdout.trim()
                val uptime = PrivilegedShell.exec("uptime", 10_000L).stdout.trim()
                val ip = PrivilegedShell.exec(
                    "ip -4 addr show wlan0 2>/dev/null | grep -o 'inet [0-9.]*' | head -1 | cut -d' ' -f2",
                    10_000L,
                ).stdout.trim()
                buildJsonObject {
                    put("success", true)
                    put("model", Build.MODEL)
                    put("manufacturer", Build.MANUFACTURER)
                    put("brand", Build.BRAND)
                    put("device", Build.DEVICE)
                    put("product", Build.PRODUCT)
                    put("android_version", Build.VERSION.RELEASE)
                    put("sdk_int", Build.VERSION.SDK_INT)
                    put("fingerprint", Build.FINGERPRINT)
                    put("abis", Build.SUPPORTED_ABIS.joinToString(", "))
                    put("screen", size)
                    put("density", density)
                    put("uptime", uptime)
                    put("wifi_ip", ip)
                }
            },
        )
    )
}

private fun CommandRegistry.registerListFiles() {
    register(
        ToolDefinition(
            name = "list_files",
            description = "List a directory using this app's filesystem access.",
            inputSchema = dSchema(
                "path" to dProp("string", "Directory path, e.g. /sdcard/Download"),
                "recursive" to dProp("boolean", "List recursively (default false)"),
                required = listOf("path"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val path = args.dStr("path") ?: throw IllegalArgumentException("Missing 'path' parameter")
                val recursive = args.dBool("recursive") == true
                val root = File(path)
                if (!root.exists()) {
                    buildJsonObject {
                        put("success", false)
                        put("error", "Not found: $path")
                    }
                } else {
                    val entries = mutableListOf<JsonObject>()
                    val queue = ArrayDeque<File>()
                    queue.add(root)
                    while (queue.isNotEmpty() && entries.size < MAX_LIST_ENTRIES) {
                        val dir = queue.removeFirst()
                        val children = dir.listFiles() ?: continue
                        for (child in children) {
                            if (entries.size >= MAX_LIST_ENTRIES) break
                            entries.add(
                                buildJsonObject {
                                    put("name", child.name)
                                    put("path", child.absolutePath)
                                    put("is_dir", child.isDirectory)
                                    put("size", if (child.isFile) child.length() else 0L)
                                    put("modified", child.lastModified())
                                }
                            )
                            if (recursive && child.isDirectory) queue.add(child)
                        }
                    }
                    buildJsonObject {
                        put("success", true)
                        put("path", root.absolutePath)
                        put("count", entries.size)
                        put("entries", buildJsonArray { entries.forEach { add(it) } })
                    }
                }
            },
        )
    )
}

private fun CommandRegistry.registerReadFile() {
    register(
        ToolDefinition(
            name = "read_file",
            description = "Read a text file (falls back to an elevated read when the app has no access).",
            inputSchema = dSchema(
                "path" to dProp("string", "File path"),
                "max_bytes" to dProp("integer", "Maximum bytes to read (default 262144)"),
                required = listOf("path"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val path = args.dStr("path") ?: throw IllegalArgumentException("Missing 'path' parameter")
                val maxBytes = (args.dInt("max_bytes") ?: 262_144).coerceIn(1, 4 * 1024 * 1024)
                withContext(Dispatchers.IO) {
                    val file = File(path)
                    if (file.exists() && file.canRead()) {
                        val bytes = file.inputStream().use { stream ->
                            val buffer = ByteArray(maxBytes)
                            val read = stream.read(buffer)
                            if (read <= 0) ByteArray(0) else buffer.copyOf(read)
                        }
                        buildJsonObject {
                            put("success", true)
                            put("path", path)
                            put("bytes", bytes.size)
                            put("truncated", file.length() > bytes.size)
                            put("text", String(bytes, Charsets.UTF_8))
                        }
                    } else {
                        val result = PrivilegedShell.exec("cat ${shellQuote(path)}", 30_000L)
                        val text = result.stdout.take(maxBytes)
                        buildJsonObject {
                            put("success", result.exitCode == 0 && text.isNotBlank())
                            put("path", path)
                            put("bytes", text.toByteArray().size)
                            put("executor", result.executor)
                            put("text", text)
                            put("stderr", result.stderr.take(2_000))
                        }
                    }
                }
            },
        )
    )
}

private fun CommandRegistry.registerWriteFile(context: Context) {
    register(
        ToolDefinition(
            name = "write_file",
            description = "Write a UTF-8 text file (app access first, then elevated write).",
            inputSchema = dSchema(
                "path" to dProp("string", "Absolute file path"),
                "content" to dProp("string", "File content"),
                required = listOf("path", "content"),
            ),
            permissionLevel = PermissionLevel.ELEVATED,
            handler = { args ->
                val path = args.dStr("path") ?: throw IllegalArgumentException("Missing 'path' parameter")
                val content = args.dStr("content") ?: throw IllegalArgumentException("Missing 'content' parameter")
                withContext(Dispatchers.IO) {
                    val direct = runCatching {
                        val file = File(path)
                        file.parentFile?.mkdirs()
                        file.writeText(content)
                        true
                    }.getOrDefault(false)

                    if (direct) {
                        buildJsonObject {
                            put("success", true)
                            put("path", path)
                            put("bytes", content.toByteArray().size)
                            put("executor", "app")
                        }
                    } else if (path.startsWith("/sdcard/Download") || path.startsWith("/storage/emulated/0/Download")) {
                        val name = File(path).name
                        val saved = PublicFiles.saveToDownloads(
                            context,
                            name,
                            "text/plain",
                            content.toByteArray(Charsets.UTF_8),
                        )
                        buildJsonObject {
                            put("success", saved != null)
                            put("path", saved ?: path)
                            put("bytes", content.toByteArray().size)
                            put("executor", "mediastore")
                        }
                    } else {
                        val encoded = Base64.getEncoder().encodeToString(content.toByteArray(Charsets.UTF_8))
                        val result = PrivilegedShell.exec(
                            "echo ${shellQuote(encoded)} | base64 -d > ${shellQuote(path)}",
                            30_000L,
                        )
                        buildJsonObject {
                            put("success", result.exitCode == 0)
                            put("path", path)
                            put("bytes", content.toByteArray().size)
                            put("executor", result.executor)
                            put("stderr", result.stderr.take(2_000))
                        }
                    }
                }
            },
        )
    )
}

private fun CommandRegistry.registerGetBatteryInfo(context: Context) {
    register(
        ToolDefinition(
            name = "get_battery_info",
            description = "Battery level, charging state and raw dumpsys battery output.",
            inputSchema = dSchema(),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = {
                val manager = context.getSystemService(BatteryManager::class.java)
                val level = manager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                val charging = manager?.isCharging ?: false
                val dump = PrivilegedShell.exec("dumpsys battery", 15_000L).stdout
                val fields = dump.lineSequence()
                    .map { it.trim() }
                    .filter { it.contains(":") }
                    .associate { it.substringBefore(":").trim() to it.substringAfter(":").trim() }
                buildJsonObject {
                    put("success", true)
                    put("level", level)
                    put("charging", charging)
                    put("status", fields["status"] ?: "")
                    put("temperature", fields["temperature"] ?: "")
                    put("voltage", fields["voltage"] ?: "")
                    put("health", fields["health"] ?: "")
                    put("raw", dump.take(4_000))
                }
            },
        )
    )
}

private fun CommandRegistry.registerGetWifiInfo(context: Context) {
    register(
        ToolDefinition(
            name = "get_wifi_info",
            description = "Real WiFi details: SSID, BSSID, RSSI, link speed, frequency, IP, gateway, DNS.",
            inputSchema = dSchema(),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = {
                val manager = context.getSystemService(ConnectivityManager::class.java)
                val network = manager?.activeNetwork
                val capabilities = network?.let { manager.getNetworkCapabilities(it) }
                val properties = network?.let { manager.getLinkProperties(it) }
                val status = PrivilegedShell.exec("cmd wifi status", 15_000L).stdout
                val dump = PrivilegedShell.exec("dumpsys wifi", 20_000L).stdout

                fun firstMatch(pattern: String): String? =
                    Regex(pattern, RegexOption.IGNORE_CASE).find(status)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
                        ?: Regex(pattern, RegexOption.IGNORE_CASE).find(dump)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

                val ip = PrivilegedShell.exec(
                    "ip -4 addr show wlan0 2>/dev/null | grep -o 'inet [0-9.]*' | head -1 | cut -d' ' -f2",
                    10_000L,
                ).stdout.trim()
                val route = PrivilegedShell.exec("ip route 2>/dev/null | grep default", 10_000L).stdout.trim()
                val gateway = Regex("default via ([0-9.]+)").find(route)?.groupValues?.get(1) ?: ""
                val interfaceName = properties?.interfaceName
                    ?: PrivilegedShell.exec("getprop wifi.interface", 8_000L).stdout.trim().ifBlank { "wlan0" }
                val enabled = PrivilegedShell.exec("cmd wifi status 2>/dev/null | head -1", 10_000L).stdout.trim()

                buildJsonObject {
                    put("success", true)
                    put("wifi_enabled", !enabled.contains("disabled", ignoreCase = true))
                    put("wifi_connected", capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false)
                    put("ssid", firstMatch("SSID:\\s*\"?([^\",\\n]+)") ?: "")
                    put("bssid", firstMatch("BSSID:\\s*([0-9a-fA-F:]{17})") ?: "")
                    put("rssi_dbm", firstMatch("RSSI:\\s*(-?\\d+)") ?: "")
                    put("link_speed_mbps", firstMatch("Link speed:\\s*(\\d+)") ?: "")
                    put("frequency_mhz", firstMatch("Frequency:\\s*(\\d+)") ?: "")
                    put("supplicant_state", firstMatch("Supplicant state:\\s*(\\w+)") ?: "")
                    put("interface", interfaceName)
                    put("ip_address", ip)
                    put("gateway", gateway)
                    put("dns", buildJsonArray { properties?.dnsServers?.forEach { add(it.hostAddress ?: "") } })
                    put("link_down_kbps", capabilities?.linkDownstreamBandwidthKbps ?: 0)
                    put("link_up_kbps", capabilities?.linkUpstreamBandwidthKbps ?: 0)
                    put("status_raw", status.take(1_500))
                }
            },
        )
    )
}

private fun CommandRegistry.registerGetSettings() {
    register(
        ToolDefinition(
            name = "get_settings",
            description = "Read a system setting via `settings get`.",
            inputSchema = dSchema(
                "namespace" to dProp("string", "system | secure | global"),
                "key" to dProp("string", "Setting key"),
                required = listOf("namespace", "key"),
            ),
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val namespace = args.dStr("namespace") ?: throw IllegalArgumentException("Missing 'namespace'")
                val key = args.dStr("key") ?: throw IllegalArgumentException("Missing 'key'")
                val result = PrivilegedShell.exec(
                    "settings get ${shellQuote(namespace)} ${shellQuote(key)}",
                    15_000L,
                )
                buildJsonObject {
                    put("success", result.exitCode == 0)
                    put("value", result.stdout.trim())
                    put("executor", result.executor)
                }
            },
        )
    )
}

private fun CommandRegistry.registerPutSettings() {
    register(
        ToolDefinition(
            name = "put_settings",
            description = "Write a system setting via `settings put` (needs Shizuku or root).",
            inputSchema = dSchema(
                "namespace" to dProp("string", "system | secure | global"),
                "key" to dProp("string", "Setting key"),
                "value" to dProp("string", "New value"),
                required = listOf("namespace", "key", "value"),
            ),
            permissionLevel = PermissionLevel.ELEVATED,
            handler = { args ->
                val namespace = args.dStr("namespace") ?: throw IllegalArgumentException("Missing 'namespace'")
                val key = args.dStr("key") ?: throw IllegalArgumentException("Missing 'key'")
                val value = args.dStr("value") ?: throw IllegalArgumentException("Missing 'value'")
                val result = PrivilegedShell.exec(
                    "settings put ${shellQuote(namespace)} ${shellQuote(key)} ${shellQuote(value)}",
                    20_000L,
                )
                buildJsonObject {
                    put("success", result.exitCode == 0)
                    put("executor", result.executor)
                    put("stderr", result.stderr.take(2_000))
                }
            },
        )
    )
}

private fun CommandRegistry.registerBroadcastIntent() {
    register(
        ToolDefinition(
            name = "broadcast_intent",
            description = "Send a broadcast via `am broadcast`, with optional string extras.",
            inputSchema = dSchema(
                "action" to dProp("string", "Broadcast action, e.g. android.intent.action.AIRPLANE_MODE"),
                "extras" to dProp("object", "Optional string extras"),
                required = listOf("action"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val action = args.dStr("action") ?: throw IllegalArgumentException("Missing 'action'")
                val extras = (args?.get("extras") as? JsonObject)
                    ?.mapNotNull { (key, value) ->
                        (value as? JsonPrimitive)?.contentOrNull?.let { "--es ${shellQuote(key)} ${shellQuote(it)}" }
                    }
                    ?.joinToString(" ")
                    .orEmpty()
                val command = "am broadcast -a ${shellQuote(action)} $extras".trim()
                val result = PrivilegedShell.exec(command, 30_000L)
                buildJsonObject {
                    put("success", result.stdout.contains("Broadcast completed", ignoreCase = true) || result.exitCode == 0)
                    put("command", command)
                    put("output", result.stdout.take(4_000).ifBlank { result.stderr.take(2_000) })
                }
            },
        )
    )
}

private fun CommandRegistry.registerStartActivity() {
    register(
        ToolDefinition(
            name = "start_activity",
            description = "Start an activity (`am start -n pkg/activity`); falls back to the launcher intent.",
            inputSchema = dSchema(
                "package_name" to dProp("string", "Package name"),
                "activity_name" to dProp("string", "Optional fully qualified activity name"),
                required = listOf("package_name"),
            ),
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val pkg = args.dStr("package_name") ?: throw IllegalArgumentException("Missing 'package_name'")
                val activity = args.dStr("activity_name")
                val command = if (activity.isNullOrBlank()) {
                    "monkey -p ${shellQuote(pkg)} -c android.intent.category.LAUNCHER 1"
                } else {
                    val component = if (activity.startsWith(".")) "$pkg$activity" else activity
                    "am start -n ${shellQuote("$pkg/$component")}"
                }
                val result = PrivilegedShell.exec(command, 30_000L)
                buildJsonObject {
                    put("success", result.exitCode == 0)
                    put("command", command)
                    put("output", result.stdout.take(4_000).ifBlank { result.stderr.take(2_000) })
                }
            },
        )
    )
}

// ── helpers ───────────────────────────────────────────────────────────────────

private fun JsonObject?.dStr(name: String): String? = (this?.get(name) as? JsonPrimitive)?.contentOrNull

private fun JsonObject?.dInt(name: String): Int? = (this?.get(name) as? JsonPrimitive)?.intOrNull

private fun JsonObject?.dBool(name: String): Boolean? = (this?.get(name) as? JsonPrimitive)?.booleanOrNull

private fun dProp(type: String, description: String) = buildJsonObject {
    put("type", type)
    put("description", description)
}

private fun dSchema(vararg properties: Pair<String, JsonObject>, required: List<String> = emptyList()) = buildJsonObject {
    put("type", "object")
    put("properties", buildJsonObject { properties.forEach { (name, value) -> put(name, value) } })
    if (required.isNotEmpty()) {
        put("required", buildJsonArray { required.forEach { add(it) } })
    }
}

/** Single-quote a shell argument safely. */
private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"

private fun dTimestamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
