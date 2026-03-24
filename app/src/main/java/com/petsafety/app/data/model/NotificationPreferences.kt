package com.petsafety.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class NotificationPreferences(
    val notifyByEmail: Boolean,
    val notifyBySms: Boolean,
    val notifyByPush: Boolean,
    val missingPetAlerts: Boolean = true
) {
    val isValid: Boolean
        get() = notifyByEmail || notifyBySms || notifyByPush

    val enabledCount: Int
        get() = listOf(notifyByEmail, notifyBySms, notifyByPush).count { it }

    companion object {
        val default = NotificationPreferences(
            notifyByEmail = true,
            notifyBySms = true,
            notifyByPush = true,
            missingPetAlerts = true
        )
    }
}
