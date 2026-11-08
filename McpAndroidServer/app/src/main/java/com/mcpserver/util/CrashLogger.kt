package com.mcpserver.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Makes startup crashes diagnosable without adb.
 *
 * There is no logcat access on this device, so:
 *  - uncaught exceptions are persisted to Download/McpCrash.txt, and
 *  - every startup stage is recorded in Download/McpStartup.txt, so the last
 *    stage reached before a silent death is still visible after the fact.
 */
object CrashLogger {

    private const val TAG = "CrashLogger"
    const val CRASH_FILE = "McpCrash.txt"
    const val STARTUP_FILE = "McpStartup.txt"

    @Volatile private var appContext: Context? = null
    @Volatile private var sessionOpen = false
    private val startupBuffer = StringBuilder()

    @Volatile var lastCrashPath: String? = null
        private set

    /** Install the global handler. Call this first thing in Application.onCreate. */
    fun install(context: Context) {
        appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            runCatching { record("uncaught exception in thread '${thread.name}'", error) }
            previous?.uncaughtException(thread, error)
        }
    }

    /** Persist a fatal/caught error report. Safe to call from any thread. */
    fun record(label: String, error: Throwable) {
        writeReport(buildReport(label, error))
    }

    /** Persist a plain message report. */
    fun recordMessage(label: String, detail: String) {
        writeReport(
            buildString {
                appendLine("=== MCP Android Server report ===")
                appendLine("time  : ${timestamp()}")
                appendLine("label : $label")
                appendLine("env   : ${environmentLine()}")
                appendLine()
                appendLine(detail)
            }
        )
    }

    /** Record a startup stage; the file is rewritten on every call. */
    fun stage(name: String) {
        runCatching {
            val context = appContext ?: return
            synchronized(startupBuffer) {
                if (!sessionOpen) {
                    sessionOpen = true
                    startupBuffer.setLength(0)
                    startupBuffer.appendLine("=== MCP Android Server startup trace ===")
                    startupBuffer.appendLine("app   : ${appVersion(context)}")
                    startupBuffer.appendLine("env   : ${environmentLine()}")
                    startupBuffer.appendLine()
                }
                startupBuffer.appendLine("${timestamp()}  $name")
                writeFile(context, STARTUP_FILE, startupBuffer.toString())
            }
        }.onFailure { Log.w(TAG, "stage($name) failed", it) }
    }

    private fun writeReport(report: String) {
        val context = appContext
        if (context == null) {
            Log.e(TAG, "CrashLogger not installed; report follows\n$report")
            return
        }
        lastCrashPath = writeFile(context, CRASH_FILE, report)
        Log.e(TAG, "crash report written to ${lastCrashPath ?: "<failed>"}")
    }

    /** @return the public path when it could be written, else the private path. */
    private fun writeFile(context: Context, fileName: String, text: String): String? {
        val bytes = text.toByteArray(Charsets.UTF_8)
        PublicFiles.saveToDownloads(context, fileName, "text/plain", bytes)?.let { return it }
        return PublicFiles.saveToAppExternal(context, fileName, bytes)
    }

    private fun buildReport(label: String, error: Throwable): String {
        val stack = StringWriter()
        PrintWriter(stack).use { error.printStackTrace(it) }
        return buildString {
            appendLine("=== MCP Android Server crash report ===")
            appendLine("time  : ${timestamp()}")
            appendLine("label : $label")
            appendLine("env   : ${environmentLine()}")
            appendLine()
            appendLine("--- startup stages ---")
            synchronized(startupBuffer) { append(startupBuffer) }
            appendLine()
            appendLine("--- stack trace ---")
            appendLine(stack.toString())
        }
    }

    private fun environmentLine(): String = buildString {
        append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        append(" | ${Build.MANUFACTURER} ${Build.MODEL}")
        append(" | ${Build.SUPPORTED_ABIS.joinToString()}")
    }

    private fun appVersion(context: Context): String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
        "${info.versionName} ($code)"
    }.getOrElse { "<unknown>" }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
}
