package com.mcpserver.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * MCP JSON-RPC Response
 */
@Serializable
data class McpResponse(
    val jsonrpc: String = "2.0",
    val id: Long? = null,
    val result: JsonElement? = null,
    val error: McpError? = null
) {
    companion object {
        fun success(id: Long?, result: JsonElement): McpResponse {
            return McpResponse(id = id, result = result)
        }

        fun error(id: Long?, code: Int, message: String, data: JsonElement? = null): McpResponse {
            return McpResponse(
                id = id,
                error = McpError(code = code, message = message, data = data)
            )
        }
    }
}

/**
 * JSON-RPC Error Object
 */
@Serializable
data class McpError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
) {
    companion object {
        // Standard JSON-RPC Error Codes
        const val PARSE_ERROR = -32700
        const val INVALID_REQUEST = -32600
        const val METHOD_NOT_FOUND = -32601
        const val INVALID_PARAMS = -32602
        const val INTERNAL_ERROR = -32603

        // MCP-specific Error Codes
        const val TOOL_NOT_FOUND = -32001
        const val RESOURCE_NOT_FOUND = -32002
        const val PERMISSION_DENIED = -32003
        const val EXECUTION_FAILED = -32004
    }
}

/**
 * MCP Tool Result
 */
@Serializable
data class ToolResult(
    val content: List<ToolContent>,
    val isError: Boolean = false
)

@Serializable
data class ToolContent(
    val type: String = "text",
    val text: String? = null,
    val mimeType: String? = null
)

/**
 * MCP Resource Content
 */
@Serializable
data class ResourceContent(
    val uri: String,
    val mimeType: String? = null,
    val text: String? = null
)
