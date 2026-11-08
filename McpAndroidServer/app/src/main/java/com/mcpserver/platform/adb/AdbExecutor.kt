package com.mcpserver.platform.adb

import com.mcpserver.platform.command.CommandExecutor
import com.mcpserver.platform.command.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ADB Command Executor
 * Executes commands via Android Debug Bridge
 */
class AdbExecutor : CommandExecutor {

    companion object {
        private const val TAG = "AdbExecutor"
        private const val COMMAND_TIMEOUT_MS = 30000L
    }

    private var adbPath: String = "adb"
    private var isWirelessMode = false
    private var targetDevice: String? = null

    /**
     * Set ADB path
     */
    fun setAdbPath(path: String) {
        adbPath = path
    }

    /**
     * Set wireless mode
     */
    fun setWirelessMode(enabled: Boolean, host: String? = null, port: Int = 5555) {
        isWirelessMode = enabled
        if (enabled && host != null) {
            targetDevice = "$host:$port"
        }
    }

    override suspend fun execute(command: String, workingDirectory: String?): CommandResult {
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()

            try {
                val fullCommand = buildAdbCommand(command, workingDirectory)
                val process = Runtime.getRuntime().exec(fullCommand)

                val stdout = readStream(process.inputStream)
                val stderr = readStream(process.errorStream)

                val exitCode = try {
                    process.waitFor()
                } catch (e: Exception) {
                    process.destroyForcibly()
                    -1
                }

                val executionTime = System.currentTimeMillis() - startTime

                if (exitCode == 0) {
                    CommandResult.success(
                        command = command,
                        output = stdout,
                        executionTimeMs = executionTime,
                        executor = getExecutorName()
                    )
                } else {
                    CommandResult.error(
                        command = command,
                        output = stdout,
                        error = stderr,
                        exitCode = exitCode,
                        executionTimeMs = executionTime,
                        executor = getExecutorName()
                    )
                }
            } catch (e: Exception) {
                CommandResult.error(
                    command = command,
                    output = "",
                    error = e.message ?: "Command execution failed",
                    exitCode = -1,
                    executionTimeMs = System.currentTimeMillis() - startTime,
                    executor = getExecutorName()
                )
            }
        }
    }

    private fun buildAdbCommand(command: String, workingDirectory: String?): Array<String> {
        val cmdList = mutableListOf<String>()

        // Add adb path
        cmdList.add(adbPath)

        // Add device specifier if in wireless mode
        targetDevice?.let { device ->
            cmdList.add("-s")
            cmdList.add(device)
        }

        // Add shell command
        cmdList.add("shell")

        // Add working directory if specified
        if (workingDirectory != null) {
            cmdList.add("cd")
            cmdList.add(workingDirectory)
            cmdList.add("&&")
        }

        // Add the actual command
        cmdList.add(command)

        return cmdList.toTypedArray()
    }

    private fun readStream(inputStream: java.io.InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val output = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line)
            output.append("\n")
        }
        return output.toString().trimEnd()
    }

    override suspend fun isAvailable(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf(adbPath, "version"))
            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    override fun getExecutorName(): String = "ADB"
}
