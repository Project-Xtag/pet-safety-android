package com.petsafety.app.data.repository

import com.petsafety.app.data.model.NotificationPreferences
import com.petsafety.app.data.network.ApiService

class NotificationPreferencesRepository(private val apiService: ApiService) {
    suspend fun getPreferences(): NotificationPreferences =
        apiService.getNotificationPreferences().data?.preferences ?: error("Missing preferences")

    suspend fun updatePreferences(preferences: NotificationPreferences): NotificationPreferences {
        return apiService.updateNotificationPreferences(preferences).data?.preferences ?: error("Missing preferences")
    }
}
