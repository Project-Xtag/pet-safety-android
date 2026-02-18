package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QrTag(
    val id: String,
    @SerialName("qr_code") val qrCode: String,
    @SerialName("pet_id") val petId: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    val qrCodeUrl: String
        get() = "https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=$qrCode"
}

@Serializable
data class ScanResponse(
    val pet: Pet
)

@Serializable
data class TagLookupResponse(
    val exists: Boolean,
    val status: String? = null,
    @SerialName("has_pet") val hasPet: Boolean = false,
    @SerialName("is_owner") val isOwner: Boolean = false,
    val pet: Pet? = null
)
