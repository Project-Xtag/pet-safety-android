package com.petsafety.app.data.fcm

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
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
        Log.d(TAG, "FCM token refreshed: ${token.take(20)}...")

        // Store token locally and register with backend
        serviceScope.launch {
            try {
                fcmRepository.saveTokenLocally(token)
                fcmRepository.registerToken(token)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register FCM token", e)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM message received from: ${remoteMessage.from}")

        val data = remoteMessage.data
        val notificationType = data["type"]

        Log.d(TAG, "Notification type: $notificationType, data: $data")

        when (notificationType) {
            "PET_SCANNED" -> handleTagScannedNotification(data)
            "MISSING_PET_ALERT" -> handleMissingPetAlert(data)
            "PET_FOUND" -> handlePetFoundNotification(data)
            "SIGHTING_REPORTED" -> handleSightingNotification(data)
            else -> {
                // Handle generic notification (fallback to title/body)
                remoteMessage.notification?.let { notification ->
                    notificationHelper.showNotification(
                        title = notification.title ?: "Pet Safety",
                        body = notification.body ?: ""
                    )
                }
            }
        }
    }

    private fun handleTagScannedNotification(data: Map<String, String>) {
        val petName = data["pet_name"] ?: "Your pet"
        val scanId = data["scan_id"]
        val petId = data["pet_id"]
        val locationType = data["location_type"] ?: "none"
        val latitude = data["latitude"]?.toDoubleOrNull()
        val longitude = data["longitude"]?.toDoubleOrNull()
        val address = data["address"]

        val locationInfo = when (locationType) {
            "precise" -> address ?: "Location shared"
            "approximate" -> "Approximate location (~500m)"
            else -> null
        }

        val body = if (locationInfo != null) {
            "$petName's tag was scanned! $locationInfo"
        } else {
            "$petName's tag was scanned!"
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
            title = "Tag Scanned",
            body = body,
            petId = petId,
            scanId = scanId,
            petName = petName,
            location = location
        )
    }

    private fun handleMissingPetAlert(data: Map<String, String>) {
        val petName = data["pet_name"] ?: "A pet"
        val alertId = data["alert_id"]
        val address = data["address"] ?: "your area"

        notificationHelper.showMissingPetAlert(
            title = "Missing Pet Nearby",
            body = "$petName is missing in $address. Keep an eye out!",
            alertId = alertId,
            petName = petName
        )
    }

    private fun handlePetFoundNotification(data: Map<String, String>) {
        val petName = data["pet_name"] ?: "Your pet"
        val alertId = data["alert_id"]
        val petId = data["pet_id"]

        notificationHelper.showPetFoundNotification(
            title = "Pet Found!",
            body = "Great news! $petName has been found!",
            petId = petId,
            alertId = alertId,
            petName = petName
        )
    }

    private fun handleSightingNotification(data: Map<String, String>) {
        val petName = data["pet_name"] ?: "Your pet"
        val alertId = data["alert_id"]
        val sightingId = data["sighting_id"]
        val latitude = data["latitude"]?.toDoubleOrNull()
        val longitude = data["longitude"]?.toDoubleOrNull()
        val address = data["address"]

        val body = if (address != null) {
            "$petName was spotted at $address!"
        } else {
            "$petName was spotted! Check the sighting location."
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
            title = "Sighting Reported",
            body = body,
            alertId = alertId,
            sightingId = sightingId,
            petName = petName,
            location = location
        )
    }

    companion object {
        private const val TAG = "FCMService"
    }
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
