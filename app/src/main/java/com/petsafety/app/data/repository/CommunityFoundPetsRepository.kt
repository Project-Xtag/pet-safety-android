package com.petsafety.app.data.repository

import com.petsafety.app.data.model.CommunityFoundPet
import com.petsafety.app.data.model.CreateFoundPetResponse
import com.petsafety.app.data.network.ApiService
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backs the community Lost & Found board's "found pet" side: nearby
 * reports for the map/list plus a multipart create call.
 *
 * Kept separate from AlertsRepository because the two endpoints have
 * different lifecycles and auth semantics — community reports are
 * public/anonymous, alerts are owner-scoped.
 */
@Singleton
class CommunityFoundPetsRepository @Inject constructor(
    private val apiService: ApiService,
) {
    suspend fun fetchNearby(
        latitude: Double,
        longitude: Double,
        radiusKm: Double = 25.0,
        species: CommunityFoundPet.Species? = null,
    ): List<CommunityFoundPet> {
        val speciesParam = when (species) {
            CommunityFoundPet.Species.DOG -> "dog"
            CommunityFoundPet.Species.CAT -> "cat"
            CommunityFoundPet.Species.OTHER -> "other"
            null -> null
        }
        return apiService.getNearbyFoundPets(
            latitude = latitude,
            longitude = longitude,
            radiusKm = radiusKm,
            species = speciesParam,
        ).data?.reports ?: emptyList()
    }

    /**
     * Submit a new found-pet report. The backend requires either a photo
     * OR a non-empty description; the caller is expected to enforce that
     * client-side before invoking us so the UX gets immediate feedback
     * instead of a 400 round-trip.
     */
    suspend fun create(
        species: CommunityFoundPet.Species,
        sex: CommunityFoundPet.Sex,
        foundAtIso: String,
        lat: Double,
        lng: Double,
        breed: String? = null,
        color: String? = null,
        description: String? = null,
        foundAddress: String? = null,
        reporterName: String? = null,
        reporterEmail: String? = null,
        reporterPhone: String? = null,
        photoBytes: ByteArray? = null,
    ): CreateFoundPetResponse? {
        val text = "text/plain".toMediaTypeOrNull()
        fun part(value: String): okhttp3.RequestBody = value.toRequestBody(text)
        fun nullable(value: String?): okhttp3.RequestBody? =
            value?.takeIf { it.isNotBlank() }?.toRequestBody(text)

        val speciesValue = when (species) {
            CommunityFoundPet.Species.DOG -> "dog"
            CommunityFoundPet.Species.CAT -> "cat"
            CommunityFoundPet.Species.OTHER -> "other"
        }
        val sexValue = when (sex) {
            CommunityFoundPet.Sex.MALE -> "male"
            CommunityFoundPet.Sex.FEMALE -> "female"
            CommunityFoundPet.Sex.UNKNOWN -> "unknown"
        }

        val photoPart = photoBytes?.let { bytes ->
            val body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("photo", "found-pet.jpg", body)
        }

        return apiService.createFoundPet(
            species = part(speciesValue),
            sex = part(sexValue),
            foundAt = part(foundAtIso),
            lat = part(lat.toString()),
            lng = part(lng.toString()),
            breed = nullable(breed),
            color = nullable(color),
            description = nullable(description),
            foundAddress = nullable(foundAddress),
            reporterName = nullable(reporterName),
            reporterEmail = nullable(reporterEmail),
            reporterPhone = nullable(reporterPhone),
            photo = photoPart,
        ).data
    }
}
