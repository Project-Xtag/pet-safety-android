package com.petsafety.app.data.network

import timber.log.Timber
import com.petsafety.app.BuildConfig
import com.petsafety.app.data.local.AuthTokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
import java.util.concurrent.atomic.AtomicBoolean

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
 * 4. If refresh fails, clear all tokens and emit authExpiredEvent **once**.
 *
 * Expiry-notification idempotency:
 *   Once tokens are cleared, any still-in-flight request will return 401 and
 *   re-enter this authenticator. Without a guard, each re-entry would re-emit
 *   authExpiredEvent — and downstream handlers that call the server (e.g. FCM
 *   unregister during logout) would feed that loop forever. We emit the event
 *   at most once per "session loss" via `hasNotifiedExpiry`. The guard is
 *   re-armed when tokens are next saved (fresh login via [resetExpiryGuard]
 *   or successful refresh inside [authenticate]).
 */
class TokenAuthenticator(
    private val tokenStore: AuthTokenStore,
    private val refreshUrl: String = "${BuildConfig.API_BASE_URL}auth/refresh"
) : Authenticator {

    companion object {
        // Shared flow to notify observers when auth expires
        private val _authExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val authExpiredEvent: SharedFlow<Unit> = _authExpiredEvent.asSharedFlow()

        // One-shot guard so the expiry event fires at most once per session loss.
        private val hasNotifiedExpiry = AtomicBoolean(false)

        /**
         * Re-arm the expiry notification after a fresh login.
         * Call from the auth repository after new tokens are persisted.
         */
        fun resetExpiryGuard() {
            hasNotifiedExpiry.set(false)
        }

        /**
         * Test hook: force an expiry emission without going through the 401
         * machinery. Not to be called from production code.
         */
        @androidx.annotation.VisibleForTesting(otherwise = androidx.annotation.VisibleForTesting.NONE)
        internal fun emitExpiryForTest() {
            _authExpiredEvent.tryEmit(Unit)
        }
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Lightweight OkHttpClient for refresh calls only.
     * No authenticator attached — prevents infinite retry loops.
     */
    private val refreshClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry if we already attempted a refresh for this request
        if (response.request.header("Retry-Auth") != null) {
            return null
        }

        synchronized(this) {
            // Fast path: session already ended. Don't attempt refresh or re-emit
            // the expiry event — that's exactly the loop we're preventing.
            if (hasNotifiedExpiry.get()) {
                return null
            }

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
                    tokenStore.saveTokensSync(newTokens.first, newTokens.second)
                    // Fresh tokens — re-arm so future expiries will be reported.
                    hasNotifiedExpiry.set(false)
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
                // Log structured context. A 500 from the backend is
                // distinct from a parse error locally, but the bare
                // message-only log collapsed them into one bucket.
                val responseCode = response.code
                Timber.e(e, "Token refresh failed (responseCode=$responseCode): ${e.message}")
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
        val jsonBody = JsonObject(mapOf("refreshToken" to JsonPrimitive(refreshToken)))
        val requestBody = jsonBody.toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(refreshUrl)
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
     * Clear all stored tokens and user info, then notify observers **once**.
     *
     * The atomic guard prevents the re-entrant 401-cascade: after tokens are
     * cleared, any follow-up request (e.g. an FCM unregister during logout)
     * re-enters this authenticator, but the guard stops it from looping on
     * the expiry event.
     */
    private fun clearAndNotify() {
        tokenStore.clearAllSync()
        if (hasNotifiedExpiry.compareAndSet(false, true)) {
            _authExpiredEvent.tryEmit(Unit)
        }
    }
}
