package com.mcpserver.platform.shizuku

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class BinderService : Service() {
    companion object {
        private const val TAG = "BinderService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d(TAG, "Service bound")
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
    }
}
