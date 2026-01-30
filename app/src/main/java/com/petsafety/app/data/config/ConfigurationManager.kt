package com.petsafety.app.data.config

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.petsafety.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ConfigurationManager - Centralized configuration management for Android
 *
 * Responsibilities:
 * - Configure Firebase App Check for API protection
 * - Fetch runtime configuration from Firebase Remote Config
 * - Provide reactive access to sensitive config values (Sentry DSN, API URLs)
 *
 * Usage:
 * 1. Call `FirebaseApp.initializeApp(context)` first
 * 2. Call `ConfigurationManager.configureAppCheck(context)` to set up App Check
 * 3. Inject ConfigurationManager via Hilt and call `fetchConfiguration()`
 * 4. Observe config values via StateFlows (sentryDSN, apiBaseUrl, sseBaseUrl)
 */
@Singleton
class ConfigurationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "ConfigurationManager"

        /**
         * Configure Firebase App Check
         *
         * IMPORTANT: This MUST be called AFTER Firebase.initializeApp(context)
         *
         * In Debug builds, uses the debug provider (works on emulator).
         * In Release builds, uses Play Integrity (requires physical device with Play Services).
         */
        fun configureAppCheck(context: Context) {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            val factory = if (BuildConfig.DEBUG) {
                Log.d(TAG, "Using App Check debug provider")
                DebugAppCheckProviderFactory.getInstance()
            } else {
                Log.d(TAG, "Using App Check Play Integrity provider")
                PlayIntegrityAppCheckProviderFactory.getInstance()
            }

            firebaseAppCheck.installAppCheckProviderFactory(factory)
        }
    }

    // Lazy initialization - only accessed after Firebase is initialized
    private val remoteConfig: FirebaseRemoteConfig by lazy {
        val config = FirebaseRemoteConfig.getInstance()

        // Configure Remote Config settings on first access
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 3600L)
            .build()
        config.setConfigSettingsAsync(configSettings)
        config.setDefaultsAsync(defaults)

        config
    }

    // Configuration values as StateFlows for reactive updates
    private val _sentryDSN = MutableStateFlow("")
    val sentryDSN: StateFlow<String> = _sentryDSN.asStateFlow()

    private val _apiBaseUrl = MutableStateFlow(BuildConfig.API_BASE_URL)
    val apiBaseUrl: StateFlow<String> = _apiBaseUrl.asStateFlow()

    private val _sseBaseUrl = MutableStateFlow(BuildConfig.SSE_BASE_URL)
    val sseBaseUrl: StateFlow<String> = _sseBaseUrl.asStateFlow()

    private val _isConfigured = MutableStateFlow(false)
    val isConfigured: StateFlow<Boolean> = _isConfigured.asStateFlow()

    // Default values for offline/fallback scenarios
    private val defaults = mapOf(
        "sentry_dsn_android" to "",
        "api_base_url" to BuildConfig.API_BASE_URL,
        "sse_base_url" to BuildConfig.SSE_BASE_URL
    )

    /**
     * Fetch configuration from Firebase Remote Config
     *
     * Call this after Firebase is initialized. Safe to call multiple times.
     * Uses cached values if fetch fails or is rate-limited.
     *
     * @return Result.success if fetch succeeded, Result.failure with exception otherwise
     */
    suspend fun fetchConfiguration(): Result<Unit> {
        return try {
            val activated = remoteConfig.fetchAndActivate().await()
            Log.d(TAG, "Remote Config fetch and activate: $activated")

            updateConfigValues()
            _isConfigured.value = true

            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Remote Config fetch failed: ${e.message}")

            // Use cached/default values on failure
            updateConfigValues()
            _isConfigured.value = true

            Result.failure(e)
        }
    }

    /**
     * Update local StateFlows from Remote Config values
     */
    private fun updateConfigValues() {
        // Sentry DSN
        val dsn = remoteConfig.getString("sentry_dsn_android")
        _sentryDSN.value = dsn

        // API Base URL (use default if empty)
        val apiUrl = remoteConfig.getString("api_base_url")
        _apiBaseUrl.value = apiUrl.ifEmpty { defaults["api_base_url"]!! }

        // SSE Base URL (use default if empty)
        val sseUrl = remoteConfig.getString("sse_base_url")
        _sseBaseUrl.value = sseUrl.ifEmpty { defaults["sse_base_url"]!! }

        Log.d(TAG, "Config values updated:")
        Log.d(TAG, "  - sentryDSN: ${if (dsn.isEmpty()) "(not configured)" else "(configured)"}")
        Log.d(TAG, "  - apiBaseUrl: ${_apiBaseUrl.value}")
        Log.d(TAG, "  - sseBaseUrl: ${_sseBaseUrl.value}")
    }

    /**
     * Get a Firebase App Check token for backend API calls
     *
     * The token should be included in the `X-Firebase-AppCheck` header
     * for requests to protected backend endpoints.
     *
     * @param forceRefresh If true, forces a new token even if cached token is valid
     * @return The App Check token, or null if retrieval fails
     */
    suspend fun getAppCheckToken(forceRefresh: Boolean = false): String? {
        return try {
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            val token = firebaseAppCheck.getAppCheckToken(forceRefresh).await()
            token.token
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get App Check token: ${e.message}")
            null
        }
    }
}
