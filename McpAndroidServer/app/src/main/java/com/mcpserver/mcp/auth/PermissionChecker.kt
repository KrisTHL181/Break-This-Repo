package com.mcpserver.mcp.auth

/**
 * Permission Types
 */
enum class PermissionType {
    SHELL_EXECUTE,
    FILE_READ,
    FILE_WRITE,
    APP_MANAGE,
    NETWORK_ACCESS,
    SYSTEM_SETTINGS
}

/**
 * Permission Result
 */
sealed class PermissionResult {
    data object Granted : PermissionResult()
    data class Denied(val reason: String) : PermissionResult()
}

/**
 * Permission Checker
 * Checks permissions for various operations
 */
class PermissionChecker {

    private val sensitivePatterns = listOf(
        Regex("rm\\s+-rf\\s+/"),
        Regex("dd\\s+if=.+of=/dev/"),
        Regex(">\\s*/dev/"),
        Regex("mkfs\\."),
        Regex("chmod\\s+777\\s+/"),
        Regex("chown\\s+.+\\s+/")
    )

    /**
     * Check if a command is allowed
     * @param command The command to check
     * @return Permission result
     */
    fun checkCommand(command: String): PermissionResult {
        if (isDangerousCommand(command)) {
            return PermissionResult.Denied("Command contains dangerous patterns")
        }
        return PermissionResult.Granted
    }

    /**
     * Check if a command requires user confirmation
     */
    fun requiresConfirmation(command: String): Boolean {
        val dangerousKeywords = listOf("install", "uninstall", "delete", "remove", "chmod", "chown")
        val commandLower = command.lowercase()
        return dangerousKeywords.any { commandLower.contains(it) }
    }

    private fun isDangerousCommand(command: String): Boolean {
        return sensitivePatterns.any { it.containsMatchIn(command) }
    }
}
