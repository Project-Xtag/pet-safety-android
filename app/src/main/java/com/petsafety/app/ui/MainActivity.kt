package com.petsafety.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.mutableStateOf
import com.petsafety.app.data.notifications.NotificationHelper
import com.petsafety.app.ui.theme.PetSafetyTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Notification data from FCM push notification tap
 */
data class NotificationData(
    val type: String,
    val petId: String?,
    val alertId: String?,
    val scanId: String?,
    val sightingId: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isApproximate: Boolean
)

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val deepLinkCodeState = mutableStateOf<String?>(null)
    private val notificationDataState = mutableStateOf<NotificationData?>(null)
    val checkoutResultState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        Log.d(TAG, "=== MainActivity.onCreate START ===")
        super.onCreate(savedInstanceState)
        Log.d(TAG, "=== handleIntent ===")
        handleIntent(intent)
        Log.d(TAG, "=== setContent ===")
        setContent {
            PetSafetyTheme {
                PetSafetyApp(
                    pendingQrCode = deepLinkCodeState.value,
                    onQrCodeHandled = { deepLinkCodeState.value = null },
                    pendingNotification = notificationDataState.value,
                    onNotificationHandled = { notificationDataState.value = null },
                    checkoutResult = checkoutResultState.value,
                    onCheckoutResultHandled = { checkoutResultState.value = null }
                )
            }
        }
        Log.d(TAG, "=== MainActivity.onCreate END ===")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        // Check for notification extras first
        val notificationType = intent.getStringExtra(NotificationHelper.EXTRA_NOTIFICATION_TYPE)
        if (notificationType != null) {
            Log.d(TAG, "Handling notification intent: type=$notificationType")
            notificationDataState.value = NotificationData(
                type = notificationType,
                petId = intent.getStringExtra(NotificationHelper.EXTRA_PET_ID),
                alertId = intent.getStringExtra(NotificationHelper.EXTRA_ALERT_ID),
                scanId = intent.getStringExtra(NotificationHelper.EXTRA_SCAN_ID),
                sightingId = intent.getStringExtra(NotificationHelper.EXTRA_SIGHTING_ID),
                latitude = if (intent.hasExtra(NotificationHelper.EXTRA_LATITUDE)) {
                    intent.getDoubleExtra(NotificationHelper.EXTRA_LATITUDE, 0.0)
                } else null,
                longitude = if (intent.hasExtra(NotificationHelper.EXTRA_LONGITUDE)) {
                    intent.getDoubleExtra(NotificationHelper.EXTRA_LONGITUDE, 0.0)
                } else null,
                isApproximate = intent.getBooleanExtra(NotificationHelper.EXTRA_IS_APPROXIMATE, false)
            )
            return
        }

        // Check for checkout deep link
        val checkoutResult = extractCheckoutResult(intent)
        if (checkoutResult != null) {
            Log.d(TAG, "Handling checkout deep link: $checkoutResult")
            checkoutResultState.value = checkoutResult
            return
        }

        // Check for QR deep link
        deepLinkCodeState.value = extractQrCode(intent)
    }

    private fun extractQrCode(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return when (data.scheme) {
            "senra" -> data.lastPathSegment
            "https" -> extractQrFromHttps(data)
            else -> null
        }
    }

    private fun extractCheckoutResult(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if (data.scheme == "senra" && data.host == "checkout") {
            return data.lastPathSegment // "success" or "cancelled"
        }
        return null
    }

    private fun extractQrFromHttps(uri: Uri): String? {
        if (uri.host != "senra.pet") return null
        val segments = uri.pathSegments
        if (segments.size >= 2 && segments[0] == "qr") {
            return segments[1]
        }
        return null
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
