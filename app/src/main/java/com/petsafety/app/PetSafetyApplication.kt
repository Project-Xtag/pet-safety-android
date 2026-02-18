package com.petsafety.app

import android.app.Application
import android.os.Build
import timber.log.Timber
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

        // Plant Timber debug tree only in debug builds
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.d("=== Application.onCreate START ===")

        // Skip Firebase on emulators - Firebase Installation Service doesn't work reliably
        if (isEmulator()) {
            Timber.w("Running on emulator - Firebase/FCM disabled")
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
            Timber.w("Google Play Services not available - Firebase/FCM disabled")
        }

        Timber.d("=== Scheduling periodic sync ===")
        // Schedule periodic sync
        syncScheduler.schedulePeriodicSync()
        Timber.d("=== Application.onCreate END ===")
    }

    /**
     * Initialize Sentry error tracking with DSN from Remote Config
     */
    private fun initializeSentry() {
        val dsn = configurationManager.sentryDSN.value.trim()

        if (dsn.isEmpty()) {
            Timber.w("Sentry DSN not configured in Remote Config - error tracking disabled")
            return
        }

        SentryAndroid.init(this) { options ->
            options.dsn = dsn
            options.environment = if (BuildConfig.DEBUG) "development" else "production"
            options.isDebug = BuildConfig.DEBUG
            options.tracesSampleRate = 0.1
            options.isEnableAutoSessionTracking = true
            options.isAttachScreenshot = BuildConfig.DEBUG
            options.isAttachViewHierarchy = BuildConfig.DEBUG

            // Filter out noisy client errors and strip sensitive data
            options.setBeforeSend { event, _ ->
                // Drop common client errors (matching iOS filter)
                val exceptionValue = event.exceptions?.firstOrNull()?.value ?: ""
                if (exceptionValue.contains("unauthorized", ignoreCase = true) ||
                    exceptionValue.contains("401") ||
                    exceptionValue.contains("400") ||
                    exceptionValue.contains("403") ||
                    exceptionValue.contains("404")
                ) {
                    return@setBeforeSend null
                }
                // Strip user email
                event.user?.email = null
                event
            }
        }

        Timber.d("Sentry initialized successfully")
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
                Timber.w("Firebase initialization returned null")
                return
            }
            Timber.d("Firebase initialized")

            // Enable auto-init for messaging (disabled in manifest for safe startup)
            try {
                FirebaseMessaging.getInstance().isAutoInitEnabled = true
            } catch (e: Exception) {
                Timber.w("Could not enable FCM auto-init: ${e.message}")
            }

            // Get initial FCM token (non-blocking, failure is ok)
            applicationScope.launch {
                try {
                    val token = fcmRepository.getCurrentToken()
                    if (token != null) {
                        Timber.d("FCM token: ${token.take(20)}...")
                    } else {
                        Timber.w("FCM token not available")
                    }
                } catch (e: Exception) {
                    Timber.w("FCM not available: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Timber.w("Firebase not available: ${e.message}")
        }
    }

    companion object
}
