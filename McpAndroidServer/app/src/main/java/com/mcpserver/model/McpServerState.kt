package com.mcpserver.model

data class McpServerState(
    val isRunning: Boolean = false,
    val status: String = "Stopped",
    val transportMode: TransportMode = TransportMode.STDIO,
    val transportType: String = "stdio",
    val host: String = "0.0.0.0",
    val port: Int = 8080,
    val connectedClients: List<ClientInfo> = emptyList(),
    val error: String? = null,
    val settings: ServerSettings = ServerSettings()
)

enum class TransportMode {
    STDIO, SSE
}

data class ClientInfo(
    val id: String,
    val name: String,
    val connectedAt: Long,
    val requestCount: Long = 0
)

data class PermissionState(
    val shizukuStatus: ShizukuStatus = ShizukuStatus.UNKNOWN,
    val adbMode: AdbMode = AdbMode.UNKNOWN,
    val binderPermission: Boolean = false,
    val shizukuVersion: String? = null
)

enum class ShizukuStatus {
    UNKNOWN, NOT_INSTALLED, RUNNING, NEED_AUTH, STOPPED
}

enum class AdbMode {
    UNKNOWN, SHIZUKU, WIRELESS_ADB, DISCONNECTED
}

data class DeviceInfo(
    val model: String = "",
    val androidVersion: String = "",
    val apiLevel: Int = 0,
    val batteryLevel: Int = 0,
    val isCharging: Boolean = false,
    val isWifiConnected: Boolean = false,
    val isRooted: Boolean = false
)

data class ServerSettings(
    val transportMode: TransportMode = TransportMode.STDIO,
    val port: Int = 8080,
    val autoStart: Boolean = false,
    val authEnabled: Boolean = false,
    val apiKey: String? = null,
    val commandPermissionLevel: PermissionLevel = PermissionLevel.STANDARD,
    val toolWhitelist: List<String> = emptyList(),
    val toolBlacklist: List<String> = emptyList(),
    val dangerousCommandBlock: Boolean = true,
    val autoDetectShizuku: Boolean = true,
    val autoReauthorize: Boolean = true,
    val logLevel: LogLevel = LogLevel.INFO,
    val commandTimeout: Int = 30,
    val maxConcurrent: Int = 5,
    val maxResultLength: Int = 10000
)

enum class PermissionLevel {
    STANDARD, ELEVATED, ROOT
}

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR
}

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String = "",
    val source: String = "",
    val message: String = ""
)
