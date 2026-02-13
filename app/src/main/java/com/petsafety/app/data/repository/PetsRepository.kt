package com.petsafety.app.data.repository

import com.petsafety.app.data.local.OfflineDataManager
import com.petsafety.app.data.model.Breed
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.CreatePetRequest
import com.petsafety.app.data.network.model.MarkMissingRequest
import com.petsafety.app.data.network.model.UpdatePetRequest
import com.petsafety.app.data.sync.NetworkMonitor
import com.petsafety.app.data.sync.SyncService
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class PetsRepository(
    private val apiService: ApiService,
    private val offlineManager: OfflineDataManager,
    private val networkMonitor: NetworkMonitor,
    private val syncService: SyncService
) {
    suspend fun fetchPets(): Pair<List<Pet>, String?> {
        networkMonitor.refreshStatus()
        return if (networkMonitor.isConnected.first()) {
            val pets = apiService.getPets().data?.pets ?: emptyList()
            offlineManager.savePets(pets)
            pets to null
        } else {
            offlineManager.fetchPets() to "Showing cached data (offline)"
        }
    }

    suspend fun createPet(request: CreatePetRequest): Pet {
        val pet = apiService.createPet(request).data?.pet ?: error("Missing pet")
        offlineManager.savePet(pet)
        return pet
    }

    suspend fun updatePet(id: String, request: UpdatePetRequest): Pet {
        val pet = apiService.updatePet(id, request).data?.pet ?: error("Missing pet")
        offlineManager.savePet(pet)
        return pet
    }

    suspend fun deletePet(id: String) {
        apiService.deletePet(id)
    }

    suspend fun uploadProfilePhoto(petId: String, bytes: ByteArray): Pet {
        val part = MultipartBody.Part.createFormData(
            "image",
            "image.jpg",
            bytes.toRequestBody("image/jpeg".toMediaType())
        )
        apiService.uploadPetImage(petId, part)
        return apiService.getPet(petId).data?.pet ?: error("Missing pet")
    }

    suspend fun markPetMissing(
        petId: String,
        location: LocationCoordinate?,
        address: String?,
        description: String?,
        rewardAmount: Double? = null,
        notificationCenterSource: String? = null,
        notificationCenterLocation: LocationCoordinate? = null,
        notificationCenterAddress: String? = null
    ): Result<Pet> {
        networkMonitor.refreshStatus()
        if (!networkMonitor.isConnected.first()) {
            val actionData = mutableMapOf<String, Any?>("petId" to petId)
            if (location != null) {
                actionData["latitude"] = location.lat
                actionData["longitude"] = location.lng
            }
            if (!address.isNullOrBlank()) actionData["lastSeenAddress"] = address
            if (!description.isNullOrBlank()) actionData["description"] = description
            if (rewardAmount != null) actionData["rewardAmount"] = rewardAmount
            if (!notificationCenterSource.isNullOrBlank()) actionData["notificationCenterSource"] = notificationCenterSource
            if (notificationCenterLocation != null) {
                actionData["notificationCenterLatitude"] = notificationCenterLocation.lat
                actionData["notificationCenterLongitude"] = notificationCenterLocation.lng
            }
            if (!notificationCenterAddress.isNullOrBlank()) actionData["notificationCenterAddress"] = notificationCenterAddress
            syncService.queueAction(SyncService.ActionType.MARK_PET_LOST, actionData)
            return Result.failure(OfflineQueuedException())
        }

        val response = apiService.markPetMissing(
            petId,
            MarkMissingRequest(
                lastSeenLocation = location,
                lastSeenAddress = address,
                description = description,
                rewardAmount = rewardAmount,
                notificationCenterSource = notificationCenterSource,
                notificationCenterLocation = notificationCenterLocation,
                notificationCenterAddress = notificationCenterAddress
            )
        )
        response.data?.pet?.let { offlineManager.savePet(it) }
        return Result.success(response.data?.pet ?: error("Missing pet"))
    }

    suspend fun markPetFound(petId: String): Result<Pet> {
        networkMonitor.refreshStatus()
        if (!networkMonitor.isConnected.first()) {
            val actionData = mapOf("petId" to petId)
            syncService.queueAction(SyncService.ActionType.MARK_PET_FOUND, actionData)
            return Result.failure(OfflineQueuedException())
        }

        val response = apiService.updatePet(petId, UpdatePetRequest(isMissing = false))
        response.data?.pet?.let { offlineManager.savePet(it) }
        return Result.success(response.data?.pet ?: error("Missing pet"))
    }

    suspend fun getDogBreeds(): List<Breed> {
        return apiService.getDogBreeds().data?.breeds ?: emptyList()
    }

    suspend fun getCatBreeds(): List<Breed> {
        return apiService.getCatBreeds().data?.breeds ?: emptyList()
    }

    suspend fun getBreedsBySpecies(species: String): List<Breed> {
        return when (species.lowercase()) {
            "dog" -> getDogBreeds()
            "cat" -> getCatBreeds()
            else -> emptyList()
        }
    }
}

class OfflineQueuedException : Exception()
