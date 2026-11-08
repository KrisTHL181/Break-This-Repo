package com.mcpserver.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.mcpserver.McpApplication
import com.mcpserver.data.SettingsStore
import com.mcpserver.model.TransportType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    data class SettingsUiState(
        val useSse: Boolean = true,
        val port: String = SettingsStore.DEFAULT_PORT.toString(),
        val autoStart: Boolean = false,
        val authEnabled: Boolean = false,
        val apiKey: String = "",
        val permissionLevel: String = "NORMAL",
        val blockDangerousCommands: Boolean = true,
        val commandTimeout: Float = 30f,
        val logLevel: String = "INFO",
        val serverRunning: Boolean = false,
    )

    private val store: SettingsStore? = runCatching { McpApplication.instance.mcpServer.settingsStore }.getOrNull()

    private val _uiState = MutableStateFlow(readFromStore())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private fun readFromStore(): SettingsUiState {
        val running = runCatching { McpApplication.instance.mcpServer.state.value.isRunning }.getOrDefault(false)
        val current = store ?: return SettingsUiState(serverRunning = running)
        return SettingsUiState(
            useSse = current.transport == TransportType.SSE,
            port = current.port.toString(),
            autoStart = current.autoStart,
            authEnabled = current.authEnabled,
            apiKey = current.apiKey,
            permissionLevel = current.permissionLevel,
            blockDangerousCommands = current.blockDangerousCommands,
            commandTimeout = current.commandTimeoutSeconds.toFloat(),
            logLevel = current.logLevel,
            serverRunning = running,
        )
    }

    fun refresh() {
        _uiState.value = readFromStore()
    }

    fun setUseSse(enabled: Boolean) {
        store?.transport = if (enabled) TransportType.SSE else TransportType.STDIO
        _uiState.update { it.copy(useSse = enabled) }
    }

    fun setPort(value: String) {
        val digits = value.filter { it.isDigit() }.take(5)
        _uiState.update { it.copy(port = digits) }
        digits.toIntOrNull()?.let { if (it in 1024..65535) store?.port = it }
    }

    fun setAutoStart(enabled: Boolean) {
        store?.autoStart = enabled
        _uiState.update { it.copy(autoStart = enabled) }
    }

    fun setAuthEnabled(enabled: Boolean) {
        store?.authEnabled = enabled
        _uiState.update { it.copy(authEnabled = enabled) }
    }

    fun generateApiKey() {
        val key = store?.generateApiKey() ?: return
        _uiState.update { it.copy(apiKey = key, authEnabled = true) }
    }

    fun revokeApiKey() {
        store?.revokeApiKey()
        _uiState.update { it.copy(apiKey = "", authEnabled = false) }
    }

    fun setPermissionLevel(level: String) {
        store?.permissionLevel = level
        _uiState.update { it.copy(permissionLevel = level) }
    }

    fun setBlockDangerousCommands(enabled: Boolean) {
        store?.blockDangerousCommands = enabled
        _uiState.update { it.copy(blockDangerousCommands = enabled) }
    }

    fun setCommandTimeout(seconds: Float) {
        store?.commandTimeoutSeconds = seconds.toInt()
        _uiState.update { it.copy(commandTimeout = seconds) }
    }

    fun setLogLevel(level: String) {
        store?.logLevel = level
        _uiState.update { it.copy(logLevel = level) }
    }
}
