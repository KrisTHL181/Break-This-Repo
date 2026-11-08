package com.mcpserver

import android.app.Application
import com.mcpserver.mcp.McpServer
import com.mcpserver.platform.shizuku.ShizukuManager
import com.mcpserver.util.CrashLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class McpApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var mcpServer: McpServer
        private set

    lateinit var shizukuManager: ShizukuManager
        private set

    /** False when startup initialisation failed; callers must not touch the managers then. */
    val isReady: Boolean
        get() = ::mcpServer.isInitialized && ::shizukuManager.isInitialized

    override fun onCreate() {
        super.onCreate()

        // Install crash capture before anything else can throw.
        CrashLogger.install(this)
        CrashLogger.stage("Application.onCreate: begin")

        instance = this

        try {
            initManagers()
            CrashLogger.stage("Application.onCreate: managers ready")
        } catch (t: Throwable) {
            CrashLogger.record("Application.initManagers failed", t)
        }

        applicationScope.launch {
            runCatching { shizukuManager.initialize() }
                .onFailure { CrashLogger.record("ShizukuManager.initialize failed", it) }
            CrashLogger.stage("Application.onCreate: shizuku probe done")

        applicationScope.launch {
            runCatching {
                val store = mcpServer.settingsStore
                if (store.autoStart) {
                    mcpServer.start(store.toAppSettings())
                    CrashLogger.stage("Application.onCreate: auto-started server")
                }
            }.onFailure { CrashLogger.record("auto-start failed", it) }
        }
        }
    }

    private fun initManagers() {
        shizukuManager = ShizukuManager(this, applicationScope)
        mcpServer = McpServer(this, shizukuManager)
    }

    companion object {
        lateinit var instance: McpApplication
            private set
    }
}
