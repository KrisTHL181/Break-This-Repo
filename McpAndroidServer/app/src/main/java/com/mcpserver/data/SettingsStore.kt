package com.mcpserver.data

import android.content.Context
import android.content.SharedPreferences
import com.mcpserver.model.AppSettings
import com.mcpserver.model.LogLevel
import com.mcpserver.model.TransportType
import java.util.UUID

/**
 * Persisted settings plus the plain-text API key.
 *
 * SharedPreferences is used deliberately: reads are synchronous, so the server
 * always starts with the values the user just typed.
 */
class SettingsStore(context: Context) {

    companion object {
        private const val PREFS = "mcp_settings"
        private const val KEY_TRANSPORT = "transport"
        private const val KEY_PORT = "port"
        private const val KEY_AUTO_START = "auto_start"
        private const val KEY_AUTH_ENABLED = "auth_enabled"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_COMMAND_TIMEOUT = "command_timeout"
        private const val KEY_LOG_LEVEL = "log_level"
        private const val KEY_PERMISSION_LEVEL = "permission_level"
        private const val KEY_BLOCK_DANGEROUS = "block_dangerous"

        const val DEFAULT_PORT = 8392
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var transport: TransportType
        get() = if (prefs.getString(KEY_TRANSPORT, null) == TransportType.STDIO.name) {
            TransportType.STDIO
        } else {
            TransportType.SSE
        }
        set(value) = prefs.edit().putString(KEY_TRANSPORT, value.name).apply()

    var port: Int
        get() = prefs.getInt(KEY_PORT, DEFAULT_PORT)
        set(value) = prefs.edit().putInt(KEY_PORT, value).apply()

    var autoStart: Boolean
        get() = prefs.getBoolean(KEY_AUTO_START, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_START, value).apply()

    var authEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTH_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTH_ENABLED, value).apply()

    /** Plain-text key so the UI can show/copy it. */
    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value).apply()

    var commandTimeoutSeconds: Int
        get() = prefs.getInt(KEY_COMMAND_TIMEOUT, 30)
        set(value) = prefs.edit().putInt(KEY_COMMAND_TIMEOUT, value.coerceIn(5, 300)).apply()

    var logLevel: String
        get() = prefs.getString(KEY_LOG_LEVEL, LogLevel.INFO.name) ?: LogLevel.INFO.name
        set(value) = prefs.edit().putString(KEY_LOG_LEVEL, value).apply()

    var permissionLevel: String
        get() = prefs.getString(KEY_PERMISSION_LEVEL, "NORMAL") ?: "NORMAL"
        set(value) = prefs.edit().putString(KEY_PERMISSION_LEVEL, value).apply()

    var blockDangerousCommands: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_DANGEROUS, true)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_DANGEROUS, value).apply()

    fun hasApiKey(): Boolean = apiKey.isNotBlank()

    fun generateApiKey(): String {
        val key = "mcp_" + UUID.randomUUID().toString().replace("-", "")
        apiKey = key
        authEnabled = true
        return key
    }

    fun revokeApiKey() {
        apiKey = ""
        authEnabled = false
    }

    fun toAppSettings(): AppSettings = AppSettings(
        serverEnabled = true,
        serverPort = port,
        transportType = transport,
        authEnabled = authEnabled,
        authToken = apiKey,
        logLevel = runCatching { LogLevel.valueOf(logLevel) }.getOrDefault(LogLevel.INFO),
    )
}
