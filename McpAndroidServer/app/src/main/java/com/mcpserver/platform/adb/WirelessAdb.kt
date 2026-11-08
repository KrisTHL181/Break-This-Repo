package com.mcpserver.platform.adb

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket

class WirelessAdb(private val scope: CoroutineScope) {
    
    companion object {
        private const val TAG = "WirelessAdb"
        private const val DEFAULT_ADB_PORT = 5555
        private const val CONNECTION_TIMEOUT_MS = 5000L
        private const val RECONNECT_INTERVAL_MS = 10000L
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }
    
    sealed class ConnectionState {
        object Disconnected : ConnectionState()
        object TcpIpMode : ConnectionState()
        object Pairing : ConnectionState()
        object Connected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }
    
    private val _state = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()
    
    private var connectionJob: Job? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0
    
    private var host: String? = null
    private var port: Int = DEFAULT_ADB_PORT
    private var pairCode: String? = null
    
    suspend fun enableTcpIpMode(port: Int = DEFAULT_ADB_PORT): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("adb", "tcpip", port.toString()))
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    this@WirelessAdb.port = port
                    _state.value = ConnectionState.TcpIpMode
                    Log.d(TAG, "TCP/IP mode enabled on port $port")
                    return@withContext true
                } else {
                    val error = process.errorStream.bufferedReader().readText()
                    Log.e(TAG, "Failed to enable TCP/IP mode: $error")
                    _state.value = ConnectionState.Error("Failed to enable TCP/IP mode: $error")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable TCP/IP mode", e)
                _state.value = ConnectionState.Error(e.message ?: "Failed to enable TCP/IP mode")
                return@withContext false
            }
        }
    }
    
    suspend fun pair(host: String, port: Int, code: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                this@WirelessAdb.host = host
                this@WirelessAdb.port = port
                this@WirelessAdb.pairCode = code
                
                _state.value = ConnectionState.Pairing
                
                val process = Runtime.getRuntime().exec(arrayOf("adb", "pair", "$host:$port", code))
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    Log.d(TAG, "Successfully paired with $host:$port")
                    return@withContext true
                } else {
                    val error = process.errorStream.bufferedReader().readText()
                    Log.e(TAG, "Pairing failed: $error")
                    _state.value = ConnectionState.Error("Pairing failed: $error")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to pair", e)
                _state.value = ConnectionState.Error(e.message ?: "Failed to pair")
                return@withContext false
            }
        }
    }
    
    suspend fun connect(host: String, port: Int = DEFAULT_ADB_PORT): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                this@WirelessAdb.host = host
                this@WirelessAdb.port = port
                
                // 检查端口是否可访问
                if (!isPortAccessible(host, port)) {
                    _state.value = ConnectionState.Error("Port $port on $host is not accessible")
                    return@withContext false
                }
                
                val process = Runtime.getRuntime().exec(arrayOf("adb", "connect", "$host:$port"))
                val exitCode = process.waitFor()
                
                if (exitCode == 0) {
                    val output = process.inputStream.bufferedReader().readText()
                    if (output.contains("connected")) {
                        _state.value = ConnectionState.Connected
                        Log.d(TAG, "Successfully connected to $host:$port")
                        
                        // 启动重连监控
                        startReconnectMonitor()
                        
                        return@withContext true
                    } else {
                        _state.value = ConnectionState.Error("Connection failed: $output")
                        return@withContext false
                    }
                } else {
                    val error = process.errorStream.bufferedReader().readText()
                    Log.e(TAG, "Connection failed: $error")
                    _state.value = ConnectionState.Error("Connection failed: $error")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to connect", e)
                _state.value = ConnectionState.Error(e.message ?: "Failed to connect")
                return@withContext false
            }
        }
    }
    
    private fun isPortAccessible(host: String, port: Int): Boolean {
        return try {
            Socket(host, port).use { true }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun startReconnectMonitor() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            while (isActive) {
                delay(RECONNECT_INTERVAL_MS)
                
                if (!isConnected()) {
                    Log.w(TAG, "Connection lost, attempting to reconnect")
                    reconnectAttempts++
                    
                    if (reconnectAttempts > MAX_RECONNECT_ATTEMPTS) {
                        Log.e(TAG, "Max reconnect attempts reached")
                        _state.value = ConnectionState.Error("Max reconnect attempts reached")
                        break
                    }
                    
                    host?.let { currentHost ->
                        if (connect(currentHost, port)) {
                            reconnectAttempts = 0
                        }
                    }
                } else {
                    reconnectAttempts = 0
                }
            }
        }
    }
    
    private fun stopReconnectMonitor() {
        reconnectJob?.cancel()
        reconnectJob = null
    }
    
    suspend fun disconnect(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                stopReconnectMonitor()
                
                host?.let { currentHost ->
                    val process = Runtime.getRuntime().exec(arrayOf("adb", "disconnect", "$currentHost:$port"))
                    val exitCode = process.waitFor()
                    
                    if (exitCode == 0) {
                        _state.value = ConnectionState.Disconnected
                        Log.d(TAG, "Disconnected from $currentHost:$port")
                        return@withContext true
                    } else {
                        val error = process.errorStream.bufferedReader().readText()
                        Log.e(TAG, "Disconnect failed: $error")
                        return@withContext false
                    }
                }
                
                _state.value = ConnectionState.Disconnected
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect", e)
                return@withContext false
            }
        }
    }
    
    fun isConnected(): Boolean {
        return _state.value == ConnectionState.Connected
    }
    
    suspend fun executeCommand(command: String): WirelessAdbCommandResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!isConnected()) {
                    return@withContext WirelessAdbCommandResult(
                        exitCode = -1,
                        stdout = "",
                        stderr = "Not connected to device",
                        executionTime = 0,
                        isTimeout = false
                    )
                }
                
                val startTime = System.currentTimeMillis()
                val process = Runtime.getRuntime().exec(arrayOf("adb", "-s", "${host}:${port}", "shell", command))
                
                val stdout = process.inputStream.bufferedReader().readText()
                val stderr = process.errorStream.bufferedReader().readText()
                
                val exitCode = try {
                    process.waitFor()
                } catch (e: Exception) {
                    process.destroyForcibly()
                    -1
                }
                
                val executionTime = System.currentTimeMillis() - startTime
                
                WirelessAdbCommandResult(
                    exitCode = exitCode,
                    stdout = stdout,
                    stderr = stderr,
                    executionTime = executionTime,
                    isTimeout = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute command via wireless ADB", e)
                WirelessAdbCommandResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = e.message ?: "Command execution failed",
                    executionTime = 0,
                    isTimeout = false
                )
            }
        }
    }
    
    fun getDeviceInfo(): Map<String, String> {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("adb", "devices", "-l"))
            val output = process.inputStream.bufferedReader().readText()
            val lines = output.lines().filter { it.contains(":") && !it.startsWith("List") }
            
            val deviceInfo = mutableMapOf<String, String>()
            for (line in lines) {
                val parts = line.split("\\s+".toRegex())
                if (parts.size >= 2) {
                    deviceInfo["device_id"] = parts[0]
                    deviceInfo["status"] = parts[1]
                    
                    // 提取更多设备信息
                    for (part in parts) {
                        when {
                            part.startsWith("model:") -> deviceInfo["model"] = part.substringAfter(":")
                            part.startsWith("device:") -> deviceInfo["device"] = part.substringAfter(":")
                            part.startsWith("product:") -> deviceInfo["product"] = part.substringAfter(":")
                            part.startsWith("transport_id:") -> deviceInfo["transport_id"] = part.substringAfter(":")
                        }
                    }
                }
            }
            
            deviceInfo
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get device info", e)
            emptyMap()
        }
    }
    
    fun destroy() {
        stopReconnectMonitor()
        scope.launch {
            disconnect()
        }
    }
    
    data class WirelessAdbCommandResult(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
        val executionTime: Long,
        val isTimeout: Boolean
    )
}