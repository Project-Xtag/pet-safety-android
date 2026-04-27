package com.petsafety.app.data.network

import com.petsafety.app.BuildConfig
import com.petsafety.app.data.config.ConfigurationManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * OkHttp Interceptor that adds the Firebase App Check token to API requests.
 *
 * Behaviour matrix (audit H47):
 *
 *   DEBUG build              → never attaches a header, never blocks the request.
 *                              ConfigurationManager.getAppCheckToken returns null
 *                              in DEBUG anyway because the backend doesn't enforce
 *                              and Firebase debug-token 403s are a developer
 *                              ergonomics nightmare.
 *
 *   Release, token available → attaches X-Firebase-AppCheck header.
 *
 *   Release, token missing,
 *     enforcement OFF        → request proceeds without header (legacy fail-open,
 *                              kept while the backend is still unauthenticated —
 *                              tracked under WS8.7 enforcement decision).
 *
 *   Release, token missing,
 *     enforcement ON         → synthesise a 503 Service Unavailable response
 *                              without ever hitting the network. This is the
 *                              fail-closed posture H47 calls for; it ships
 *                              dormant behind a Remote Config flag so backend
 *                              enforcement (WS8.7) can flip it on without an
 *                              app release.
 *
 * The runtime decision is funnelled through an injectable [enforceFailClosed]
 * lambda so unit tests can exercise both branches without touching Firebase.
 */
class AppCheckInterceptor @Inject constructor(
    private val configManager: ConfigurationManager,
) : Interceptor {

    // Hilt requires a single @Inject constructor with no default values, so
    // testability seams are exposed as separate setters with defaults that
    // wire up the production behaviour. Tests can swap them in-place without
    // needing a second constructor.
    private var isDebugBuild: () -> Boolean = { BuildConfig.DEBUG }
    private var enforceFailClosed: () -> Boolean = {
        configManager.shouldEnforceAppCheckClient()
    }

    /** Visible-for-testing seam (audit H47). Production code should not call this. */
    internal fun setBehaviorForTest(
        isDebugBuild: () -> Boolean,
        enforceFailClosed: () -> Boolean,
    ) {
        this.isDebugBuild = isDebugBuild
        this.enforceFailClosed = enforceFailClosed
    }

    companion object {
        private const val HEADER_APP_CHECK = "X-Firebase-AppCheck"
        private const val TOKEN_TTL_MS = 30 * 60 * 1000L // 30 minutes
        internal const val FAIL_CLOSED_BODY =
            """{"success":false,"error":"App attestation unavailable. Please retry."}"""
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

        if (appCheckToken == null && !isDebugBuild() && enforceFailClosed()) {
            Timber.w("Blocking request — App Check token unavailable and fail-closed enforcement is enabled")
            return synthesizeFailClosedResponse(originalRequest)
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

    /**
     * Build a 503 response in-place (no chain.proceed) so the network stack
     * never even dials. Visible to upstream callers as a normal HTTP failure
     * — no special exception type to handle, retry/backoff in callers Just Works.
     */
    private fun synthesizeFailClosedResponse(request: okhttp3.Request): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(503)
            .message("App Check token unavailable")
            .body(FAIL_CLOSED_BODY.toResponseBody("application/json".toMediaType()))
            .build()
    }
}
