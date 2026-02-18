package com.petsafety.app.data.network

import timber.log.Timber
import com.petsafety.app.BuildConfig
import com.petsafety.app.data.local.AuthTokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

/**
 * Handles 401 Unauthorized responses by attempting a token refresh.
 *
 * Uses a separate lightweight OkHttpClient (without an authenticator) to call
 * POST /auth/refresh. This avoids a circular dependency — the main OkHttpClient
 * has this authenticator attached, so using it for refresh would cause infinite loops.
 *
 * Flow:
 * 1. On 401, read the stored refresh token.
 * 2. Call POST /auth/refresh with the refresh token.
 * 3. If successful, save the new tokens and retry the original request.
 * 4. If refresh fails, clear all tokens and emit authExpiredEvent.
 */
class TokenAuthenticator(
    private val tokenStore: AuthTokenStore
) : Authenticator {

    companion object {
        // Shared flow to notify observers when auth expires
        private val _authExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val authExpiredEvent: SharedFlow<Unit> = _authExpiredEvent.asSharedFlow()
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Lightweight OkHttpClient for refresh calls only.
     * No authenticator attached — prevents infinite retry loops.
     */
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry if we already attempted a refresh for this request
        if (response.request.header("Retry-Auth") != null) {
            return null
        }

        synchronized(this) {
            // Check if token was already refreshed by another concurrent thread.
            // If the current stored token differs from the one used in the failed request,
            // another thread already refreshed it — just retry with the new token.
            val currentToken = tokenStore.authToken.value
            val failedToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")

            if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                Timber.d("Token already refreshed by another thread, retrying")
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .header("Retry-Auth", "true")
                    .build()
            }

            // Attempt refresh
            val refreshToken = tokenStore.refreshToken.value
            if (refreshToken.isNullOrBlank()) {
                Timber.d("No refresh token available, clearing session")
                clearAndNotify()
                return null
            }

            return try {
                val newTokens = executeRefresh(refreshToken)
                if (newTokens != null) {
                    Timber.d("Token refresh successful")
                    runBlocking {
                        tokenStore.saveAuthToken(newTokens.first)
                        tokenStore.saveRefreshToken(newTokens.second)
                    }
                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${newTokens.first}")
                        .header("Retry-Auth", "true")
                        .build()
                } else {
                    Timber.w("Token refresh returned null, clearing session")
                    clearAndNotify()
                    null
                }
            } catch (e: Exception) {
                Timber.e("Token refresh failed: ${e.message}")
                clearAndNotify()
                null
            }
        }
    }

    /**
     * Execute a synchronous POST /auth/refresh call using the lightweight client.
     *
     * @param refreshToken The current refresh token
     * @return Pair of (accessToken, refreshToken) on success, null on failure
     */
    private fun executeRefresh(refreshToken: String): Pair<String, String>? {
        val requestBody = """{"refreshToken":"$refreshToken"}"""
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("${BuildConfig.API_BASE_URL}auth/refresh")
            .post(requestBody)
            .build()

        val response = refreshClient.newCall(request).execute()

        if (!response.isSuccessful) {
            Timber.w("Refresh endpoint returned ${response.code}")
            response.close()
            return null
        }

        val body = response.body?.string() ?: return null
        response.close()

        return try {
            val parsed = json.parseToJsonElement(body).jsonObject
            val success = parsed["success"]?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false
            if (!success) return null

            val data = parsed["data"]?.jsonObject ?: return null
            val token = data["token"]?.jsonPrimitive?.content ?: return null
            val newRefreshToken = data["refreshToken"]?.jsonPrimitive?.content ?: return null

            Pair(token, newRefreshToken)
        } catch (e: Exception) {
            Timber.e("Failed to parse refresh response: ${e.message}")
            null
        }
    }

    /**
     * Clear all stored tokens and user info, then notify observers.
     */
    private fun clearAndNotify() {
        runBlocking {
            tokenStore.clearAuthToken()
            tokenStore.clearRefreshToken()
            tokenStore.clearUserInfo()
        }
        _authExpiredEvent.tryEmit(Unit)
    }
}
