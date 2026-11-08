package com.mcpserver.mcp.protocol

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * JSON-RPC 2.0 Message Parser
 * Handles parsing and serialization of JSON-RPC messages
 */
object JsonRpcMessage {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /**
     * Parse a JSON string into a JsonElement
     */
    fun parse(jsonString: String): JsonElement {
        return json.parseToJsonElement(jsonString)
    }

    /**
     * Parse a JSON string into a JsonObject
     */
    fun parseObject(jsonString: String): JsonObject {
        return json.parseToJsonElement(jsonString) as? JsonObject
            ?: throw IllegalArgumentException("Not a JSON object")
    }

    /**
     * Serialize a JsonElement to a JSON string
     */
    fun serialize(element: JsonElement): String {
        return json.encodeToString(element)
    }

    /**
     * Create a JSON-RPC request
     */
    fun createRequest(method: String, params: JsonElement? = null, id: Long = 1): JsonObject {
        return buildJsonObject {
            put("jsonrpc", "2.0")
            put("id", id)
            put("method", method)
            params?.let { put("params", it) }
        }
    }

    /**
     * Create a JSON-RPC response
     */
    fun createResponse(id: Long?, result: JsonElement? = null, error: JsonObject? = null): JsonObject {
        return buildJsonObject {
            put("jsonrpc", "2.0")
            id?.let { put("id", it) }
            result?.let { put("result", it) }
            error?.let { put("error", it) }
        }
    }

    /**
     * Create a JSON-RPC error object
     */
    fun createError(code: Int, message: String, data: JsonElement? = null): JsonObject {
        return buildJsonObject {
            put("code", code)
            put("message", message)
            data?.let { put("data", it) }
        }
    }
}
