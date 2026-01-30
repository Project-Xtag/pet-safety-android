package com.petsafety.app.data.network

import com.petsafety.app.data.config.ConfigurationManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * OkHttp Interceptor that adds Firebase App Check token to API requests
 *
 * The App Check token verifies that requests come from a legitimate app instance,
 * protecting the backend API from abuse.
 *
 * The token is added as the `X-Firebase-AppCheck` header.
 */
class AppCheckInterceptor @Inject constructor(
    private val configManager: ConfigurationManager
) : Interceptor {

    companion object {
        private const val HEADER_APP_CHECK = "X-Firebase-AppCheck"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip adding App Check for certain requests if needed
        // (e.g., public endpoints that don't require verification)

        // Get App Check token (blocking call since OkHttp interceptors are synchronous)
        val appCheckToken = runBlocking {
            configManager.getAppCheckToken(forceRefresh = false)
        }

        val newRequest = if (appCheckToken != null) {
            originalRequest.newBuilder()
                .header(HEADER_APP_CHECK, appCheckToken)
                .build()
        } else {
            // Continue without App Check header if token unavailable
            // Backend should handle missing tokens appropriately
            originalRequest
        }

        return chain.proceed(newRequest)
    }
}
