package com.mcpserver.mcp.router

import android.util.Log
import kotlinx.serialization.json.*
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "CommandRegistry"

// ─── Permission Levels ────────────────────────────────────────────────────────

/**
 * Permission levels for tool access control.
 * Higher levels encompass lower levels.
 */
enum class PermissionLevel(val value: Int) {
    READ_ONLY(1),    // Read-only operations (list, get, query)
    NORMAL(2),       // Standard operations (install, input)
    ELEVATED(3),     // Elevated operations (shell, write file)
    DANGEROUS(4);    // Dangerous operations (factory reset, delete all)

}

// ─── Tool Definition ──────────────────────────────────────────────────────────

/**
 * Defines an MCP tool with its metadata and handler.
 */
data class ToolDefinition(
    val name: String,
    val description: String,
    val inputSchema: JsonObject,
    val permissionLevel: PermissionLevel = PermissionLevel.NORMAL,
    val handler: suspend (JsonObject?) -> JsonObject,
)

// ─── Command Registry ─────────────────────────────────────────────────────────

/**
 * Command Registry
 * Manages available commands and their definitions.
 */
class CommandRegistry {

    private val commands = ConcurrentHashMap<String, ToolDefinition>()

    // ── Registration ────────────────────────────────────────────────────────

    /**
     * Register a tool definition.
     */
    fun register(tool: ToolDefinition) {
        commands[tool.name] = tool
        Log.d(TAG, "Registered tool: ${tool.name} (permission=${tool.permissionLevel})")
    }

    /**
     * Unregister a tool by name.
     */
    fun unregister(name: String) {
        commands.remove(name)
    }

    // ── Lookup ──────────────────────────────────────────────────────────────

    /**
     * Get a tool definition by name.
     */
    fun getTool(name: String): ToolDefinition? = commands[name]

    /**
     * Check if a tool is registered.
     */
    fun hasTool(name: String): Boolean = commands.containsKey(name)

    /**
     * Get all registered tools.
     */
    fun listTools(): List<ToolDefinition> = commands.values.toList()

    /**
     * Get command names.
     */
    fun getCommandNames(): List<String> = commands.keys.toList()

    // ── MCP tools/list Response ─────────────────────────────────────────────

    /**
     * Build the JSON response for the MCP tools/list method.
     */
    fun toToolsList(): List<JsonObject> {
        return commands.values.map { tool ->
            buildJsonObject {
                put("name", tool.name)
                put("description", tool.description)
                put("inputSchema", tool.inputSchema)
            }
        }
    }

    // ── Execution ───────────────────────────────────────────────────────────

    /**
     * Execute a tool by name with given arguments.
     */
    suspend fun execute(name: String, arguments: JsonObject?): JsonObject {
        val tool = commands[name]
            ?: throw IllegalArgumentException("Tool not found: $name")

        return try {
            tool.handler(arguments)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Tool execution error: $name", e)
            throw RuntimeException("Tool execution failed: ${e.message}")
        }
    }

    // ── Filtering ───────────────────────────────────────────────────────────

    /**
     * Filter tools by permission level.
     */
    fun toolsForPermission(level: PermissionLevel): List<ToolDefinition> =
        commands.values.filter { it.permissionLevel <= level }

    /**
     * Count of registered tools.
     */
    val toolCount: Int get() = commands.size

    // ── Register Built-in Tools ─────────────────────────────────────────────

    /**
     * Register all 20 built-in Android tools.
     */
    fun registerBuiltinTools() {
        registerShellCommand()
        registerInstallApp()
        registerListPackages()
        registerStartApp()
        registerStopApp()
        registerUninstallApp()
        registerScreenshot()
        registerInputText()
        registerTapScreen()
        registerSwipeScreen()
        registerGetDeviceInfo()
        registerListFiles()
        registerReadFile()
        registerWriteFile()
        registerGetBatteryInfo()
        registerGetWifiInfo()
        registerGetSettings()
        registerPutSettings()
        registerBroadcastIntent()
        registerStartActivity()
        Log.i(TAG, "Registered ${toolCount} built-in tools")
    }

    // ── Tool Implementations ────────────────────────────────────────────────

    private fun registerShellCommand() {
        register(ToolDefinition(
            name = "shell_command",
            description = "Execute a shell command on the Android device",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("command", buildJsonObject {
                        put("type", "string")
                        put("description", "The shell command to execute")
                    })
                })
                put("required", buildJsonArray { add("command") })
            },
            permissionLevel = PermissionLevel.ELEVATED,
            handler = { args ->
                val command = args?.get("command")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'command' parameter")
                buildJsonObject {
                    put("output", "Shell command received: $command")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerInstallApp() {
        register(ToolDefinition(
            name = "install_app",
            description = "Install an APK file on the device",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("apk_path", buildJsonObject {
                        put("type", "string")
                        put("description", "Path to the APK file")
                    })
                })
                put("required", buildJsonArray { add("apk_path") })
            },
            permissionLevel = PermissionLevel.DANGEROUS,
            handler = { args ->
                val path = args?.get("apk_path")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'apk_path' parameter")
                buildJsonObject {
                    put("output", "Install app from: $path")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerListPackages() {
        register(ToolDefinition(
            name = "list_packages",
            description = "List installed packages on the device",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("filter", buildJsonObject {
                        put("type", "string")
                        put("description", "Optional filter: 'system', 'third_party', or search string")
                    })
                })
            },
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val filter = args?.get("filter")?.jsonPrimitive?.contentOrNull
                buildJsonObject {
                    put("output", "List packages filter=$filter")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerStartApp() {
        register(ToolDefinition(
            name = "start_app",
            description = "Launch an application by package name",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("package_name", buildJsonObject {
                        put("type", "string")
                        put("description", "Package name of the app to launch")
                    })
                })
                put("required", buildJsonArray { add("package_name") })
            },
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val pkg = args?.get("package_name")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'package_name' parameter")
                buildJsonObject {
                    put("output", "Starting app: $pkg")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerStopApp() {
        register(ToolDefinition(
            name = "stop_app",
            description = "Force stop a running application",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("package_name", buildJsonObject {
                        put("type", "string")
                        put("description", "Package name of the app to stop")
                    })
                })
                put("required", buildJsonArray { add("package_name") })
            },
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val pkg = args?.get("package_name")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'package_name' parameter")
                buildJsonObject {
                    put("output", "Stopping app: $pkg")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerUninstallApp() {
        register(ToolDefinition(
            name = "uninstall_app",
            description = "Uninstall an application",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("package_name", buildJsonObject {
                        put("type", "string")
                        put("description", "Package name of the app to uninstall")
                    })
                })
                put("required", buildJsonArray { add("package_name") })
            },
            permissionLevel = PermissionLevel.DANGEROUS,
            handler = { args ->
                val pkg = args?.get("package_name")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'package_name' parameter")
                buildJsonObject {
                    put("output", "Uninstalling app: $pkg")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerScreenshot() {
        register(ToolDefinition(
            name = "screenshot",
            description = "Capture a screenshot of the current device screen",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("save_path", buildJsonObject {
                        put("type", "string")
                        put("description", "Path to save the screenshot (optional)")
                    })
                })
            },
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val path = args?.get("save_path")?.jsonPrimitive?.contentOrNull ?: "/sdcard/screenshot.png"
                buildJsonObject {
                    put("output", "Screenshot saved to: $path")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerInputText() {
        register(ToolDefinition(
            name = "input_text",
            description = "Type text into the currently focused input field",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("text", buildJsonObject {
                        put("type", "string")
                        put("description", "The text to type")
                    })
                })
                put("required", buildJsonArray { add("text") })
            },
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val text = args?.get("text")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'text' parameter")
                buildJsonObject {
                    put("output", "Input text: $text")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerTapScreen() {
        register(ToolDefinition(
            name = "tap_screen",
            description = "Tap at specific coordinates on the screen",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("x", buildJsonObject {
                        put("type", "integer")
                        put("description", "X coordinate")
                    })
                    put("y", buildJsonObject {
                        put("type", "integer")
                        put("description", "Y coordinate")
                    })
                })
                put("required", buildJsonArray { add("x"); add("y") })
            },
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val x = args?.get("x")?.jsonPrimitive?.intOrNull
                    ?: throw IllegalArgumentException("Missing 'x' parameter")
                val y = args?.get("y")?.jsonPrimitive?.intOrNull
                    ?: throw IllegalArgumentException("Missing 'y' parameter")
                buildJsonObject {
                    put("output", "Tap at ($x, $y)")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerSwipeScreen() {
        register(ToolDefinition(
            name = "swipe_screen",
            description = "Swipe between two points on the screen",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("x1", buildJsonObject { put("type", "integer"); put("description", "Start X") })
                    put("y1", buildJsonObject { put("type", "integer"); put("description", "Start Y") })
                    put("x2", buildJsonObject { put("type", "integer"); put("description", "End X") })
                    put("y2", buildJsonObject { put("type", "integer"); put("description", "End Y") })
                    put("duration_ms", buildJsonObject {
                        put("type", "integer")
                        put("description", "Duration in milliseconds (default: 300)")
                    })
                })
                put("required", buildJsonArray { add("x1"); add("y1"); add("x2"); add("y2") })
            },
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val x1 = args?.get("x1")?.jsonPrimitive?.intOrNull ?: throw IllegalArgumentException("Missing 'x1'")
                val y1 = args?.get("y1")?.jsonPrimitive?.intOrNull ?: throw IllegalArgumentException("Missing 'y1'")
                val x2 = args?.get("x2")?.jsonPrimitive?.intOrNull ?: throw IllegalArgumentException("Missing 'x2'")
                val y2 = args?.get("y2")?.jsonPrimitive?.intOrNull ?: throw IllegalArgumentException("Missing 'y2'")
                val duration = args?.get("duration_ms")?.jsonPrimitive?.intOrNull ?: 300
                buildJsonObject {
                    put("output", "Swipe from ($x1,$y1) to ($x2,$y2) in ${duration}ms")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerGetDeviceInfo() {
        register(ToolDefinition(
            name = "get_device_info",
            description = "Get device information (model, Android version, screen size, etc.)",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {})
            },
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { _ ->
                buildJsonObject {
                    put("output", "Device info placeholder")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerListFiles() {
        register(ToolDefinition(
            name = "list_files",
            description = "List files in a directory on the device",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("path", buildJsonObject {
                        put("type", "string")
                        put("description", "Directory path to list")
                    })
                    put("recursive", buildJsonObject {
                        put("type", "boolean")
                        put("description", "List recursively (default: false)")
                    })
                })
                put("required", buildJsonArray { add("path") })
            },
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val path = args?.get("path")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'path' parameter")
                val recursive = args?.get("recursive")?.jsonPrimitive?.booleanOrNull ?: false
                buildJsonObject {
                    put("output", "List files in $path (recursive=$recursive)")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerReadFile() {
        register(ToolDefinition(
            name = "read_file",
            description = "Read the contents of a file on the device",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("path", buildJsonObject {
                        put("type", "string")
                        put("description", "File path to read")
                    })
                    put("max_bytes", buildJsonObject {
                        put("type", "integer")
                        put("description", "Maximum bytes to read (default: 1MB)")
                    })
                })
                put("required", buildJsonArray { add("path") })
            },
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { args ->
                val path = args?.get("path")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'path' parameter")
                val maxBytes = args?.get("max_bytes")?.jsonPrimitive?.intOrNull ?: 1_048_576
                buildJsonObject {
                    put("output", "Read file: $path (maxBytes=$maxBytes)")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerWriteFile() {
        register(ToolDefinition(
            name = "write_file",
            description = "Write content to a file on the device",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("path", buildJsonObject {
                        put("type", "string")
                        put("description", "File path to write")
                    })
                    put("content", buildJsonObject {
                        put("type", "string")
                        put("description", "Content to write")
                    })
                })
                put("required", buildJsonArray { add("path"); add("content") })
            },
            permissionLevel = PermissionLevel.DANGEROUS,
            handler = { args ->
                val path = args?.get("path")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'path' parameter")
                val content = args?.get("content")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'content' parameter")
                buildJsonObject {
                    put("output", "Write file: $path (${content.length} chars)")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerGetBatteryInfo() {
        register(ToolDefinition(
            name = "get_battery_info",
            description = "Get battery status and level",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {})
            },
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { _ ->
                buildJsonObject {
                    put("output", "Battery info placeholder")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerGetWifiInfo() {
        register(ToolDefinition(
            name = "get_wifi_info",
            description = "Get current WiFi connection information",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {})
            },
            permissionLevel = PermissionLevel.READ_ONLY,
            handler = { _ ->
                buildJsonObject {
                    put("output", "WiFi info placeholder")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerGetSettings() {
        register(ToolDefinition(
            name = "get_settings",
            description = "Read a system setting value",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("namespace", buildJsonObject {
                        put("type", "string")
                        put("description", "Settings namespace (e.g., 'system', 'secure', 'global')")
                    })
                    put("key", buildJsonObject {
                        put("type", "string")
                        put("description", "Setting key name")
                    })
                })
                put("required", buildJsonArray { add("namespace"); add("key") })
            },
            permissionLevel = PermissionLevel.ELEVATED,
            handler = { args ->
                val namespace = args?.get("namespace")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'namespace' parameter")
                val key = args?.get("key")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'key' parameter")
                buildJsonObject {
                    put("output", "Get settings: $namespace/$key")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerPutSettings() {
        register(ToolDefinition(
            name = "put_settings",
            description = "Write a system setting value",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("namespace", buildJsonObject {
                        put("type", "string")
                        put("description", "Settings namespace (e.g., 'system', 'secure', 'global')")
                    })
                    put("key", buildJsonObject {
                        put("type", "string")
                        put("description", "Setting key name")
                    })
                    put("value", buildJsonObject {
                        put("type", "string")
                        put("description", "Setting value")
                    })
                })
                put("required", buildJsonArray { add("namespace"); add("key"); add("value") })
            },
            permissionLevel = PermissionLevel.DANGEROUS,
            handler = { args ->
                val namespace = args?.get("namespace")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'namespace' parameter")
                val key = args?.get("key")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'key' parameter")
                val value = args?.get("value")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'value' parameter")
                buildJsonObject {
                    put("output", "Put settings: $namespace/$key = $value")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerBroadcastIntent() {
        register(ToolDefinition(
            name = "broadcast_intent",
            description = "Send an Android broadcast intent",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("action", buildJsonObject {
                        put("type", "string")
                        put("description", "Broadcast action")
                    })
                    put("extras", buildJsonObject {
                        put("type", "object")
                        put("description", "Extra key-value pairs")
                    })
                })
                put("required", buildJsonArray { add("action") })
            },
            permissionLevel = PermissionLevel.ELEVATED,
            handler = { args ->
                val action = args?.get("action")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'action' parameter")
                val extras = args?.get("extras")?.jsonObject
                buildJsonObject {
                    put("output", "Broadcast intent: $action (extras=$extras)")
                    put("success", true)
                }
            }
        ))
    }

    private fun registerStartActivity() {
        register(ToolDefinition(
            name = "start_activity",
            description = "Start an Android Activity",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("package_name", buildJsonObject {
                        put("type", "string")
                        put("description", "Package name")
                    })
                    put("activity_name", buildJsonObject {
                        put("type", "string")
                        put("description", "Activity class name (optional, launches main if omitted)")
                    })
                })
                put("required", buildJsonArray { add("package_name") })
            },
            permissionLevel = PermissionLevel.NORMAL,
            handler = { args ->
                val pkg = args?.get("package_name")?.jsonPrimitive?.contentOrNull
                    ?: throw IllegalArgumentException("Missing 'package_name' parameter")
                val activity = args?.get("activity_name")?.jsonPrimitive?.contentOrNull
                buildJsonObject {
                    put("output", "Start activity: $pkg/${activity ?: "main"}")
                    put("success", true)
                }
            }
        ))
    }
}
