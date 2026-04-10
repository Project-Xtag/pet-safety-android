package com.petsafety.app.data.model

import com.petsafety.app.data.network.model.CreatePetRequest
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
    val pet: Pet? = null,
    @SerialName("scan_id") val scanId: String? = null,
    val message: String? = null
)

@Serializable
data class TagLookupResponse(
    val exists: Boolean,
    val status: String? = null,
    @SerialName("has_pet") val hasPet: Boolean = false,
    @SerialName("is_owner") val isOwner: Boolean = false,
    @SerialName("can_activate") val canActivate: Boolean = false,
    val pet: Pet? = null,
    @SerialName("can_claim_promo") val canClaimPromo: Boolean? = null,
    val promo: PromoTagInfo? = null
)

@Serializable
data class PromoTagInfo(
    @SerialName("is_promo_tag") val isPromoTag: Boolean,
    @SerialName("shelter_name") val shelterName: String,
    @SerialName("batch_expired") val batchExpired: Boolean,
    @SerialName("promo_duration_months") val promoDurationMonths: Int
)

@Serializable
data class ClaimPromoTagRequest(
    val qrCode: String,
    val pet: CreatePetRequest? = null,
    val petId: String? = null
)

@Serializable
data class ClaimPromoTagResponse(
    val success: Boolean,
    val message: String,
    val pet: Pet? = null,
    val tag: ClaimedTag? = null,
    @SerialName("subscription_action") val subscriptionAction: String? = null,
    @SerialName("promo_details") val promoDetails: PromoDetails? = null
) {
    @Serializable
    data class ClaimedTag(
        @SerialName("qr_code") val qrCode: String,
        val status: String,
        @SerialName("pet_id") val petId: String
    )

    @Serializable
    data class PromoDetails(
        @SerialName("shelter_name") val shelterName: String,
        @SerialName("trial_end_date") val trialEndDate: String,
        @SerialName("promo_duration_months") val promoDurationMonths: Int
    )
}
