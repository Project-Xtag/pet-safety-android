package com.petsafety.app.data.network

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.petsafety.app.BuildConfig
import com.petsafety.app.data.local.AuthTokenStore
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
     * Certificate pins for pet-er.app
     *
     * These are SHA-256 hashes of the Subject Public Key Info (SPKI) of trusted certificates.
     * To generate a pin from a certificate:
     * openssl s_client -connect pet-er.app:443 | openssl x509 -pubkey -noout | \
     *   openssl pkey -pubin -outform der | openssl dgst -sha256 -binary | openssl enc -base64
     *
     * Include multiple pins for certificate rotation (primary + backup).
     */
    private val certificatePinner = CertificatePinner.Builder()
        // Primary certificate pin (pet-er.app current certificate)
        // TODO: Replace with actual hash from production certificate
        .add("pet-er.app", "sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=")
        // Backup pin (for certificate rotation)
        .add("pet-er.app", "sha256/CCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC=")
        .build()

    /**
     * Create ApiService with optional App Check interceptor
     *
     * @param tokenStore For authentication token management
     * @param appCheckInterceptor Optional interceptor for Firebase App Check token injection
     */
    fun create(tokenStore: AuthTokenStore, appCheckInterceptor: AppCheckInterceptor? = null): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))

        // Add App Check interceptor if provided (adds X-Firebase-AppCheck header)
        appCheckInterceptor?.let { clientBuilder.addInterceptor(it) }

        // Add certificate pinning in release builds only
        // This prevents MITM attacks by validating server certificate public key
        if (!BuildConfig.DEBUG) {
            clientBuilder.certificatePinner(certificatePinner)
        }

        val client = clientBuilder
            .addInterceptor(logging)
            .authenticator(TokenAuthenticator(tokenStore))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return retrofit.create(ApiService::class.java)
    }
}
