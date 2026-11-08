package com.mcpserver.platform.command

/**
 * Command Executor Interface
 * Abstracts command execution across different platforms (Shizuku, ADB)
 */
interface CommandExecutor {
    /**
     * Execute a command
     * @param command The command to execute
     * @param workingDirectory Optional working directory
     * @return Command result
     */
    suspend fun execute(
        command: String,
        workingDirectory: String? = null
    ): CommandResult

    /**
     * Check if the executor is available
     * @return true if the executor can execute commands
     */
    suspend fun isAvailable(): Boolean

    /**
     * Get executor name
     */
    fun getExecutorName(): String
}
