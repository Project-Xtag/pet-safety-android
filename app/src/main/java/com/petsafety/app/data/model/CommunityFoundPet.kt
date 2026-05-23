package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A community-submitted "I found this stray" report.
 *
 * Mirrors the iOS `CommunityFoundPet` struct and the backend's
 * `community_found_pets` table. Optional contact fields are nullable
 * because anonymous reporters are allowed.
 */
@Serializable
data class CommunityFoundPet(
    val id: String,
    val species: Species,
    val sex: Sex,
    val breed: String? = null,
    val color: String? = null,
    val description: String? = null,
    @SerialName("photoUrl") val photoUrl: String? = null,
    @SerialName("foundAt") val foundAt: String,
    @SerialName("foundLatitude") val foundLatitude: Double,
    @SerialName("foundLongitude") val foundLongitude: Double,
    @SerialName("foundAddress") val foundAddress: String? = null,
    val status: Status,
    @SerialName("reporterName") val reporterName: String? = null,
    @SerialName("reporterEmail") val reporterEmail: String? = null,
    @SerialName("reporterPhone") val reporterPhone: String? = null,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("updatedAt") val updatedAt: String,
    @SerialName("expiresAt") val expiresAt: String,
    /** Populated only by the "nearby" endpoint. */
    @SerialName("distanceKm") val distanceKm: Double? = null,
) {
    @Serializable
    enum class Species {
        @SerialName("dog") DOG,
        @SerialName("cat") CAT,
        @SerialName("other") OTHER,
    }

    @Serializable
    enum class Sex {
        @SerialName("male") MALE,
        @SerialName("female") FEMALE,
        @SerialName("unknown") UNKNOWN,
    }

    @Serializable
    enum class Status {
        @SerialName("active") ACTIVE,
        @SerialName("reunited") REUNITED,
        @SerialName("expired") EXPIRED,
        @SerialName("removed") REMOVED,
    }
}

@Serializable
data class NearbyFoundPetsResponse(
    val reports: List<CommunityFoundPet>,
    val count: Int,
)

@Serializable
data class CreateFoundPetResponse(
    val report: CommunityFoundPet,
    @SerialName("manageToken") val manageToken: String,
)
