package com.mcpserver.mcp.router

import android.util.Log
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "RouteEngine"

// ─── Route Handler Interface ──────────────────────────────────────────────────

/**
 * Route Handler Interface
 * Each handler processes a specific MCP method.
 */
interface RouteHandler {
    /**
     * Handle an MCP request.
     * @param method The MCP method name
     * @param params The request parameters
     * @return The result as a JsonObject
     */
    suspend fun handle(method: String, params: JsonObject?): JsonObject
}

// ─── Route Engine ─────────────────────────────────────────────────────────────

/**
 * Route Engine
 * Routes incoming MCP requests to appropriate tool handlers.
 */
class RouteEngine(
    private val commandRegistry: CommandRegistry,
) {

    private val handlers = ConcurrentHashMap<String, RouteHandler>()

    // ── Registration ────────────────────────────────────────────────────────

    /**
     * Register a route handler for a specific method.
     */
    fun register(method: String, handler: RouteHandler) {
        handlers[method] = handler
    }

    /**
     * Register a handler for multiple methods.
     */
    fun register(methods: List<String>, handler: RouteHandler) {
        methods.forEach { register(it, handler) }
    }

    /**
     * Unregister a handler.
     */
    fun unregister(method: String) {
        handlers.remove(method)
    }

    // ── Tool Execution ──────────────────────────────────────────────────────

    /**
     * Execute a tool by name with given arguments.
     * This is the main entry point for MCP tools/call.
     */
    suspend fun executeTool(toolName: String, arguments: JsonObject?): JsonObject {
        if (!commandRegistry.hasTool(toolName)) {
            throw IllegalArgumentException("Tool not found: $toolName")
        }

        return commandRegistry.execute(toolName, arguments)
    }

    // ── Request Routing ─────────────────────────────────────────────────────

    /**
     * Route a request to its handler.
     * @param method The MCP method name
     * @param params The request parameters
     * @return The result as a JsonObject
     */
    suspend fun route(method: String, params: JsonObject?): JsonObject {
        val handler = handlers[method]
            ?: throw IllegalArgumentException("Method not found: $method")

        return try {
            handler.handle(method, params)
        } catch (e: Exception) {
            Log.e(TAG, "Route error for $method", e)
            throw e
        }
    }

    /**
     * Check if a method has a registered handler.
     */
    fun hasHandler(method: String): Boolean = handlers.containsKey(method)

    /**
     * Get list of all registered methods.
     */
    fun getRegisteredMethods(): List<String> = handlers.keys.toList()

    /**
     * Clear all handlers.
     */
    fun clear() {
        handlers.clear()
    }
}
