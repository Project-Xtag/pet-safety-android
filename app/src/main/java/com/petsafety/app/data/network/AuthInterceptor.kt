package com.petsafety.app.data.network

import com.petsafety.app.data.local.AuthTokenStore
import okhttp3.Interceptor
import okhttp3.Response
import java.util.Locale

class AuthInterceptor(
    private val tokenStore: AuthTokenStore
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStore.authToken.value
        val builder = chain.request().newBuilder()
            .header("Accept-Language", Locale.getDefault().language)

        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(builder.build())
    }
}
