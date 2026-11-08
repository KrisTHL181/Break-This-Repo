package com.mcpserver.mcp.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

private const val TAG = "AuthManager"
private const val PREFS_NAME = "mcp_auth"
private const val KEY_API_KEY_HASH = "api_key_hash"
private const val KEY_AUTH_ENABLED = "auth_enabled"

/**
 * Auth Manager
 * Manages API Key-based authentication.
 */
class AuthManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var authEnabled: Boolean = prefs.getBoolean(KEY_AUTH_ENABLED, false)

    @Volatile
    private var storedApiKeyHash: String? = prefs.getString(KEY_API_KEY_HASH, null)

    // ── API Key Management ──────────────────────────────────────────────────

    /**
     * Generate a new API key and store its hash.
     * @return The plain-text API key (only shown once!)
     */
    fun generateApiKey(): String {
        val apiKey = "mcp_${UUID.randomUUID().toString().replace("-", "")}"
        val hash = hashApiKey(apiKey)

        prefs.edit()
            .putString(KEY_API_KEY_HASH, hash)
            .putBoolean(KEY_AUTH_ENABLED, true)
            .apply()

        storedApiKeyHash = hash
        authEnabled = true

        Log.i(TAG, "Generated new API key")
        return apiKey
    }

    /**
     * Validate an API key.
     * @param apiKey The API key to validate
     * @return true if the key is valid
     */
    fun validateApiKey(apiKey: String): Boolean {
        if (!authEnabled) return true
        if (apiKey.isBlank()) return false

        val storedHash = storedApiKeyHash ?: return false
        val inputHash = hashApiKey(apiKey)

        return storedHash == inputHash
    }

    /**
     * Revoke the current API key.
     */
    fun revokeApiKey() {
        prefs.edit()
            .remove(KEY_API_KEY_HASH)
            .putBoolean(KEY_AUTH_ENABLED, false)
            .apply()

        storedApiKeyHash = null
        authEnabled = false

        Log.i(TAG, "API key revoked")
    }

    /**
     * Enable or disable authentication.
     */
    fun setAuthEnabled(enabled: Boolean) {
        authEnabled = enabled
        prefs.edit().putBoolean(KEY_AUTH_ENABLED, enabled).apply()
    }

    /**
     * Check if authentication is enabled.
     */
    fun isAuthEnabled(): Boolean = authEnabled

    /**
     * Check if an API key is configured.
     */
    fun hasApiKey(): Boolean = storedApiKeyHash != null

    // ── Hashing ─────────────────────────────────────────────────────────────

    /**
     * Hash an API key using SHA-256.
     */
    private fun hashApiKey(apiKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(apiKey.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hash)
    }
}
