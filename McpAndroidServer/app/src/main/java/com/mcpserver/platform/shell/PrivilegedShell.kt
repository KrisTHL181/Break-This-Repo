package com.mcpserver.platform.shell

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Real command execution, in descending order of privilege:
 *   1. Shizuku  -> runs as the shell user (no root needed)
 *   2. su       -> rooted devices / root managers
 *   3. this app -> unprivileged fallback, always available
 */
object PrivilegedShell {

    private const val TAG = "PrivilegedShell"

    data class Result(
        val exitCode: Int,
        val stdout: String,
        val stderr: String,
        val executor: String,
        val durationMs: Long,
    )

    @Volatile private var rootChecked = false
    @Volatile private var rootAvailable = false

    fun isShizukuAvailable(): Boolean = runCatching {
        rikka.shizuku.Shizuku.pingBinder()
    }.getOrDefault(false)

    fun shizukuVersion(): Int = runCatching { rikka.shizuku.Shizuku.getVersion() }.getOrDefault(-1)

    /** True when `su` exists and actually returns uid 0. */
    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        if (rootChecked) return@withContext rootAvailable
        rootAvailable = runCatching {
            val process = ProcessBuilder("su", "-c", "id").redirectErrorStream(true).start()
            val output = process.inputStream.bufferedReader().use { it.readText() }
            val finished = withTimeoutOrNull(5_000L) { process.waitFor() } != null
            if (!finished) process.destroyForcibly()
            finished && output.contains("uid=0")
        }.getOrDefault(false)
        rootChecked = true
        rootAvailable
    }

    /** Name of the strongest channel currently usable. */
    suspend fun bestExecutor(): String = when {
        isShizukuAvailable() -> "shizuku"
        isRootAvailable() -> "root"
        else -> "app"
    }

    suspend fun exec(command: String, timeoutMs: Long = 30_000L): Result = withContext(Dispatchers.IO) {
        val startedAt = System.currentTimeMillis()
        var executor = "app"

        var process: Process? = runCatching { startViaShizuku(command) }
            .onFailure { Log.w(TAG, "Shizuku spawn failed", it) }
            .getOrNull()
            ?.also { executor = "shizuku" }

        if (process == null && isRootAvailable()) {
            process = runCatching { ProcessBuilder("su", "-c", command).start() }
                .onFailure { Log.w(TAG, "su spawn failed", it) }
                .getOrNull()
                ?.also { executor = "root" }
        }

        if (process == null) {
            process = runCatching { ProcessBuilder("sh", "-c", command).start() }
                .getOrElse { error ->
                    return@withContext Result(
                        exitCode = -1,
                        stdout = "",
                        stderr = error.message ?: error.javaClass.simpleName,
                        executor = "none",
                        durationMs = System.currentTimeMillis() - startedAt,
                    )
                }
        }

        val stdout = StringBuilder()
        val stderr = StringBuilder()
        val stdoutJob = launch {
            runCatching { process.inputStream.bufferedReader().use { stdout.append(it.readText()) } }
        }
        val stderrJob = launch {
            runCatching { process.errorStream.bufferedReader().use { stderr.append(it.readText()) } }
        }

        val finished = withTimeoutOrNull(timeoutMs) { process.waitFor() }
        if (finished == null) {
            runCatching { process.destroyForcibly() }
            stdoutJob.cancel()
            stderrJob.cancel()
            return@withContext Result(
                exitCode = -1,
                stdout = stdout.toString(),
                stderr = stderr.toString() + "\n[timed out after ${timeoutMs}ms]",
                executor = executor,
                durationMs = System.currentTimeMillis() - startedAt,
            )
        }

        runCatching { joinAll(stdoutJob, stderrJob) }
        Result(
            exitCode = runCatching { process.exitValue() }.getOrDefault(-1),
            stdout = stdout.toString(),
            stderr = stderr.toString(),
            executor = executor,
            durationMs = System.currentTimeMillis() - startedAt,
        )
    }

    private fun startViaShizuku(command: String): Process? {
        if (!rikka.shizuku.Shizuku.pingBinder()) return null
        val method = Class.forName("rikka.shizuku.Shizuku").getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java,
        )
        method.isAccessible = true
        return method.invoke(null, arrayOf("sh", "-c", command), null, null) as? Process
    }
}
