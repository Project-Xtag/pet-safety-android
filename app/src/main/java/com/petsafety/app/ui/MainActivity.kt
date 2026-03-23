package com.petsafety.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import timber.log.Timber
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import android.preference.PreferenceManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.petsafety.app.data.notifications.NotificationHelper
import com.petsafety.app.ui.theme.PetSafetyTheme
import com.petsafety.app.util.WebUrlHelper
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
    val isApproximate: Boolean,
    val planName: String? = null,
    val daysLeft: String? = null
)

@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    private val deepLinkCodeState = mutableStateOf<String?>(null)
    private val notificationDataState = mutableStateOf<NotificationData?>(null)
    val checkoutResultState = mutableStateOf<String?>(null)
    val checkoutTypeState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        Timber.d("=== MainActivity.onCreate START ===")
        super.onCreate(savedInstanceState)
        // Allow Compose to handle window insets (needed for imePadding to work)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        Timber.d("=== handleIntent ===")
        handleIntent(intent)
        Timber.d("=== setContent ===")
        setContent {
            val prefs = remember { PreferenceManager.getDefaultSharedPreferences(this) }
            val appearanceMode = remember { mutableStateOf(prefs.getString("appearanceMode", "system") ?: "system") }
            val darkTheme = when (appearanceMode.value) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }
            PetSafetyTheme(darkTheme = darkTheme) {
                PetSafetyApp(
                    pendingQrCode = deepLinkCodeState.value,
                    onQrCodeHandled = { deepLinkCodeState.value = null },
                    pendingNotification = notificationDataState.value,
                    onNotificationHandled = { notificationDataState.value = null },
                    checkoutResult = checkoutResultState.value,
                    onCheckoutResultHandled = { checkoutResultState.value = null },
                    checkoutType = checkoutTypeState.value,
                    onCheckoutTypeHandled = { checkoutTypeState.value = null }
                )
            }
        }
        Timber.d("=== MainActivity.onCreate END ===")
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
            Timber.d("Handling notification intent: type=$notificationType")
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
                isApproximate = intent.getBooleanExtra(NotificationHelper.EXTRA_IS_APPROXIMATE, false),
                planName = intent.getStringExtra(NotificationHelper.EXTRA_PLAN_NAME),
                daysLeft = intent.getStringExtra(NotificationHelper.EXTRA_DAYS_LEFT)
            )
            return
        }

        // Check for checkout deep link
        val checkoutResult = extractCheckoutResult(intent)
        if (checkoutResult != null) {
            Timber.d("Handling checkout deep link: ${checkoutResult.first}, type: ${checkoutResult.second}")
            checkoutResultState.value = checkoutResult.first
            checkoutTypeState.value = checkoutResult.second
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

    private fun extractCheckoutResult(intent: Intent?): Pair<String, String?>? {
        val data = intent?.data ?: return null
        if (data.scheme == "senra" && data.host == "checkout") {
            val result = data.lastPathSegment ?: return null // "success" or "cancelled"
            val type = data.getQueryParameter("type") // "subscription", "qr_tag_order", "replacement_shipping"
            return Pair(result, type)
        }
        return null
    }

    private fun extractQrFromHttps(uri: Uri): String? {
        val host = uri.host?.lowercase()
        if (host != "senra.pet" && host != "www.senra.pet") return null
        val path = uri.path ?: return null
        val strippedPath = WebUrlHelper.stripCountryPrefix(path)
        val strippedSegments = strippedPath.trimStart('/').split("/")
        val prefix = strippedSegments.getOrNull(0)?.lowercase()
        if (strippedSegments.size >= 2 && (prefix == "qr" || prefix == "t")) {
            return strippedSegments[1]
        }
        return null
    }

    companion object
}
