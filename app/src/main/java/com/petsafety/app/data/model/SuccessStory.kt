package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SuccessStory(
    val id: String,
    @SerialName("alert_id") val alertId: String? = null,
    @SerialName("pet_id") val petId: String,
    @SerialName("owner_id") val ownerId: String,
    @SerialName("reunion_city") val reunionCity: String? = null,
    @SerialName("reunion_latitude") val reunionLatitude: Double? = null,
    @SerialName("reunion_longitude") val reunionLongitude: Double? = null,
    @SerialName("story_text") val storyText: String? = null,
    @SerialName("is_public") val isPublic: Boolean,
    @SerialName("is_confirmed") val isConfirmed: Boolean,
    @SerialName("missing_since") val missingSince: String? = null,
    @SerialName("found_at") val foundAt: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null,
    @SerialName("pet_name") val petName: String? = null,
    @SerialName("pet_species") val petSpecies: String? = null,
    @SerialName("pet_photo_url") val petPhotoUrl: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    val photos: List<SuccessStoryPhoto>? = null
)

@Serializable
data class SuccessStoryPhoto(
    val id: String,
    @SerialName("success_story_id") val successStoryId: String,
    @SerialName("photo_url") val photoUrl: String,
    @SerialName("display_order") val displayOrder: Int
)
