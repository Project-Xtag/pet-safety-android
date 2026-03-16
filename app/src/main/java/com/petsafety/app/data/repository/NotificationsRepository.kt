package com.petsafety.app.data.repository

import com.petsafety.app.data.model.NotificationItem
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.PaginationInfo

class NotificationsRepository(private val apiService: ApiService) {
    suspend fun getNotifications(page: Int = 1, limit: Int = 20): Pair<List<NotificationItem>, PaginationInfo?> {
        val response = apiService.getNotifications(page, limit).data
            ?: return emptyList<NotificationItem>() to null
        return response.notifications to response.pagination
    }

    suspend fun getUnreadCount(): Int =
        apiService.getUnreadNotificationCount().data?.count ?: 0

    suspend fun markAsRead(id: String) {
        apiService.markNotificationAsRead(id)
    }

    suspend fun markAllAsRead() {
        apiService.markAllNotificationsAsRead()
    }
}
