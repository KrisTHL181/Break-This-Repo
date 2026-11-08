package com.mcpserver.mcp.protocol

import android.util.Log
import kotlinx.serialization.json.*

private const val TAG = "McpProtocol"

class McpProtocol {
    private val serverInfo = mapOf("name" to "mcp-android-server", "version" to "1.0.0")
    private val capabilities = mapOf("tools" to mapOf("listChanged" to true))

    fun handleInitialize(params: JsonObject?): JsonObject {
        return buildJsonObject {
            put("protocolVersion", "2024-11-05")
            put("serverInfo", buildJsonObject { serverInfo.forEach { (k, v) -> put(k, v) } })
            put("capabilities", buildJsonObject { capabilities.forEach { (k, v) -> put(k, JsonPrimitive(true)) } })
        }
    }

    fun handlePing(): JsonObject = buildJsonObject {}

    fun createTextContent(text: String): JsonPrimitive = JsonPrimitive(text)
}
