package com.mcpserver.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * MCP JSON-RPC Request
 */
@Serializable
data class McpRequest(
    val jsonrpc: String = "2.0",
    val id: Long? = null,
    val method: String,
    val params: JsonElement? = null
) {
    val isNotification: Boolean get() = id == null
}

/**
 * MCP Tool Call Request
 */
@Serializable
data class ToolCallRequest(
    val name: String,
    val arguments: Map<String, JsonElement> = emptyMap()
)

/**
 * MCP Resource Request
 */
@Serializable
data class ResourceRequest(
    val uri: String
)
