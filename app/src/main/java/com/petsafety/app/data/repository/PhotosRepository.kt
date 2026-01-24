package com.petsafety.app.data.repository

import com.petsafety.app.data.model.PetPhoto
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.PhotoReorderRequest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class PhotosRepository(private val apiService: ApiService) {
    suspend fun getPetPhotos(petId: String): List<PetPhoto> =
        apiService.getPetPhotos(petId).data?.photos ?: emptyList()

    suspend fun uploadPetPhoto(petId: String, bytes: ByteArray, isPrimary: Boolean): PetPhoto {
        val photo = MultipartBody.Part.createFormData(
            "photo",
            "photo.jpg",
            bytes.toRequestBody("image/jpeg".toMediaType())
        )
        val isPrimaryBody = isPrimary.toString().toRequestBody("text/plain".toMediaType())
        val response = apiService.uploadPetPhoto(petId, photo, isPrimaryBody)
        return response.data?.photo ?: error("Missing photo")
    }

    suspend fun setPrimaryPhoto(petId: String, photoId: String) =
        apiService.setPrimaryPhoto(petId, photoId).data ?: error("Missing response")

    suspend fun deletePhoto(petId: String, photoId: String) =
        apiService.deletePetPhoto(petId, photoId).data ?: error("Missing response")

    suspend fun reorderPhotos(petId: String, photoIds: List<String>) =
        apiService.reorderPetPhotos(petId, PhotoReorderRequest(photoIds)).data
            ?: error("Missing response")
}
