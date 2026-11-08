package com.mcpserver.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.mcpserver.McpApplication
import com.mcpserver.ui.MainActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Keeps the MCP server alive.
 *
 * Without a foreground service Android freezes the process once the UI is
 * backgrounded: the listening socket stays bound (so TCP still connects) but the
 * app never answers, which makes every client request time out.
 */
class ServerService : Service() {

    companion object {
        private const val TAG = "ServerService"
        private const val CHANNEL_ID = "mcp_server"
        private const val NOTIFICATION_ID = 8392
        const val ACTION_STOP = "com.mcpserver.action.STOP_SERVER"
    }

    private var watchdogJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("正在启动…"))

        val app = application as? McpApplication
        if (app == null || !app.isReady) {
            Log.e(TAG, "application not ready")
            stopSelf()
            return START_NOT_STICKY
        }

        if (intent?.action == ACTION_STOP) {
            app.applicationScope.launch {
                runCatching { app.mcpServer.stop() }
                updateNotification("已停止")
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            return START_NOT_STICKY
        }

        app.applicationScope.launch {
            val store = app.mcpServer.settingsStore
            val settings = store.toAppSettings()
            val started = runCatching { app.mcpServer.start(settings) }.getOrDefault(false)
            val address = "http://0.0.0.0:${settings.serverPort}"
            updateNotification(
                if (started) "运行中 $address" else "启动失败（端口 ${settings.serverPort} 可能被占用）"
            )
        }
        startWatchdog(app)
        return START_STICKY
    }

    /** Restarts the listener if it ever stops, so the server cannot silently die. */
    private fun startWatchdog(app: McpApplication) {
        if (watchdogJob?.isActive == true) return
        watchdogJob = app.applicationScope.launch {
            while (isActive) {
                delay(30_000L)
                runCatching {
                    val server = app.mcpServer
                    if (!server.state.value.isRunning) {
                        Log.w(TAG, "server not running, restarting")
                        server.start(server.settingsStore.toAppSettings())
                    }
                }.onFailure { Log.w(TAG, "watchdog restart failed", it) }
            }
        }
    }

    override fun onDestroy() {
        watchdogJob?.cancel()
        watchdogJob = null
        Log.i(TAG, "service destroyed")
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "MCP Server", NotificationManager.IMPORTANCE_LOW).apply {
                description = "保持 MCP 服务器运行"
                setShowBadge(false)
            }
        )
    }

    private fun buildNotification(text: String): Notification {
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MCP Android Server")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        runCatching { manager.notify(NOTIFICATION_ID, buildNotification(text)) }
    }
}
