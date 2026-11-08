package com.mcpserver.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.mcpserver.McpApplication

/** Brings the server back after a reboot or an app update. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        val app = context.applicationContext as? McpApplication ?: return
        runCatching {
            if (app.mcpServer.settingsStore.autoStart) {
                ContextCompat.startForegroundService(context, Intent(context, ServerService::class.java))
                Log.i("BootReceiver", "server restarted after $action")
            }
        }.onFailure { Log.w("BootReceiver", "restart failed", it) }
    }
}
