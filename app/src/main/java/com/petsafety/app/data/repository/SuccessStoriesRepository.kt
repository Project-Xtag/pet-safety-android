package com.petsafety.app.data.repository

import com.petsafety.app.data.local.OfflineDataManager
import com.petsafety.app.data.model.SuccessStory
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.CreateSuccessStoryRequest
import com.petsafety.app.data.network.model.UpdateSuccessStoryRequest
import com.petsafety.app.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.first

class SuccessStoriesRepository(
    private val apiService: ApiService,
    private val offlineManager: OfflineDataManager,
    private val networkMonitor: NetworkMonitor
) {
    suspend fun fetchPublicStories(
        latitude: Double,
        longitude: Double,
        radiusKm: Double,
        page: Int,
        limit: Int
    ): com.petsafety.app.data.network.model.SuccessStoriesResponse {
        networkMonitor.refreshStatus()
        return if (networkMonitor.isConnected.first()) {
            val response = apiService.getPublicSuccessStories(latitude, longitude, radiusKm, page, limit)
            response.data?.stories?.forEach { offlineManager.saveSuccessStory(it) }
            response.data ?: com.petsafety.app.data.network.model.SuccessStoriesResponse(
                stories = emptyList(),
                total = 0,
                hasMore = false,
                page = page,
                limit = limit
            )
        } else {
            com.petsafety.app.data.network.model.SuccessStoriesResponse(
                stories = offlineManager.fetchSuccessStories(),
                total = 0,
                hasMore = false,
                page = 1,
                limit = limit
            )
        }
    }

    suspend fun getStoriesForPet(petId: String): List<SuccessStory> =
        apiService.getSuccessStoriesForPet(petId).data ?: emptyList()

    suspend fun createStory(request: CreateSuccessStoryRequest): SuccessStory =
        apiService.createSuccessStory(request).data ?: error("Missing story")

    suspend fun updateStory(id: String, request: UpdateSuccessStoryRequest): SuccessStory =
        apiService.updateSuccessStory(id, request).data ?: error("Missing story")

    suspend fun deleteStory(id: String) {
        apiService.deleteSuccessStory(id)
    }
}
