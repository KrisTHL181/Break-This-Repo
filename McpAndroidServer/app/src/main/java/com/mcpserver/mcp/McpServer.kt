package com.mcpserver.mcp

import android.content.Context
import android.util.Log
import com.mcpserver.mcp.auth.AuthManager
import com.mcpserver.mcp.formatter.ResultFormatter
import com.mcpserver.mcp.protocol.Transport
import com.mcpserver.mcp.protocol.SseTransport
import com.mcpserver.mcp.protocol.StdioTransport
import com.mcpserver.data.SettingsStore
import com.mcpserver.mcp.router.CommandRegistry
import com.mcpserver.mcp.router.RouteEngine
import com.mcpserver.mcp.router.registerCoreHandlers
import com.mcpserver.mcp.router.registerDeviceTools
import com.mcpserver.mcp.router.registerExtendedTools
import com.mcpserver.model.AppSettings
import com.mcpserver.model.McpError
import com.mcpserver.model.McpResponse
import com.mcpserver.model.TransportType
import com.mcpserver.platform.shizuku.ShizukuManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

class McpServer(private val context: Context, private val shizukuManager: ShizukuManager) {
    companion object { private const val TAG = "McpServer" }

    private val commandRegistry = CommandRegistry()
    private val routeEngine = RouteEngine(commandRegistry)
    private val authManager = AuthManager(context)
    private val resultFormatter = ResultFormatter()
    private var transport: Transport = StdioTransport()
    private val _state = MutableStateFlow(McpServerState())
    val state: StateFlow<McpServerState> = _state.asStateFlow()
    private val serverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var receiveJob: Job? = null

    /** Settings the Settings screen persists. */
    val settingsStore = SettingsStore(context)

    val auth: AuthManager get() = authManager

    init {
        commandRegistry.registerBuiltinTools()
        commandRegistry.registerExtendedTools(context, shizukuManager)
        commandRegistry.registerDeviceTools(context)
        routeEngine.registerCoreHandlers(commandRegistry)
    }

    suspend fun start(settings: AppSettings): Boolean {
        return try {
            _state.value = _state.value.copy(settings = settings)
            transport = createTransport(settings)
            withContext(Dispatchers.IO) { transport.connect() }
            startReceiving()
            _state.value = _state.value.copy(
                isRunning = true,
                status = "Running",
                transportType = transport.getTransportType(),
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start", e)
            _state.value = _state.value.copy(isRunning = false, status = "Error: ${e.message}")
            false
        }
    }

    suspend fun stop() {
        receiveJob?.cancel()
        transport.disconnect()
        _state.value = _state.value.copy(isRunning = false, status = "Stopped")
    }

    suspend fun processRequest(requestJson: String): String {
        return try {
            val json = Json.parseToJsonElement(requestJson) as? JsonObject
                ?: return errorResponse(null, McpError.PARSE_ERROR, "Invalid JSON")
            val method = json["method"]?.jsonPrimitive?.content
                ?: return errorResponse(null, McpError.INVALID_REQUEST, "Missing method")
            val idElement = json["id"]
            val params = json["params"] as? JsonObject
            if (idElement == null || idElement is JsonNull) {
                // JSON-RPC notification: run it, but send no response.
                runCatching { routeEngine.route(method, params) }
                return ""
            }
            val result = routeEngine.route(method, params)
            buildJsonObject {
                put("jsonrpc", "2.0")
                put("id", idElement)
                put("result", result)
            }.toString()
        } catch (e: Exception) {
            errorResponse(null, McpError.INTERNAL_ERROR, e.message ?: "Unknown error")
        }
    }

    private fun createTransport(settings: AppSettings): Transport = when (settings.transportType) {
        TransportType.SSE -> SseTransport(
            port = settings.serverPort,
            host = "0.0.0.0",
            authValidator = { token ->
                !settings.authEnabled || settings.authToken.isBlank() || token == settings.authToken
            },
            onRequest = { body -> processRequest(body) },
        )
        TransportType.STDIO -> StdioTransport()
    }

    private fun startReceiving() {
        receiveJob = serverScope.launch {
            transport.receive().collect { message ->
                try {
                    val response = processRequest(message)
                    if (response.isNotBlank()) transport.send(response)
                } catch (e: Exception) { Log.e(TAG, "Failed to handle message", e) }
            }
        }
    }

    private fun errorResponse(id: Long?, code: Int, message: String): String {
        return buildJsonObject {
            put("jsonrpc", "2.0")
            if (id != null) put("id", id)
            put(
                "error",
                buildJsonObject {
                    put("code", code)
                    put("message", message)
                },
            )
        }.toString()
    }
}

data class McpServerState(
    val isRunning: Boolean = false,
    val status: String = "Stopped",
    val transportType: String = "stdio",
    val error: String? = null,
    val settings: AppSettings = AppSettings()
)
