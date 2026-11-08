package com.mcpserver.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.os.BatteryManager
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewModelScope
import com.mcpserver.McpApplication
import com.mcpserver.platform.ServerService
import com.mcpserver.platform.adb.AdbController
import com.mcpserver.platform.shell.PrivilegedShell
import com.mcpserver.platform.shizuku.ShizukuManager
import com.mcpserver.ui.theme.StatusGreen
import com.mcpserver.ui.theme.StatusRed
import com.mcpserver.ui.theme.StatusYellow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    data class HomeUiState(
        val serverRunning: Boolean = false,
        val serverAddress: String = "",
        val toolCount: Int = 0,
        val shizukuState: String = "检测中...",
        val shizukuStateColor: Color = StatusYellow,
        val adbMode: String = "应用进程",
        val adbStatus: String = "检测中...",
        val deviceModel: String = "",
        val androidVersion: String = "",
        val apiLevel: Int = 0,
        val batteryLevel: Int = 0,
        val batteryCharging: Boolean = false,
        val wifiConnected: Boolean = false,
        val lastResult: String = "",
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDeviceInfo()
        refreshShizuku()
        refreshServer()
        refreshAdb()
    }

    // ── device info ─────────────────────────────────────────────────────────

    private fun loadDeviceInfo() {
        val context = getApplication<Application>()
        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
        val isCharging = batteryManager?.isCharging ?: false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager
        val wifi = connectivityManager?.let { manager ->
            val network = manager.activeNetwork ?: return@let false
            val capabilities = manager.getNetworkCapabilities(network) ?: return@let false
            capabilities.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)
        } ?: false

        _uiState.update {
            it.copy(
                deviceModel = Build.MODEL,
                androidVersion = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                batteryLevel = batteryLevel,
                batteryCharging = isCharging,
                wifiConnected = wifi,
            )
        }
    }

    // ── server ──────────────────────────────────────────────────────────────

    private fun refreshServer() {
        val app = runCatching { McpApplication.instance }.getOrNull()
        val running = app?.let { runCatching { it.mcpServer.state.value.isRunning }.getOrDefault(false) } ?: false
        val port = app?.let { runCatching { it.mcpServer.settingsStore.port }.getOrDefault(8392) } ?: 8392
        _uiState.update {
            it.copy(
                serverRunning = running,
                serverAddress = if (running) "http://${localIpv4() ?: "127.0.0.1"}:$port/events" else "",
            )
        }
    }

    fun toggleServer() {
        val app = runCatching { McpApplication.instance }.getOrNull()
        viewModelScope.launch {
            if (app == null || !app.isReady) {
                _uiState.update { it.copy(lastResult = "应用未就绪，服务无法启动") }
                return@launch
            }
            val server = app.mcpServer
            val context = getApplication<Application>()
            if (_uiState.value.serverRunning) {
                runCatching { context.stopService(Intent(context, ServerService::class.java)) }
                runCatching { server.stop() }
                _uiState.update { it.copy(serverRunning = false, serverAddress = "", lastResult = "服务已停止") }
            } else {
                val port = runCatching { server.settingsStore.port }.getOrDefault(8392)
                runCatching { ContextCompat.startForegroundService(context, Intent(context, ServerService::class.java)) }
                    .onFailure { error ->
                        _uiState.update { it.copy(lastResult = "前台服务启动失败: ${error.message}") }
                    }
                delay(1_500)
                refreshServer()
                _uiState.update {
                    it.copy(
                        lastResult = if (it.serverRunning) {
                            "服务已启动（前台服务保活）: http://${localIpv4() ?: "127.0.0.1"}:$port/events"
                        } else {
                            "启动未成功，端口 $port 可能被占用"
                        },
                    )
                }
            }
        }
    }

    // ── shizuku ─────────────────────────────────────────────────────────────

    private fun refreshShizuku() {
        val manager = runCatching { McpApplication.instance.shizukuManager }.getOrNull()
        val state = runCatching { manager?.refresh() }.getOrNull()
        _uiState.update {
            it.copy(
                shizukuState = shizukuLabel(state),
                shizukuStateColor = shizukuColor(state),
                adbMode = if (state is ShizukuManager.ShizukuState.PermissionGranted) "Shizuku (shell)" else "应用进程",
            )
        }
    }

    fun reconnectShizuku() {
        viewModelScope.launch {
            _uiState.update { it.copy(shizukuState = "重新检测中...", shizukuStateColor = StatusYellow) }
            val manager = runCatching { McpApplication.instance.shizukuManager }.getOrNull()
            runCatching { manager?.initialize() }
            refreshShizuku()
        }
    }

    fun requestShizukuPermission() {
        viewModelScope.launch {
            val manager = runCatching { McpApplication.instance.shizukuManager }.getOrNull()
            val requested = runCatching { manager?.requestPermission(1001) }.getOrDefault(false) == true
            _uiState.update {
                it.copy(
                    lastResult = if (requested) {
                        "已向 Shizuku 发送授权请求，请在弹窗中点击允许"
                    } else {
                        "无法请求授权：请先安装并启动 Shizuku（或 Shizuku 未运行）"
                    }
                )
            }
            refreshShizuku()
        }
    }

    private fun shizukuLabel(state: ShizukuManager.ShizukuState?): String = when (state) {
        null -> "应用未就绪"
        ShizukuManager.ShizukuState.NotInstalled -> "未安装 Shizuku"
        ShizukuManager.ShizukuState.NotRunning -> "Shizuku 未运行"
        ShizukuManager.ShizukuState.PermissionGranted -> "已授权"
        ShizukuManager.ShizukuState.PermissionDenied -> "未授权"
        is ShizukuManager.ShizukuState.Error -> "错误: ${state.message}"
    }

    private fun shizukuColor(state: ShizukuManager.ShizukuState?): Color = when (state) {
        ShizukuManager.ShizukuState.PermissionGranted -> StatusGreen
        ShizukuManager.ShizukuState.NotInstalled, ShizukuManager.ShizukuState.PermissionDenied, null -> StatusRed
        else -> StatusYellow
    }

    // ── adb ─────────────────────────────────────────────────────────────────

    private fun refreshAdb() {
        viewModelScope.launch {
            val status = runCatching { AdbController.status() }.getOrNull()
            _uiState.update {
                it.copy(
                    adbStatus = when {
                        status == null -> "检测失败"
                        status.wirelessEnabled -> "无线调试已开 (${status.wifiAddress ?: "?"}:${status.tcpPort})"
                        else -> "无线调试关闭"
                    }
                )
            }
        }
    }

    fun enableWirelessAdb(port: Int = 5555) {
        viewModelScope.launch {
            _uiState.update { it.copy(adbStatus = "开启中...") }
            val result = runCatching { AdbController.enableTcpIp(port) }.getOrNull()
            _uiState.update {
                it.copy(
                    lastResult = result?.let { r ->
                        "adb tcpip $port → exit=${r.exitCode} executor=${r.executor}\n${r.stdout.ifBlank { r.stderr }}".trim()
                    } ?: "开启无线 ADB 失败"
                )
            }
            refreshAdb()
        }
    }

    fun disableWirelessAdb() {
        viewModelScope.launch {
            _uiState.update { it.copy(adbStatus = "关闭中...") }
            val result = runCatching { AdbController.disableTcpIp() }.getOrNull()
            _uiState.update {
                it.copy(
                    lastResult = result?.let { r ->
                        "adb tcpip off → exit=${r.exitCode} executor=${r.executor}\n${r.stdout.ifBlank { r.stderr }}".trim()
                    } ?: "关闭无线 ADB 失败"
                )
            }
            refreshAdb()
        }
    }

    fun requestIgnoreBatteryOptimizations() {
        val context = getApplication<Application>()
        val granted = runCatching {
            val power = context.getSystemService(android.os.PowerManager::class.java)
            power?.isIgnoringBatteryOptimizations(context.packageName) ?: false
        }.getOrDefault(false)
        if (granted) {
            _uiState.update { it.copy(lastResult = "已在电池优化白名单中") }
            return
        }
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}"))
        direct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(direct) }.onFailure {
            val list = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            list.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(list) }
        }
        _uiState.update { it.copy(lastResult = "请在系统弹窗中允许后台运行，MCP 服务才能长期保活") }
    }

    // ── quick shell ─────────────────────────────────────────────────────────

    fun testCommand(command: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(lastResult = "执行中: $command") }
            val result = runCatching { PrivilegedShell.exec(command, 20_000L) }.getOrNull()
            _uiState.update {
                it.copy(
                    lastResult = result?.let { r ->
                        "[${r.executor} exit=${r.exitCode} ${r.durationMs}ms]\n${r.stdout.ifBlank { r.stderr }}".trim()
                    } ?: "执行失败"
                )
            }
        }
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private fun localIpv4(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp == true && it.isLoopback == false }
            .flatMap { it.inetAddresses.toList() }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress && it.isSiteLocalAddress }
            ?.hostAddress
    }.getOrNull()
}
