package com.petsafety.app.data.repository

import com.petsafety.app.data.model.NotificationPreferences
import com.petsafety.app.data.network.ApiService
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

class NotificationPreferencesRepository(private val apiService: ApiService) {
    suspend fun getPreferences(): NotificationPreferences =
        apiService.getNotificationPreferences().data?.preferences ?: error("Missing preferences")

    /**
     * Save preferences. Sends ONLY the channels that differ from the
     * supplied `original` snapshot — the backend's PUT endpoint applies
     * each channel independently when present, so a partial body lets
     * one device toggle email-OFF without clobbering another device's
     * concurrent SMS-OFF change.
     *
     * The pre-fix path always sent all three channel booleans, which
     * meant a stale local copy on Android could overwrite a change
     * made on iOS or web 30 seconds earlier — the user reported
     * receiving SMS again despite turning it off on the other device.
     *
     * `original == null` falls back to sending the full state, used
     * for first-time loads where there's nothing to diff against.
     */
    suspend fun updatePreferences(
        next: NotificationPreferences,
        original: NotificationPreferences? = null,
    ): NotificationPreferences {
        val body = buildJsonObject {
            if (original == null || next.notifyByEmail != original.notifyByEmail) {
                put("notifyByEmail", JsonPrimitive(next.notifyByEmail))
            }
            if (original == null || next.notifyBySms != original.notifyBySms) {
                put("notifyBySms", JsonPrimitive(next.notifyBySms))
            }
            if (original == null || next.notifyByPush != original.notifyByPush) {
                put("notifyByPush", JsonPrimitive(next.notifyByPush))
            }
            if (original == null || next.missingPetAlerts != original.missingPetAlerts) {
                put("missingPetAlerts", JsonPrimitive(next.missingPetAlerts))
            }
        }
        // Nothing changed — short-circuit. Caller's "saved!" toast still
        // fires; backend already has the desired state.
        if (body.isEmpty()) return next

        return apiService.updateNotificationPreferences(body).data?.preferences
            ?: error("Missing preferences")
    }
}
