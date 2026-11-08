package com.mcpserver.model

/**
 * Application Settings
 */
data class AppSettings(
    // Server settings
    val serverEnabled: Boolean = false,
    val serverPort: Int = 3000,

    // Transport settings
    val transportType: TransportType = TransportType.STDIO,
    val sseEndpoint: String = "",

    // Shizuku settings
    val shizukuEnabled: Boolean = true,
    val autoStartShizuku: Boolean = true,

    // ADB settings
    val adbEnabled: Boolean = true,
    val adbPort: Int = 5555,
    val wirelessAdbEnabled: Boolean = false,

    // Security settings
    val authEnabled: Boolean = false,
    val authToken: String = "",
    val allowedCommands: List<String> = emptyList(),
    val blockedCommands: List<String> = listOf(
        "rm -rf /",
        "dd if=",
        "mkfs",
        "> /dev/sda"
    ),

    // Logging
    val logLevel: LogLevel = LogLevel.INFO,
    val maxLogEntries: Int = 1000
)

enum class TransportType {
    STDIO,
    SSE
}
