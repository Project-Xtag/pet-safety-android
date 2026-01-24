package com.petsafety.app.data.network.model

import com.petsafety.app.data.model.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val message: String,
    @SerialName("expiresIn") val expiresIn: Int? = null
)

@Serializable
data class VerifyOtpResponse(
    val token: String,
    val user: User
)

@Serializable
data class UserResponse(
    val user: User
)

@Serializable
data class PetsResponse(
    val pets: List<Pet>
)

@Serializable
data class PetResponse(
    val pet: Pet
)

@Serializable
data class AlertsResponse(
    val alerts: List<MissingPetAlert>
)

@Serializable
data class NearbyAlertsResponse(
    val alerts: List<MissingPetAlert>,
    val count: Int
)

@Serializable
data class AlertResponse(
    val alert: MissingPetAlert,
    val message: String? = null
)

@Serializable
data class SightingResponse(
    val sighting: Sighting,
    val message: String? = null
)

@Serializable
data class OrdersResponse(
    val orders: List<Order>
)

@Serializable
data class ImageUploadResponse(
    val imageUrl: String,
    val pet: PartialPet
) {
    @Serializable
    data class PartialPet(
        @SerialName("profile_image") val profileImage: String? = null
    )
}

@Serializable
data class MarkMissingResponse(
    val pet: Pet,
    val alert: AlertInfo? = null,
    val message: String
)

@Serializable
data class AlertInfo(
    val id: String
)

@Serializable
data class NotificationPreferencesResponse(
    val preferences: NotificationPreferences,
    val message: String? = null
)

@Serializable
data class PetPhotosResponse(
    val photos: List<PetPhoto>
)

@Serializable
data class PhotoUploadResponse(
    val photo: PetPhoto,
    val message: String? = null
)

@Serializable
data class PhotoOperationResponse(
    val message: String? = null,
    val photo: PetPhoto? = null
)

@Serializable
data class PhotoReorderResponse(
    val message: String? = null,
    val photos: List<PetPhoto>? = null
)

@Serializable
data class ShareLocationResponse(
    val message: String,
    val sightingId: String,
    val sentSMS: Boolean,
    val sentEmail: Boolean
)

@Serializable
data class ActivateTagResponse(
    val tag: QrTag,
    val message: String? = null
)

@Serializable
data class GetTagResponse(
    val tag: QrTag? = null,
    val message: String? = null
)

@Serializable
data class ReplacementOrderResponse(
    val order: Order,
    val message: String? = null
)

@Serializable
data class CreateTagOrderResponse(
    val order: Order,
    val userCreated: Boolean? = null,
    val userId: String? = null,
    val message: String
)

@Serializable
data class PaymentIntentResponse(
    val paymentIntent: PaymentIntent
)

@Serializable
data class PaymentIntentStatusResponse(
    val paymentIntent: PaymentIntent
)

@Serializable
data class SuccessStoriesResponse(
    val stories: List<SuccessStory>,
    val total: Int,
    val hasMore: Boolean,
    val page: Int,
    val limit: Int
)

@Serializable
data class BreedsResponse(
    val breeds: List<Breed>
)
