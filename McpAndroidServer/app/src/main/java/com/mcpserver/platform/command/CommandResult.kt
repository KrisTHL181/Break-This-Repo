package com.mcpserver.platform.command

import kotlinx.serialization.Serializable

/**
 * Command Execution Result
 */
@Serializable
data class CommandResult(
    val command: String,
    val output: String,
    val error: String,
    val exitCode: Int,
    val executionTimeMs: Long,
    val executor: String
) {
    val isSuccess: Boolean get() = exitCode == 0

    companion object {
        fun success(
            command: String,
            output: String,
            executionTimeMs: Long,
            executor: String
        ): CommandResult {
            return CommandResult(
                command = command,
                output = output,
                error = "",
                exitCode = 0,
                executionTimeMs = executionTimeMs,
                executor = executor
            )
        }

        fun error(
            command: String,
            output: String,
            error: String,
            exitCode: Int,
            executionTimeMs: Long,
            executor: String
        ): CommandResult {
            return CommandResult(
                command = command,
                output = output,
                error = error,
                exitCode = exitCode,
                executionTimeMs = executionTimeMs,
                executor = executor
            )
        }
    }
}
