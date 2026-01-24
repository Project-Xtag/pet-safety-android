package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PetPhoto(
    val id: String,
    @SerialName("pet_id") val petId: String,
    @SerialName("photo_url") val photoUrl: String,
    @SerialName("is_primary") val isPrimary: Boolean,
    @SerialName("display_order") val displayOrder: Int,
    @SerialName("uploaded_at") val uploadedAt: String
)
