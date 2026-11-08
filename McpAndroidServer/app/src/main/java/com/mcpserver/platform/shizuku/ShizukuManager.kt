package com.mcpserver.platform.shizuku

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.mcpserver.platform.command.CommandResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

class ShizukuManager(private val context: Context, private val scope: CoroutineScope? = null) {
    companion object {
        private const val TAG = "ShizukuManager"
    }

    sealed class ShizukuState {
        object NotInstalled : ShizukuState()
        object NotRunning : ShizukuState()
        object PermissionGranted : ShizukuState()
        object PermissionDenied : ShizukuState()
        data class Error(val message: String) : ShizukuState()
    }

    @Volatile private var pendingPermissionRequest: Int = -1

    private val _state = MutableStateFlow<ShizukuState>(ShizukuState.NotInstalled)
    val state: StateFlow<ShizukuState> = _state.asStateFlow()

    /** Re-reads the real Shizuku state (installed / running / permitted). */
    fun refresh(): ShizukuState {
        val next = try {
            when {
                !isInstalled() -> ShizukuState.NotInstalled
                !Shizuku.pingBinder() -> ShizukuState.NotRunning
                Shizuku.isPreV11() -> ShizukuState.PermissionGranted
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> ShizukuState.PermissionGranted
                else -> ShizukuState.PermissionDenied
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to read Shizuku state", t)
            ShizukuState.Error(t.message ?: t.javaClass.simpleName)
        }
        _state.value = next
        return next
    }

    /** Asks Shizuku for shell permission; Shizuku itself shows the dialog. */
    fun requestPermission(requestCode: Int = 1001): Boolean = try {
        registerListeners()
        if (!isInstalled()) {
            pendingPermissionRequest = -1
            _state.value = ShizukuState.NotInstalled
            false
        } else if (!Shizuku.pingBinder()) {
            // Shizuku is installed but not running yet: remember the request and
            // fire it from the binder-received listener instead of failing silently.
            pendingPermissionRequest = requestCode
            _state.value = ShizukuState.NotRunning
            false
        } else {
            pendingPermissionRequest = -1
            Shizuku.requestPermission(requestCode)
            true
        }
    } catch (t: Throwable) {
        Log.e(TAG, "requestPermission failed", t)
        _state.value = ShizukuState.Error(t.message ?: t.javaClass.simpleName)
        false
    }

    /** Detailed, JSON-friendly snapshot for diagnostics. */
    fun describe(): Map<String, Any?> {
        val installed = isInstalled()
        val binderAlive = installed && runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        return mapOf(
            "installed" to installed,
            "binder_alive" to binderAlive,
            "pre_v11" to (binderAlive && runCatching { Shizuku.isPreV11() }.getOrDefault(false)),
            "granted" to (binderAlive && runCatching {
                Shizuku.isPreV11() || Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)),
            "pending_request_code" to pendingPermissionRequest,
            "state" to refresh().toString(),
        )
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        registerListeners()
        refresh() is ShizukuState.PermissionGranted
    }

    private fun isInstalled(): Boolean = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
        true
    } catch (e: Exception) {
        false
    }

    suspend fun executeCommand(command: String): CommandResult {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                val stdout = process.inputStream.bufferedReader().readText()
                val stderr = process.errorStream.bufferedReader().readText()
                val exitCode = try { process.waitFor() } catch (e: Exception) { process.destroyForcibly(); -1 }
                val executionTime = System.currentTimeMillis() - startTime

                CommandResult(
                    command = command,
                    output = stdout,
                    error = stderr,
                    exitCode = exitCode,
                    executionTimeMs = executionTime,
                    executor = "runtime"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to execute command", e)
                CommandResult(
                    command = command,
                    output = "",
                    error = e.message ?: "Command execution failed",
                    exitCode = -1,
                    executionTimeMs = 0,
                    executor = "runtime"
                )
            }
        }
    }

    // ── Permission listeners ────────────────────────────────────────────────

    private val permissionResultListener = object : Shizuku.OnRequestPermissionResultListener {
        override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
            _state.value = if (grantResult == PackageManager.PERMISSION_GRANTED) {
                ShizukuState.PermissionGranted
            } else {
                ShizukuState.PermissionDenied
            }
        }
    }

    private val binderReceivedListener = object : Shizuku.OnBinderReceivedListener {
        override fun onBinderReceived() {
            refresh()
            val pending = pendingPermissionRequest
            if (pending >= 0) {
                pendingPermissionRequest = -1
                runCatching { Shizuku.requestPermission(pending) }
                    .onFailure { Log.w(TAG, "deferred requestPermission failed", it) }
            }
        }
    }

    private val binderDeadListener = object : Shizuku.OnBinderDeadListener {
        override fun onBinderDead() {
            _state.value = ShizukuState.NotRunning
        }
    }

    private fun registerListeners() {
        runCatching {
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
        }.onFailure { Log.w(TAG, "registerListeners failed", it) }
    }

    fun destroy() {
        // Cleanup
    }
}
