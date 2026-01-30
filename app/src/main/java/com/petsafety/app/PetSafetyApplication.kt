package com.petsafety.app

import android.app.Application
import android.os.Build
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.petsafety.app.data.config.ConfigurationManager
import com.petsafety.app.data.fcm.FCMRepository
import com.petsafety.app.data.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PetSafetyApplication : Application() {
    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var fcmRepository: FCMRepository

    @Inject
    lateinit var configurationManager: ConfigurationManager

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "=== Application.onCreate START ===")

        // Skip Firebase on emulators - Firebase Installation Service doesn't work reliably
        if (isEmulator()) {
            Log.w(TAG, "Running on emulator - Firebase/FCM disabled")
        } else if (isGooglePlayServicesAvailable()) {
            // Initialize Firebase first (required before any Firebase APIs)
            initializeFirebase()

            // Configure App Check after Firebase init
            // This protects Firebase APIs from abuse
            ConfigurationManager.configureAppCheck(this)

            // Fetch remote configuration and initialize Sentry
            applicationScope.launch {
                configurationManager.fetchConfiguration()
                initializeSentry()
            }
        } else {
            Log.w(TAG, "Google Play Services not available - Firebase/FCM disabled")
        }

        Log.d(TAG, "=== Scheduling periodic sync ===")
        // Schedule periodic sync
        syncScheduler.schedulePeriodicSync()
        Log.d(TAG, "=== Application.onCreate END ===")
    }

    /**
     * Initialize Sentry error tracking with DSN from Remote Config
     */
    private fun initializeSentry() {
        val dsn = configurationManager.sentryDSN.value

        if (dsn.isEmpty()) {
            Log.w(TAG, "Sentry DSN not configured in Remote Config - error tracking disabled")
            return
        }

        SentryAndroid.init(this) { options ->
            options.dsn = dsn
            options.environment = if (BuildConfig.DEBUG) "development" else "production"
            options.isDebug = BuildConfig.DEBUG
            options.tracesSampleRate = 0.2 // 20% of transactions for performance monitoring

            // Strip sensitive data before sending
            options.setBeforeSend { event, _ ->
                event.user?.email = null
                event
            }
        }

        Log.d(TAG, "Sentry initialized successfully")
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || "google_sdk" == Build.PRODUCT
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu"))
    }

    private fun isGooglePlayServicesAvailable(): Boolean {
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        return resultCode == ConnectionResult.SUCCESS
    }

    private fun initializeFirebase() {
        try {
            val app = FirebaseApp.initializeApp(this)
            if (app == null) {
                Log.w(TAG, "Firebase initialization returned null")
                return
            }
            Log.d(TAG, "Firebase initialized")

            // Enable auto-init for messaging (disabled in manifest for safe startup)
            try {
                FirebaseMessaging.getInstance().isAutoInitEnabled = true
            } catch (e: Exception) {
                Log.w(TAG, "Could not enable FCM auto-init: ${e.message}")
            }

            // Get initial FCM token (non-blocking, failure is ok)
            applicationScope.launch {
                try {
                    val token = fcmRepository.getCurrentToken()
                    if (token != null) {
                        Log.d(TAG, "FCM token: ${token.take(20)}...")
                    } else {
                        Log.w(TAG, "FCM token not available")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "FCM not available: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Firebase not available: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "PetSafetyApp"
    }
}
