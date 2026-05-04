package com.petsafety.app.data.network

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

/**
 * OkHttp interceptor that rewrites every outbound request's scheme + host +
 * port to the URL returned by [baseUrlProvider] at intercept time.
 *
 * Why an interceptor (and not Retrofit's `.baseUrl()`):
 *
 * Retrofit fixes the base URL at construction. To support runtime endpoint
 * switches via Firebase Remote Config (e.g. for region routing, blue/green
 * cutover, or emergency fallback) without rebuilding Retrofit — which would
 * invalidate every in-flight request — we instead leave Retrofit pinned to
 * the bootstrap URL and rewrite the host on each call. The path, query
 * string, headers, and body all pass through unchanged; only scheme/host/
 * port are taken from the live provider.
 *
 * If [baseUrlProvider] returns a malformed URL we leave the request alone
 * and warn — better an in-progress request hits the bootstrap URL than the
 * app crashes mid-flow because of a bad config push.
 *
 * Cert pinning interacts with this rewrite: any host the provider returns
 * MUST be covered by [ApiClient]'s CertificatePinner patterns, otherwise
 * the connection will be killed by pinning. ApiClient pins both
 * `api.senra.pet` and `*.senra.pet` so any single-level subdomain swap
 * succeeds; multi-level subdomains (api.staging.senra.pet) would need
 * another pinner entry.
 */
class ApiBaseUrlInterceptor(
    private val baseUrlProvider: () -> String,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val targetBase = baseUrlProvider().trim()

        if (targetBase.isEmpty()) {
            return chain.proceed(request)
        }

        val targetUrl = targetBase.toHttpUrlOrNull()
        if (targetUrl == null) {
            Timber.w("ApiBaseUrlInterceptor: ignoring malformed base URL '$targetBase'")
            return chain.proceed(request)
        }

        // Compose the new URL: scheme + host + port from target, everything
        // else (path, query, fragment) from the original request. The
        // original request's path is what Retrofit built from its baseUrl
        // + endpoint annotations, so it already has the right shape.
        val originalUrl = request.url
        val rewritten = originalUrl.newBuilder()
            .scheme(targetUrl.scheme)
            .host(targetUrl.host)
            .port(targetUrl.port)
            .build()

        // Cheap fast-path: if nothing actually changed, proceed unmodified
        // so we don't churn Request objects on every call.
        if (rewritten == originalUrl) {
            return chain.proceed(request)
        }

        return chain.proceed(request.newBuilder().url(rewritten).build())
    }
}
