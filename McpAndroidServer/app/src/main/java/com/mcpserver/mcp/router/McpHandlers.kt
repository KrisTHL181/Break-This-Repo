package com.mcpserver.mcp.router

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Registers the MCP protocol methods.
 *
 * Without these the server answers every request with "Method not found", so no
 * client can even list the tools.
 */
fun RouteEngine.registerCoreHandlers(
    registry: CommandRegistry,
    serverName: String = "mcp-android-server",
    serverVersion: String = "1.1.0",
) {
    register("initialize", object : RouteHandler {
        override suspend fun handle(method: String, params: JsonObject?): JsonObject {
            val requested = (params?.get("protocolVersion") as? JsonPrimitive)?.contentOrNull
            return buildJsonObject {
                put("protocolVersion", requested ?: "2025-06-18")
                put("capabilities", buildJsonObject {
                    put("tools", buildJsonObject { put("listChanged", false) })
                    put("resources", buildJsonObject { put("listChanged", false) })
                    put("logging", buildJsonObject { })
                })
                put("serverInfo", buildJsonObject {
                    put("name", serverName)
                    put("version", serverVersion)
                })
                put(
                    "instructions",
                    "Android device control server. Use shell_command for shell access, " +
                        "web_fetch/web_search for the web, browser_* for a headless WebView " +
                        "(browser_open -> browser_content / browser_eval / browser_screenshot) " +
                        "and shizuku_status to check elevated-shell availability."
                )
            }
        }
    })

    register("ping", object : RouteHandler {
        override suspend fun handle(method: String, params: JsonObject?): JsonObject = buildJsonObject { }
    })

    register("notifications/initialized", object : RouteHandler {
        override suspend fun handle(method: String, params: JsonObject?): JsonObject = buildJsonObject { }
    })

    register("logging/setLevel", object : RouteHandler {
        override suspend fun handle(method: String, params: JsonObject?): JsonObject = buildJsonObject { }
    })

    register("tools/list", object : RouteHandler {
        override suspend fun handle(method: String, params: JsonObject?): JsonObject = buildJsonObject {
            put("tools", buildJsonArray { registry.toToolsList().forEach { add(it) } })
        }
    })

    register("tools/call", object : RouteHandler {
        override suspend fun handle(method: String, params: JsonObject?): JsonObject {
            val name = (params?.get("name") as? JsonPrimitive)?.contentOrNull
                ?: throw IllegalArgumentException("Missing 'name' parameter")
            val arguments = params["arguments"] as? JsonObject
            return try {
                val result = registry.execute(name, arguments)
                buildJsonObject {
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", Json.encodeToString(JsonObject.serializer(), result))
                        })
                    })
                    put("isError", false)
                }
            } catch (t: Throwable) {
                buildJsonObject {
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", t.message ?: t.javaClass.simpleName)
                        })
                    })
                    put("isError", true)
                }
            }
        }
    })

    register("resources/list", object : RouteHandler {
        override suspend fun handle(method: String, params: JsonObject?): JsonObject = buildJsonObject {
            put("resources", buildJsonArray { })
        }
    })

    register("prompts/list", object : RouteHandler {
        override suspend fun handle(method: String, params: JsonObject?): JsonObject = buildJsonObject {
            put("prompts", buildJsonArray { })
        }
    })
}
