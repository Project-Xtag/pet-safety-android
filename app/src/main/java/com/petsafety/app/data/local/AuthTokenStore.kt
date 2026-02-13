package com.petsafety.app.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthTokenStore(context: Context) {
    private val authTokenKey = "auth_token"
    private val refreshTokenKey = "refresh_token"
    private val userIdKey = "user_id"
    private val userEmailKey = "user_email"
    private val biometricEnabledKey = "biometric_enabled"

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "pet_safety_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _authToken = MutableStateFlow(prefs.getString(authTokenKey, null))
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _refreshToken = MutableStateFlow(prefs.getString(refreshTokenKey, null))
    val refreshToken: StateFlow<String?> = _refreshToken.asStateFlow()

    private val _userId = MutableStateFlow(prefs.getString(userIdKey, null))
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private val _userEmail = MutableStateFlow(prefs.getString(userEmailKey, null))
    val userEmail: StateFlow<String?> = _userEmail.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(prefs.getBoolean(biometricEnabledKey, false))
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    fun hasStoredToken(): Boolean = !prefs.getString(authTokenKey, null).isNullOrBlank()

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(biometricEnabledKey, false)

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(biometricEnabledKey, enabled).apply()
        _biometricEnabled.value = enabled
    }

    suspend fun saveAuthToken(token: String) {
        prefs.edit().putString(authTokenKey, token).apply()
        _authToken.value = token
    }

    suspend fun clearAuthToken() {
        prefs.edit().remove(authTokenKey).apply()
        _authToken.value = null
    }

    suspend fun saveRefreshToken(token: String) {
        prefs.edit().putString(refreshTokenKey, token).apply()
        _refreshToken.value = token
    }

    suspend fun clearRefreshToken() {
        prefs.edit().remove(refreshTokenKey).apply()
        _refreshToken.value = null
    }

    suspend fun saveUserInfo(userId: String, email: String) {
        prefs.edit()
            .putString(userIdKey, userId)
            .putString(userEmailKey, email)
            .apply()
        _userId.value = userId
        _userEmail.value = email
    }

    suspend fun clearUserInfo() {
        prefs.edit()
            .remove(userIdKey)
            .remove(userEmailKey)
            .apply()
        _userId.value = null
        _userEmail.value = null
    }
}
