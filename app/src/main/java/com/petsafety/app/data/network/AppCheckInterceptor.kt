package com.petsafety.app.data.network

import com.petsafety.app.data.config.ConfigurationManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * OkHttp Interceptor that adds Firebase App Check token to API requests.
 *
 * The App Check token verifies that requests come from a legitimate app instance,
 * protecting the backend API from abuse.
 *
 * Uses a cached token with TTL to avoid blocking OkHttp threads on every request.
 * Token is refreshed in the background; if unavailable, requests proceed without it.
 */
class AppCheckInterceptor @Inject constructor(
    private val configManager: ConfigurationManager
) : Interceptor {

    companion object {
        private const val HEADER_APP_CHECK = "X-Firebase-AppCheck"
        private const val TOKEN_TTL_MS = 30 * 60 * 1000L // 30 minutes
    }

    private data class CachedToken(val token: String, val expiresAt: Long)

    private val cachedToken = AtomicReference<CachedToken?>(null)

    /**
     * Pre-fetch the App Check token from a coroutine context (e.g., at app startup).
     * This avoids any blocking calls from OkHttp interceptor threads.
     */
    suspend fun prefetchToken() {
        try {
            val token = configManager.getAppCheckToken(forceRefresh = false)
            if (token != null) {
                cachedToken.set(CachedToken(token, System.currentTimeMillis() + TOKEN_TTL_MS))
            }
        } catch (e: Exception) {
            Timber.w("Failed to prefetch App Check token: ${e.message}")
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val cached = cachedToken.get()
        val appCheckToken = if (cached != null && System.currentTimeMillis() < cached.expiresAt) {
            cached.token
        } else {
            // Cache expired or empty — fetch synchronously as last resort.
            // This is the fallback; prefetchToken() should be called at app start.
            try {
                val token = runBlocking {
                    configManager.getAppCheckToken(forceRefresh = false)
                }
                if (token != null) {
                    cachedToken.set(CachedToken(token, System.currentTimeMillis() + TOKEN_TTL_MS))
                }
                token
            } catch (e: Exception) {
                Timber.w("App Check token fetch failed: ${e.message}")
                null
            }
        }

        val newRequest = if (appCheckToken != null) {
            originalRequest.newBuilder()
                .header(HEADER_APP_CHECK, appCheckToken)
                .build()
        } else {
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
