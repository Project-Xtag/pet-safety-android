package com.petsafety.app.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.petsafety.app.BuildConfig
import com.petsafety.app.data.local.AuthTokenStore
import io.sentry.android.okhttp.SentryOkHttpInterceptor
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

@kotlinx.serialization.ExperimentalSerializationApi
object ApiClient {
    /**
     * Certificate pins for senra.pet
     *
     * These are SHA-256 hashes of the Subject Public Key Info (SPKI) of trusted certificates.
     * To generate a pin from a certificate:
     * openssl s_client -connect senra.pet:443 | openssl x509 -pubkey -noout | \
     *   openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
     *
     * Include multiple pins for certificate rotation (primary + backup).
     *
     * Both `api.senra.pet` (the bootstrap host baked into BuildConfig) and
     * `*.senra.pet` (any other single-level subdomain ApiBaseUrlInterceptor
     * may rewrite the request to via Remote Config) are pinned so a runtime
     * host swap doesn't break TLS validation. All senra.pet hosts share the
     * Amazon ACM intermediate so the same SPKI hashes apply.
     */
    private val certificatePinner = CertificatePinner.Builder()
        // Amazon RSA 2048 M01 (Intermediate CA) — stable for years
        .add("api.senra.pet", "sha256/DxH4tt40L+eduF6szpY6TONlxhZhBd+pJ9wbHlQ2fuw=")
        .add("*.senra.pet", "sha256/DxH4tt40L+eduF6szpY6TONlxhZhBd+pJ9wbHlQ2fuw=")
        // Amazon Root CA 1 — stable indefinitely
        .add("api.senra.pet", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=")
        .add("*.senra.pet", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=")
        .build()

    /**
     * Create ApiService with optional App Check + base-URL interceptors.
     *
     * @param tokenStore Authentication token management.
     * @param appCheckInterceptor Optional Firebase App Check token injector.
     * @param baseUrlInterceptor Optional Remote Config-driven host rewriter
     *     (M3). When supplied, every outbound request's host is replaced
     *     with whatever ConfigurationManager.apiBaseUrl currently advertises
     *     so we can flip endpoints without rebuilding Retrofit.
     * @param refreshUrlProvider Lambda returning the auth-refresh endpoint
     *     to use when TokenAuthenticator handles a 401. Reads
     *     ConfigurationManager.apiBaseUrl when wired through Hilt; falls
     *     back to BuildConfig.API_BASE_URL otherwise.
     */
    fun create(
        tokenStore: AuthTokenStore,
        appCheckInterceptor: AppCheckInterceptor? = null,
        baseUrlInterceptor: ApiBaseUrlInterceptor? = null,
        refreshUrlProvider: () -> String = { "${BuildConfig.API_BASE_URL}auth/refresh" },
    ): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))

        // Add base-URL rewrite interceptor BEFORE App Check / Sentry so that
        // downstream interceptors see the final URL. Cert pinning runs on
        // the network stage (after all application interceptors) so it
        // sees the rewritten host.
        baseUrlInterceptor?.let { clientBuilder.addInterceptor(it) }

        // Add App Check interceptor if provided (adds X-Firebase-AppCheck header)
        appCheckInterceptor?.let { clientBuilder.addInterceptor(it) }

        // Add certificate pinning in release builds only
        // This prevents MITM attacks by validating server certificate public key
        if (!BuildConfig.DEBUG) {
            clientBuilder.certificatePinner(certificatePinner)
        }

        // Sentry OkHttp interceptor for automatic breadcrumbs and HTTP span tracking
        clientBuilder.addInterceptor(SentryOkHttpInterceptor())

        val client = clientBuilder
            .addInterceptor(logging)
            .authenticator(TokenAuthenticator(tokenStore, refreshUrlProvider))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
