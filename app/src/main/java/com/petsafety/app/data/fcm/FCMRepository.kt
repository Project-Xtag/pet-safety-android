package com.petsafety.app.data.fcm

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.messaging.FirebaseMessaging
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.FCMTokenRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for FCM token management
 *
 * Handles:
 * - Local token storage
 * - Backend token registration/removal
 * - Token refresh handling
 */
@Singleton
class FCMRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService
) {
    /**
     * Encrypted SharedPreferences for secure FCM token storage.
     * Returns null if encryption is unavailable - in that case, we don't persist
     * the token locally (FCM will provide it fresh each time).
     *
     * Security: Never fall back to unencrypted storage for sensitive tokens.
     */
    private val prefs: android.content.SharedPreferences? by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                "fcm_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Security: Do NOT fall back to unencrypted storage
            // FCM will provide a fresh token on each app launch
            Log.w(TAG, "EncryptedSharedPreferences not available, token will not be persisted locally", e)
            null
        }
    }

    /**
     * Get current FCM token, requesting a new one if needed
     */
    suspend fun getCurrentToken(): String? {
        return try {
            val token = FirebaseMessaging.getInstance().token.await()
            saveTokenLocally(token)
            token
        } catch (e: Exception) {
            // Expected on emulators without Google Play Services
            Log.w(TAG, "FCM token not available: ${e.message}")
            getStoredToken()
        }
    }

    /**
     * Save FCM token locally (only if secure storage is available)
     */
    fun saveTokenLocally(token: String) {
        prefs?.edit {
            putString(KEY_FCM_TOKEN, token)
        }
    }

    /**
     * Get locally stored FCM token
     */
    fun getStoredToken(): String? {
        return prefs?.getString(KEY_FCM_TOKEN, null)
    }

    /**
     * Register FCM token with backend
     */
    suspend fun registerToken(token: String? = null) {
        val tokenToRegister = token ?: getCurrentToken() ?: return

        try {
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
            val request = FCMTokenRequest(
                token = tokenToRegister,
                deviceName = deviceName,
                platform = "android"
            )
            apiService.registerFCMToken(request)
            Log.d(TAG, "FCM token registered with backend")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM token with backend", e)
            throw e
        }
    }

    /**
     * Remove FCM token from backend (on logout)
     */
    suspend fun removeToken(token: String? = null) {
        val tokenToRemove = token ?: getStoredToken() ?: return

        try {
            apiService.removeFCMToken(tokenToRemove)
            Log.d(TAG, "FCM token removed from backend")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove FCM token from backend", e)
            // Don't throw - user is logging out anyway
        }
    }

    /**
     * Clear locally stored token
     */
    fun clearStoredToken() {
        prefs?.edit {
            remove(KEY_FCM_TOKEN)
        }
    }

    /**
     * Delete FCM instance ID (forces new token on next request)
     */
    suspend fun deleteInstanceId() {
        try {
            FirebaseMessaging.getInstance().deleteToken().await()
            clearStoredToken()
            Log.d(TAG, "FCM instance ID deleted")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete FCM instance ID", e)
        }
    }

    companion object {
        private const val TAG = "FCMRepository"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }
}
