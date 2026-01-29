package com.petsafety.app.data.repository

import android.util.Log
import com.petsafety.app.data.fcm.FCMRepository
import com.petsafety.app.data.local.AuthTokenStore
import com.petsafety.app.data.model.User
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.CanDeleteAccountResponse
import com.petsafety.app.data.network.model.LoginRequest
import com.petsafety.app.data.network.model.SupportRequest
import com.petsafety.app.data.network.model.VerifyOtpRequest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository(
    private val apiService: ApiService,
    private val tokenStore: AuthTokenStore,
    private val fcmRepository: FCMRepository? = null // Optional for backward compatibility
) {
    companion object {
        private const val TAG = "AuthRepository"
    }
    val isAuthenticated: Flow<Boolean> = tokenStore.authToken.map { !it.isNullOrBlank() }

    suspend fun login(email: String) {
        val response = apiService.login(LoginRequest(email))
        if (!response.success) {
            throw Exception(response.error ?: "Failed to send OTP")
        }
    }

    suspend fun verifyOtp(email: String, code: String): User {
        val response = apiService.verifyOtp(VerifyOtpRequest(email, code))
        if (!response.success) {
            throw Exception(response.error ?: "OTP verification failed")
        }
        val data = response.data ?: throw Exception("Missing response data")
        val token = data.token
        val user = data.user
        tokenStore.saveAuthToken(token)
        tokenStore.saveUserInfo(user.id, user.email)

        // Register FCM token after successful login
        registerFCMToken()

        return user
    }

    suspend fun logout() {
        // Unregister FCM token before clearing auth
        unregisterFCMToken()

        tokenStore.clearAuthToken()
        tokenStore.clearUserInfo()
    }

    /**
     * Register FCM token with backend
     * Called after successful login
     */
    private suspend fun registerFCMToken() {
        try {
            fcmRepository?.registerToken()
            Log.d(TAG, "FCM token registered after login")
        } catch (e: Exception) {
            // Don't fail login if FCM registration fails
            Log.e(TAG, "Failed to register FCM token", e)
        }
    }

    /**
     * Unregister FCM token from backend
     * Called before logout
     */
    private suspend fun unregisterFCMToken() {
        try {
            fcmRepository?.removeToken()
            Log.d(TAG, "FCM token unregistered before logout")
        } catch (e: Exception) {
            // Don't fail logout if FCM unregistration fails
            Log.e(TAG, "Failed to unregister FCM token", e)
        }
    }

    suspend fun getCurrentUser(): User =
        apiService.getCurrentUser().data?.user ?: error("Missing user")

    suspend fun updateUser(updates: Map<String, Any>): User {
        val json = JsonObject(
            updates.mapValues { (_, value) ->
                when (value) {
                    is String -> JsonPrimitive(value)
                    is Boolean -> JsonPrimitive(value)
                    is Int -> JsonPrimitive(value)
                    is Double -> JsonPrimitive(value)
                    else -> JsonNull
                }
            }
        )
        return apiService.updateUser(json).data?.user ?: error("Missing user")
    }

    suspend fun canDeleteAccount(): CanDeleteAccountResponse {
        val response = apiService.canDeleteAccount()
        if (!response.success) {
            throw Exception(response.error ?: "Failed to check delete eligibility")
        }
        return response.data ?: throw Exception("Missing response data")
    }

    suspend fun deleteAccount() {
        // Unregister FCM token before deleting account
        unregisterFCMToken()

        apiService.deleteAccount()
        tokenStore.clearAuthToken()
        tokenStore.clearUserInfo()
        tokenStore.setBiometricEnabled(false)

        // Also delete FCM instance ID to prevent any further notifications
        try {
            fcmRepository?.deleteInstanceId()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete FCM instance ID", e)
        }
    }

    fun hasStoredToken(): Boolean = tokenStore.hasStoredToken()

    fun isBiometricEnabled(): Boolean = tokenStore.isBiometricEnabled()

    fun setBiometricEnabled(enabled: Boolean) {
        tokenStore.setBiometricEnabled(enabled)
    }

    suspend fun submitSupportRequest(category: String, subject: String, message: String): String {
        val response = apiService.submitSupportRequest(
            SupportRequest(category = category, subject = subject, message = message)
        )
        if (!response.success) {
            throw Exception(response.error ?: "Failed to submit support request")
        }
        return response.data?.ticketId ?: throw Exception("Missing ticket ID")
    }
}
