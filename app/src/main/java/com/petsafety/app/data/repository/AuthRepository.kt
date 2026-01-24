package com.petsafety.app.data.repository

import com.petsafety.app.data.local.AuthTokenStore
import com.petsafety.app.data.model.User
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.CanDeleteAccountResponse
import com.petsafety.app.data.network.model.LoginRequest
import com.petsafety.app.data.network.model.VerifyOtpRequest
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AuthRepository(
    private val apiService: ApiService,
    private val tokenStore: AuthTokenStore
) {
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
        return user
    }

    suspend fun logout() {
        tokenStore.clearAuthToken()
        tokenStore.clearUserInfo()
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
        apiService.deleteAccount()
        tokenStore.clearAuthToken()
        tokenStore.clearUserInfo()
        tokenStore.setBiometricEnabled(false)
    }

    fun hasStoredToken(): Boolean = tokenStore.hasStoredToken()

    fun isBiometricEnabled(): Boolean = tokenStore.isBiometricEnabled()

    fun setBiometricEnabled(enabled: Boolean) {
        tokenStore.setBiometricEnabled(enabled)
    }
}
