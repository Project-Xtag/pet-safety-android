package com.petsafety.app.data.fcm

import timber.log.Timber
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.petsafety.app.R
import com.petsafety.app.data.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging Service
 *
 * Handles incoming push notifications and FCM token refresh.
 * Supports notification types:
 * - PET_SCANNED: Tag was scanned, optionally with location
 * - MISSING_PET_ALERT: Pet reported missing nearby
 * - PET_FOUND: Missing pet has been found
 * - SIGHTING_REPORTED: New sighting for user's missing pet
 */
@AndroidEntryPoint
class PetSafetyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var fcmRepository: FCMRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM token refreshed: ${token.take(20)}...")

        // Store token locally and register with backend
        serviceScope.launch {
            try {
                fcmRepository.saveTokenLocally(token)
                fcmRepository.registerToken(token)
            } catch (e: Exception) {
                Timber.e("Failed to register FCM token", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Timber.d("FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notificationType = data["type"]

        Timber.d("Notification type: $notificationType, data: $data")

        when (notificationType) {
            "PET_SCANNED" -> handleTagScannedNotification(data)
            "MISSING_PET_ALERT" -> handleMissingPetAlert(data)
            "PET_FOUND" -> handlePetFoundNotification(data)
            "SIGHTING_REPORTED" -> handleSightingNotification(data)
            else -> {
                // Handle generic notification (fallback to title/body)
                remoteMessage.notification?.let { notification ->
                    notificationHelper.showNotification(
                        title = notification.title ?: getString(R.string.notif_fallback_title),
                        body = notification.body ?: ""
                    )
                }
            }
        }
    }

    private fun handleTagScannedNotification(data: Map<String, String>) {
        val petName = data["pet_name"] ?: getString(R.string.notif_fallback_pet_name)
        val scanId = data["scan_id"]
        val petId = data["pet_id"]
        val locationType = data["location_type"] ?: "none"
        val latitude = data["latitude"]?.toDoubleOrNull()
        val longitude = data["longitude"]?.toDoubleOrNull()
        val address = data["address"]

        val locationInfo = when (locationType) {
            "precise" -> address ?: getString(R.string.notif_location_shared)
            "approximate" -> getString(R.string.notif_location_approximate)
            else -> null
        }

        val body = if (locationInfo != null) {
            getString(R.string.notif_tag_scanned_body_location, petName, locationInfo)
        } else {
            getString(R.string.notif_tag_scanned_body, petName)
        }

        val location = if (latitude != null && longitude != null) {
            NotificationLocation(
                latitude = latitude,
                longitude = longitude,
                isApproximate = locationType == "approximate",
                address = address
            )
        } else null

        notificationHelper.showTagScannedNotification(
            title = getString(R.string.notif_tag_scanned_title),
            body = body,
            petId = petId,
            scanId = scanId,
            petName = petName,
            location = location
        )
    }

    private fun handleMissingPetAlert(data: Map<String, String>) {
        val petName = data["pet_name"] ?: getString(R.string.notif_fallback_a_pet)
        val alertId = data["alert_id"]
        val address = data["address"] ?: getString(R.string.notif_fallback_your_area)

        notificationHelper.showMissingPetAlert(
            title = getString(R.string.notif_missing_pet_title),
            body = getString(R.string.notif_missing_pet_body, petName, address),
            alertId = alertId,
            petName = petName
        )
    }

    private fun handlePetFoundNotification(data: Map<String, String>) {
        val petName = data["pet_name"] ?: getString(R.string.notif_fallback_pet_name)
        val alertId = data["alert_id"]
        val petId = data["pet_id"]

        notificationHelper.showPetFoundNotification(
            title = getString(R.string.notif_pet_found_title),
            body = getString(R.string.notif_pet_found_body, petName),
            petId = petId,
            alertId = alertId,
            petName = petName
        )
    }

    private fun handleSightingNotification(data: Map<String, String>) {
        val petName = data["pet_name"] ?: getString(R.string.notif_fallback_pet_name)
        val alertId = data["alert_id"]
        val sightingId = data["sighting_id"]
        val latitude = data["latitude"]?.toDoubleOrNull()
        val longitude = data["longitude"]?.toDoubleOrNull()
        val address = data["address"]

        val body = if (address != null) {
            getString(R.string.notif_sighting_body_location, petName, address)
        } else {
            getString(R.string.notif_sighting_body_no_location, petName)
        }

        val location = if (latitude != null && longitude != null) {
            NotificationLocation(
                latitude = latitude,
                longitude = longitude,
                isApproximate = false,
                address = address
            )
        } else null

        notificationHelper.showSightingNotification(
            title = getString(R.string.notif_sighting_title),
            body = body,
            alertId = alertId,
            sightingId = sightingId,
            petName = petName,
            location = location
        )
    }

    companion object
}

/**
 * Location data from push notification
 */
data class NotificationLocation(
    val latitude: Double,
    val longitude: Double,
    val isApproximate: Boolean,
    val address: String? = null
)
