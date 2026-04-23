package com.petsafety.app.data.network

import com.petsafety.app.data.local.AuthTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import java.util.Locale

class AuthInterceptor(
    private val tokenStore: AuthTokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.authToken.value
        val builder = chain.request().newBuilder()
            .header("Accept-Language", Locale.getDefault().language)

        if (!token.isNullOrBlank()) {
            if (isValidJwtShape(token)) {
                builder.header("Authorization", "Bearer $token")
            } else {
                // A corrupted SharedPrefs entry used to land here and get
                // shipped as the Authorization header — every request
                // then 401'd and TokenAuthenticator spun trying to
                // refresh from an unrecoverable state. Drop the header;
                // the subsequent 401 on protected routes lets
                // TokenAuthenticator clear session state cleanly through
                // its existing path (which is a suspend context).
                Timber.w("AuthInterceptor: malformed auth token in store, skipping header")
            }
        }

        return chain.proceed(builder.build())
    }

    /**
     * Minimal shape check: a JWT has 3 dot-separated base64url parts.
     * We deliberately don't verify the signature — the server does that
     * authoritatively — but we refuse to ship anything that isn't even
     * the right shape.
     */
    private fun isValidJwtShape(token: String): Boolean {
        val parts = token.split('.')
        if (parts.size != 3) return false
        return parts.all { it.isNotEmpty() && it.all(::isJwtChar) }
    }

    private fun isJwtChar(c: Char): Boolean =
        c.isLetterOrDigit() || c == '-' || c == '_' || c == '='
}
