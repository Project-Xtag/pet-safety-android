package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MissingPetAlert(
    val id: String,
    @SerialName("pet_id") val petId: String,
    @SerialName("user_id") val userId: String,
    val status: String,
    @SerialName("last_seen_location") val lastSeenLocation: String? = null,
    @SerialName("last_seen_latitude") val lastSeenLatitude: Double? = null,
    @SerialName("last_seen_longitude") val lastSeenLongitude: Double? = null,
    @SerialName("additional_info") val additionalInfo: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val pet: Pet? = null,
    val sightings: List<Sighting>? = null,
    @SerialName("description") val legacyDescription: String? = null,
    @SerialName("last_seen_address") val legacyLastSeenAddress: String? = null,
    val lat: Double? = null,
    val lng: Double? = null
) {
    val resolvedLastSeenLocation: String?
        get() = legacyLastSeenAddress ?: lastSeenLocation

    val resolvedLatitude: Double?
        get() = lastSeenLatitude ?: lat

    val resolvedLongitude: Double?
        get() = lastSeenLongitude ?: lng
}

@Serializable
data class Sighting(
    val id: String,
    @SerialName("alert_id") val alertId: String,
    @SerialName("reporter_name") val reporterName: String? = null,
    @SerialName("reporter_phone") val reporterPhone: String? = null,
    @SerialName("reporter_email") val reporterEmail: String? = null,
    @SerialName("sighting_location") val sightingLocation: String? = null,
    @SerialName("sighting_latitude") val sightingLatitude: Double? = null,
    @SerialName("sighting_longitude") val sightingLongitude: Double? = null,
    @SerialName("sighting_notes") val sightingNotes: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("description") val legacyDescription: String? = null,
    @SerialName("sighting_address") val legacySightingAddress: String? = null,
    @SerialName("reported_at") val legacyReportedAt: String? = null
) {
    val resolvedLocation: String?
        get() = legacySightingAddress ?: sightingLocation

    val resolvedNotes: String?
        get() = sightingNotes ?: legacyDescription

    val resolvedCreatedAt: String
        get() = createdAt.ifBlank { legacyReportedAt ?: createdAt }
}

@Serializable
data class LocationCoordinate(
    val lat: Double,
    val lng: Double
)
