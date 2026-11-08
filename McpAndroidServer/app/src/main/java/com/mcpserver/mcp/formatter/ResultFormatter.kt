package com.mcpserver.mcp.formatter

import com.mcpserver.platform.command.CommandResult
import kotlinx.serialization.json.*

/**
 * Result Formatter
 * Formats command results for MCP protocol responses
 */
class ResultFormatter {

    /**
     * Format a command result to MCP tool result content
     */
    fun formatResult(result: CommandResult): List<JsonObject> {
        return when {
            result.isSuccess -> formatSuccess(result)
            else -> formatError(result)
        }
    }

    private fun formatSuccess(result: CommandResult): List<JsonObject> {
        val content = mutableListOf<JsonObject>()

        if (result.output.isNotBlank()) {
            content.add(buildJsonObject {
                put("type", "text")
                put("text", result.output)
            })
        }

        if (result.exitCode != 0 || result.executionTimeMs > 0) {
            content.add(buildJsonObject {
                put("type", "text")
                put("text", "Exit code: ${result.exitCode} | Time: ${result.executionTimeMs}ms")
            })
        }

        if (content.isEmpty()) {
            content.add(buildJsonObject {
                put("type", "text")
                put("text", "Command executed successfully")
            })
        }

        return content
    }

    private fun formatError(result: CommandResult): List<JsonObject> {
        val content = mutableListOf<JsonObject>()

        if (result.error.isNotBlank()) {
            content.add(buildJsonObject {
                put("type", "text")
                put("text", "Error: ${result.error}")
            })
        }

        if (result.output.isNotBlank()) {
            content.add(buildJsonObject {
                put("type", "text")
                put("text", result.output)
            })
        }

        content.add(buildJsonObject {
            put("type", "text")
            put("text", "Exit code: ${result.exitCode}")
        })

        return content
    }
}
