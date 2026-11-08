package com.mcpserver.platform.command

import android.util.Log
import com.mcpserver.platform.shizuku.ShizukuManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.BufferedReader
import java.io.InputStreamReader

class ShizukuCommandExecutor(
    private val shizukuManager: ShizukuManager,
    private val scope: CoroutineScope
) : CommandExecutor {
    
    companion object {
        private const val TAG = "ShizukuCommandExecutor"
        private const val DEFAULT_TIMEOUT_MS = 30000L // 30 seconds
        private const val DEFAULT_MAX_CONCURRENT = 5
    }
    
    private val _state = MutableStateFlow<ExecutionState>(ExecutionState.Idle)
    val state: StateFlow<ExecutionState> = _state.asStateFlow()
    
    private val semaphore = Semaphore(DEFAULT_MAX_CONCURRENT)
    private var timeoutMs = DEFAULT_TIMEOUT_MS
    private var maxConcurrent = DEFAULT_MAX_CONCURRENT
    
    private val executionHistory = mutableListOf<ExecutionRecord>()
    
    override suspend fun execute(
        command: String,
        workingDirectory: String?
    ): CommandResult {
        return withContext(Dispatchers.IO) {
            semaphore.withPermit {
                try {
                    _state.value = ExecutionState.Executing(command)
                    val startTime = System.currentTimeMillis()
                    
                    // 检查Shizuku权限
                    if (shizukuManager.state.value != ShizukuManager.ShizukuState.PermissionGranted) {
                        throw IllegalStateException("Shizuku permission not granted")
                    }
                    
                    // 构建完整命令
                    val fullCommand = if (workingDirectory != null) {
                        "cd $workingDirectory && $command"
                    } else {
                        command
                    }
                    
                    // 通过Shizuku执行命令
                    val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "shizuku --user 0 $fullCommand"))
                    
                    val stdoutFuture = scope.async {
                        readStream(process.inputStream)
                    }
                    
                    val stderrFuture = scope.async {
                        readStream(process.errorStream)
                    }
                    
                    val stdout: String
                    val stderr: String
                    
                    try {
                        withTimeout(timeoutMs) {
                            stdout = stdoutFuture.await()
                            stderr = stderrFuture.await()
                        }
                    } catch (e: TimeoutCancellationException) {
                        process.destroyForcibly()
                        val result = CommandResult.error(
                            command = command,
                            output = "",
                            error = "Command timed out after ${timeoutMs}ms",
                            exitCode = -1,
                            executionTimeMs = timeoutMs,
                            executor = getExecutorName()
                        )
                        
                        executionHistory.add(ExecutionRecord(
                            command = command,
                            result = result,
                            timestamp = System.currentTimeMillis()
                        ))
                        
                        _state.value = ExecutionState.Idle
                        return@withContext result
                    }
                    
                    val exitCode = try {
                        process.waitFor()
                    } catch (e: Exception) {
                        process.destroyForcibly()
                        -1
                    }
                    
                    val executionTime = System.currentTimeMillis() - startTime
                    
                    val result = if (exitCode == 0) {
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
                    
                    // 记录执行历史
                    executionHistory.add(ExecutionRecord(
                        command = command,
                        result = result,
                        timestamp = System.currentTimeMillis()
                    ))
                    
                    // 保持历史记录在合理范围内
                    if (executionHistory.size > 100) {
                        executionHistory.removeAt(0)
                    }
                    
                    _state.value = ExecutionState.Idle
                    return@withContext result
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to execute command", e)
                    _state.value = ExecutionState.Error(e.message ?: "Execution failed")
                    
                    val errorResult = CommandResult.error(
                        command = command,
                        output = "",
                        error = e.message ?: "Execution failed",
                        exitCode = -1,
                        executionTimeMs = 0,
                        executor = getExecutorName()
                    )
                    
                    executionHistory.add(ExecutionRecord(
                        command = command,
                        result = errorResult,
                        timestamp = System.currentTimeMillis()
                    ))
                    
                    return@withContext errorResult
                }
            }
        }
    }
    
    override suspend fun isAvailable(): Boolean {
        return try {
            shizukuManager.state.value == ShizukuManager.ShizukuState.PermissionGranted
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check availability", e)
            false
        }
    }
    
    override fun getExecutorName(): String {
        return "Shizuku"
    }
    
    private suspend fun readStream(inputStream: java.io.InputStream): String {
        return withContext(Dispatchers.IO) {
            val reader = BufferedReader(InputStreamReader(inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line)
                output.append("\n")
            }
            output.toString().trimEnd()
        }
    }
    
    fun setTimeout(timeoutMs: Long) {
        this.timeoutMs = timeoutMs
    }
    
    fun setMaxConcurrent(maxConcurrent: Int) {
        this.maxConcurrent = maxConcurrent
    }
    
    fun getExecutionHistory(): List<ExecutionRecord> {
        return executionHistory.toList()
    }
    
    fun clearHistory() {
        executionHistory.clear()
    }
    
    fun destroy() {
        scope.cancel()
    }
    
    sealed class ExecutionState {
        object Idle : ExecutionState()
        data class Executing(val command: String) : ExecutionState()
        data class Error(val message: String) : ExecutionState()
    }
    
    data class ExecutionRecord(
        val command: String,
        val result: CommandResult,
        val timestamp: Long
    )
}