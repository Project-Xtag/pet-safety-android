package com.petsafety.app.data.fcm

import timber.log.Timber
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.petsafety.app.R
import com.petsafety.app.data.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import io.sentry.Sentry
import io.sentry.SentryLevel
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

        // Pre-fix the per-type handlers below trusted the FCM payload to
        // contain alert_id/pet_id/etc. unchecked. A backend regression
        // shipping a malformed push silently produced a notification that
        // deep-linked nowhere — and ops had no signal anything was wrong.
        // Validate up front, capture missing required fields to Sentry,
        // and drop the notification (matching iOS NotificationHandler).
        if (notificationType != null) {
            val missingField = validatePayload(notificationType, data)
            if (missingField != null) {
                captureMalformedPayload(
                    reason = "missing_$missingField",
                    type = notificationType,
                    data = data
                )
                return
            }
        }

        when (notificationType) {
            "PET_SCANNED", "TAG_INITIAL_SCAN" -> handleTagScannedNotification(data)
            "TAG_ACTIVATED" -> {
                val petName = data["pet_name"] ?: getString(R.string.notif_fallback_pet_name)
                notificationHelper.showNotification(
                    title = data["title"] ?: getString(R.string.notif_tag_activated_title),
                    body = data["body"] ?: getString(R.string.notif_tag_activated_body, petName)
                )
            }
            "MISSING_PET_ALERT" -> handleMissingPetAlert(data)
            "PET_FOUND" -> handlePetFoundNotification(data)
            "SIGHTING_REPORTED" -> handleSightingNotification(data)
            NotificationHelper.TYPE_ALERT_CONFIRMATION -> handleAlertConfirmation(data)
            NotificationHelper.TYPE_PROMO_EXPIRING -> handlePromoExpiring(data)
            NotificationHelper.TYPE_MULTIPLE_SIGHTINGS -> handleMultipleSightings(data)
            "VACCINATION_DUE" -> handleVaccinationDue(data)
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

    /**
     * Telemetry helper: report a malformed FCM payload to Sentry.
     * Logs the keys but not the values (some keys carry PII like
     * pet_name or addresses).
     */
    private fun captureMalformedPayload(
        reason: String,
        type: String,
        data: Map<String, String>
    ) {
        Timber.w("Malformed FCM payload — type=$type reason=$reason keys=${data.keys}")
        if (Sentry.isEnabled()) {
            Sentry.captureMessage("Malformed FCM payload: $type ($reason)") { scope ->
                scope.level = SentryLevel.WARNING
                scope.setTag("operation", "fcm_malformed_payload")
                scope.setTag("notification_type", type)
                scope.setTag("malformed_reason", reason)
                scope.setContexts("notification", mapOf(
                    "type" to type,
                    "reason" to reason,
                    "keys" to data.keys.toList()
                ))
            }
        }
    }

    private fun handleTagScannedNotification(data: Map<String, String>) {
        // 2026-05-02 missing-pet flow overhaul: backend dropped the
        // `location_type` discriminator. The payload now optionally
        // includes lat/lng (precise GPS or geocoded manual address) plus
        // an optional `manual_address` text the finder typed verbatim.
        // Always treat coords as precise when present.
        val petName = data["pet_name"] ?: getString(R.string.notif_fallback_pet_name)
        val scanId = data["scan_id"]
        val petId = data["pet_id"]
        val coords = parseValidCoords(data["latitude"], data["longitude"])
        val latitude = coords?.first
        val longitude = coords?.second
        val manualAddress = data["manual_address"]

        val locationInfo = when {
            latitude != null && longitude != null && !manualAddress.isNullOrBlank() -> manualAddress
            latitude != null && longitude != null -> getString(R.string.notif_location_shared)
            !manualAddress.isNullOrBlank() -> manualAddress
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
                isApproximate = false,
                address = manualAddress
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
        val coords = parseValidCoords(data["latitude"], data["longitude"])
        val latitude = coords?.first
        val longitude = coords?.second
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

    private fun handleAlertConfirmation(data: Map<String, String>) {
        val alertId = data["alert_id"]
        val petId = data["pet_id"]

        notificationHelper.showAlertConfirmationNotification(
            title = data["title"] ?: getString(R.string.notif_alert_confirmation_title),
            body = data["body"] ?: getString(R.string.notif_alert_confirmation_body),
            alertId = alertId,
            petId = petId
        )
    }

    private fun handlePromoExpiring(data: Map<String, String>) {
        val planName = data["plan_name"]
        val daysLeft = data["days_left"]

        notificationHelper.showPromoExpiringNotification(
            title = data["title"] ?: getString(R.string.notif_promo_expiring_title),
            body = data["body"] ?: getString(R.string.notif_promo_expiring_body),
            planName = planName,
            daysLeft = daysLeft
        )
    }

    /**
     * VACCINATION_DUE: a vaccination record is approaching (or past) its expiry.
     * Payload `{type, pet_id, vaccination_id, days_until, title, body}` (A.6).
     * Renders on the lifecycle channel; the tap deep-links off `pet_id` into the
     * converged pets → pet_detail → vaccinations back stack. `vaccination_id` and
     * `days_until` are validated as required (so a malformed push is caught +
     * dropped) but aren't needed for the list-level deep link — the target is the
     * pet's list, not a specific record (matches iOS).
     */
    private fun handleVaccinationDue(data: Map<String, String>) {
        notificationHelper.showVaccinationDueNotification(
            title = data["title"] ?: getString(R.string.notif_vaccination_due_title),
            body = data["body"] ?: getString(R.string.notif_vaccination_due_body),
            petId = data["pet_id"]
        )
    }

    private fun handleMultipleSightings(data: Map<String, String>) {
        val alertId = data["alert_id"]

        notificationHelper.showMultipleSightingsNotification(
            title = data["title"] ?: getString(R.string.notif_multiple_sightings_title),
            body = data["body"] ?: getString(R.string.notif_multiple_sightings_body),
            alertId = alertId
        )
    }

    /**
     * Parse lat/lng strings from an FCM data payload and range-validate them.
     *
     * Returns null if either value is missing, non-numeric, out of range
     * (|lat| > 90 or |lng| > 180), or is the (0,0) sentinel — an all-zeros
     * payload used to point at the Gulf of Guinea would otherwise flow
     * through to a "View on Map" action that opens the ocean. A malformed
     * push shouldn't crash the app but also shouldn't render a map link
     * pointing at junk.
     */
    private fun parseValidCoords(latStr: String?, lngStr: String?): Pair<Double, Double>? {
        val lat = latStr?.toDoubleOrNull() ?: return null
        val lng = lngStr?.toDoubleOrNull() ?: return null
        if (kotlin.math.abs(lat) > 90.0 || kotlin.math.abs(lng) > 180.0) return null
        if (lat == 0.0 && lng == 0.0) return null
        return lat to lng
    }

    companion object {
        /**
         * Required field map per known notification type. Mirrors the
         * iOS NotificationHandler validation guards. Types absent from
         * this map (e.g. PROMO_EXPIRING which doesn't deep-link to an
         * alert) are not validated here.
         */
        private val REQUIRED_FIELDS_BY_TYPE: Map<String, List<String>> = mapOf(
            "PET_SCANNED" to listOf("pet_id"),
            "TAG_INITIAL_SCAN" to listOf("pet_id"),
            "MISSING_PET_ALERT" to listOf("alert_id"),
            "PET_FOUND" to listOf("alert_id"),
            "SIGHTING_REPORTED" to listOf("alert_id"),
            NotificationHelper.TYPE_ALERT_CONFIRMATION to listOf("alert_id"),
            NotificationHelper.TYPE_MULTIPLE_SIGHTINGS to listOf("alert_id"),
            "VACCINATION_DUE" to listOf("pet_id", "vaccination_id", "days_until"),
        )

        /**
         * Returns the name of the first required field that is missing
         * or blank for [type], or null if the payload is acceptable.
         * Pure function for unit-testability — the FCM service can't be
         * easily exercised end-to-end in a JVM unit test.
         */
        fun validatePayload(type: String, data: Map<String, String>): String? {
            val required = REQUIRED_FIELDS_BY_TYPE[type] ?: return null
            return required.firstOrNull { data[it].isNullOrBlank() }
        }
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
