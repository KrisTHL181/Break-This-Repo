package com.mcpserver.platform.shizuku

import android.content.pm.PackageManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

class PermissionHelper(private val scope: CoroutineScope) {
    
    companion object {
        private const val TAG = "PermissionHelper"
        private const val PERMISSION_REQUEST_CODE = 1002
        private const val PERMISSION_EXPIRY_CHECK_INTERVAL_MS = 60000L // 1 minute
    }
    
    sealed class PermissionState {
        object Checking : PermissionState()
        object Granted : PermissionState()
        object Denied : PermissionState()
        object Expired : PermissionState()
        data class Error(val message: String) : PermissionState()
    }
    
    private val _state = MutableStateFlow<PermissionState>(PermissionState.Checking)
    val state: StateFlow<PermissionState> = _state.asStateFlow()
    
    private var expiryCheckJob: Job? = null
    private var lastPermissionCheckTime = 0L
    private val permissionCacheDurationMs = 300000L // 5 minutes
    
    private val permissionResultListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == PERMISSION_REQUEST_CODE) {
            when (grantResult) {
                PackageManager.PERMISSION_GRANTED -> {
                    Log.d(TAG, "Shizuku permission granted")
                    _state.value = PermissionState.Granted
                    lastPermissionCheckTime = System.currentTimeMillis()
                    startExpiryCheck()
                }
                else -> {
                    Log.w(TAG, "Shizuku permission denied")
                    _state.value = PermissionState.Denied
                }
            }
        }
    }
    
    init {
        Shizuku.addRequestPermissionResultListener(permissionResultListener)
    }
    
    suspend fun checkPermission(): PermissionState {
        return withContext(Dispatchers.IO) {
            try {
                if (!Shizuku.pingBinder()) {
                    _state.value = PermissionState.Error("Shizuku not running")
                    return@withContext PermissionState.Error("Shizuku not running")
                }
                
                val permissionResult = Shizuku.checkSelfPermission()
                when (permissionResult) {
                    PackageManager.PERMISSION_GRANTED -> {
                        _state.value = PermissionState.Granted
                        lastPermissionCheckTime = System.currentTimeMillis()
                        startExpiryCheck()
                        return@withContext PermissionState.Granted
                    }
                    PackageManager.PERMISSION_DENIED -> {
                        _state.value = PermissionState.Denied
                        return@withContext PermissionState.Denied
                    }
                    else -> {
                        _state.value = PermissionState.Checking
                        return@withContext PermissionState.Checking
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check Shizuku permission", e)
                _state.value = PermissionState.Error(e.message ?: "Permission check failed")
                return@withContext PermissionState.Error(e.message ?: "Permission check failed")
            }
        }
    }
    
    suspend fun requestPermission(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (!Shizuku.pingBinder()) {
                    return@withContext false
                }
                
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    return@withContext true
                }
                
                if (!Shizuku.shouldShowRequestPermissionRationale()) {
                    // 用户之前已经拒绝过，需要引导用户手动授权
                    Log.w(TAG, "Permission rationale should be shown")
                    return@withContext false
                }
                
                Shizuku.requestPermission(PERMISSION_REQUEST_CODE)
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to request Shizuku permission", e)
                return@withContext false
            }
        }
    }
    
    private fun startExpiryCheck() {
        expiryCheckJob?.cancel()
        expiryCheckJob = scope.launch {
            while (isActive) {
                delay(PERMISSION_EXPIRY_CHECK_INTERVAL_MS)
                checkPermissionExpiry()
            }
        }
    }
    
    private suspend fun checkPermissionExpiry() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPermissionCheckTime > permissionCacheDurationMs) {
            // 权限缓存已过期，重新检查
            Log.d(TAG, "Permission cache expired, rechecking")
            checkPermission()
        }
    }
    
    fun stopExpiryCheck() {
        expiryCheckJob?.cancel()
        expiryCheckJob = null
    }
    
    fun isPermissionGranted(): Boolean {
        return _state.value == PermissionState.Granted
    }
    
    fun destroy() {
        stopExpiryCheck()
        try {
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup permission listener", e)
        }
    }
}