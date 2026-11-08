package com.mcpserver.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogViewModel(application: Application) : AndroidViewModel(application) {
    data class LogEntry(
        val timestamp: String,
        val level: String,    // DEBUG, INFO, WARN, ERROR
        val module: String,   // MCP, Shizuku, ADB, Auth
        val message: String
    )

    data class LogUiState(
        val entries: List<LogEntry> = emptyList(),
        val filterLevel: String = "ALL",
        val searchQuery: String = "",
        val autoScroll: Boolean = true
    )

    private val _uiState = MutableStateFlow(LogUiState())
    val uiState: StateFlow<LogUiState> = _uiState.asStateFlow()

    init {
        addSampleLogs()
    }

    private fun addSampleLogs() {
        val sampleLogs = listOf(
            LogEntry(
                timestamp = formatTimestamp(System.currentTimeMillis() - 300000),
                level = "INFO",
                module = "MCP",
                message = "MCP Server 启动成功"
            ),
            LogEntry(
                timestamp = formatTimestamp(System.currentTimeMillis() - 240000),
                level = "DEBUG",
                module = "Shizuku",
                message = "Shizuku 服务已连接"
            ),
            LogEntry(
                timestamp = formatTimestamp(System.currentTimeMillis() - 180000),
                level = "WARN",
                module = "Auth",
                message = "Binder 权限即将过期，需要重新授权"
            ),
            LogEntry(
                timestamp = formatTimestamp(System.currentTimeMillis() - 120000),
                level = "INFO",
                module = "MCP",
                message = "新客户端已连接: AI Assistant"
            ),
            LogEntry(
                timestamp = formatTimestamp(System.currentTimeMillis() - 60000),
                level = "ERROR",
                module = "ADB",
                message = "命令执行超时: ls -la /system"
            ),
            LogEntry(
                timestamp = formatTimestamp(System.currentTimeMillis()),
                level = "INFO",
                module = "MCP",
                message = "服务运行正常，已处理 15 个请求"
            )
        )
        _uiState.update { it.copy(entries = sampleLogs) }
    }

    fun addLog(level: String, module: String, message: String) {
        val entry = LogEntry(
            timestamp = formatTimestamp(System.currentTimeMillis()),
            level = level,
            module = module,
            message = message
        )
        _uiState.update { state ->
            state.copy(entries = state.entries + entry)
        }
    }

    fun setFilter(level: String) {
        _uiState.update { it.copy(filterLevel = level) }
    }

    fun setSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(entries = emptyList()) }
    }

    fun exportLogs(): Uri? {
        // TODO: Export logs to file
        return null
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
