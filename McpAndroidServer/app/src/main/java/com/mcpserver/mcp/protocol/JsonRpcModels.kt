package com.mcpserver.mcp.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

// ─── JSON-RPC 2.0 Message Models ──────────────────────────────────────────────

@Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val method: String,
    val params: JsonObject? = null
)

@Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val result: JsonObject? = null,
    val error: JsonRpcError? = null
)

@Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonObject? = null
)

// ─── MCP-Specific Error Codes ─────────────────────────────────────────────────

object McpErrorCodes {
    // JSON-RPC standard errors
    const val PARSE_ERROR = -32700
    const val INVALID_REQUEST = -32600
    const val METHOD_NOT_FOUND = -32601
    const val INVALID_PARAMS = -32602
    const val INTERNAL_ERROR = -32603

    // MCP custom errors
    const val TOOL_NOT_FOUND = -32001
    const val TOOL_EXECUTION_ERROR = -32002
    const val AUTH_FAILED = -32003
    const val PERMISSION_DENIED = -32004
}

// ─── Convenience Builders ─────────────────────────────────────────────────────

fun successResponse(id: Int?, result: JsonObject): JsonRpcResponse {
    return JsonRpcResponse(id = id, result = result)
}

fun errorResponse(id: Int?, code: Int, message: String, data: JsonObject? = null): JsonRpcResponse {
    return JsonRpcResponse(
        id = id,
        error = JsonRpcError(code = code, message = message, data = data)
    )
}

fun parseError(id: Int? = null): JsonRpcResponse {
    return errorResponse(id, McpErrorCodes.PARSE_ERROR, "Parse error")
}

fun invalidRequest(id: Int? = null, detail: String = "Invalid request"): JsonRpcResponse {
    return errorResponse(id, McpErrorCodes.INVALID_REQUEST, detail)
}

fun methodNotFound(id: Int?, method: String): JsonRpcResponse {
    return errorResponse(id, McpErrorCodes.METHOD_NOT_FOUND, "Method not found: $method")
}

fun invalidParams(id: Int? = null, detail: String): JsonRpcResponse {
    return errorResponse(id, McpErrorCodes.INVALID_PARAMS, detail)
}

fun internalError(id: Int? = null, detail: String): JsonRpcResponse {
    return errorResponse(id, McpErrorCodes.INTERNAL_ERROR, detail)
}
