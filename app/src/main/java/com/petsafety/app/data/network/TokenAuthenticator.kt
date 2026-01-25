package com.petsafety.app.data.network

import com.petsafety.app.data.local.AuthTokenStore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Handles 401 Unauthorized responses.
 *
 * Since the app uses OTP-based authentication without refresh tokens,
 * when a 401 is received, we clear the stored token and signal that
 * re-authentication is required.
 */
class TokenAuthenticator(
    private val tokenStore: AuthTokenStore
) : Authenticator {

    companion object {
        // Shared flow to notify observers when auth expires
        private val _authExpiredEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val authExpiredEvent: SharedFlow<Unit> = _authExpiredEvent.asSharedFlow()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        // Check if this is already a retry to prevent infinite loops
        if (response.request.header("Retry-Auth") != null) {
            return null
        }

        // Only handle 401 responses
        if (response.code != 401) {
            return null
        }

        synchronized(this) {
            // Check if token was already cleared by another thread
            val currentToken = tokenStore.authToken.value
            if (currentToken.isNullOrBlank()) {
                // Token already cleared, don't retry
                return null
            }

            // Clear the invalid token
            runBlocking {
                tokenStore.clearAuthToken()
                tokenStore.clearUserInfo()
            }

            // Emit auth expired event to notify the app
            _authExpiredEvent.tryEmit(Unit)

            // Don't retry the request - let it fail and the UI will redirect to login
            return null
        }
    }
}
