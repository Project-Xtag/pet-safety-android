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
    @SerialName("scan_id") val scanId: String? = null,
    @SerialName("sent_fcm") val sentFcm: Boolean? = null,
    @SerialName("sent_email") val sentEmail: Boolean? = null,
    @SerialName("sent_sse") val sentSse: Boolean? = null
)

/**
 * FCM token registration response
 */
@Serializable
data class FCMTokenResponse(
    val message: String,
    @SerialName("token_count") val tokenCount: Int? = null
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

@Serializable
data class CanDeleteAccountResponse(
    val canDelete: Boolean,
    val reason: String? = null,
    val message: String? = null,
    val missingPets: List<MissingPetInfo>? = null
)

@Serializable
data class MissingPetInfo(
    val id: String,
    val name: String
)

@Serializable
data class SupportRequestResponse(
    val ticketId: String,
    val message: String
)

// Subscription responses
@Serializable
data class SubscriptionPlansResponse(
    val plans: List<SubscriptionPlan>
)

@Serializable
data class MySubscriptionResponse(
    val subscription: UserSubscription? = null
)

@Serializable
data class CheckoutResponse(
    @SerialName("session_id") val sessionId: String? = null,
    val url: String? = null,
    val checkout: CheckoutData? = null
) {
    @Serializable
    data class CheckoutData(
        val id: String? = null,
        val url: String? = null
    )

    val resolvedUrl: String?
        get() = url ?: checkout?.url

    val resolvedSessionId: String?
        get() = sessionId ?: checkout?.id
}

@Serializable
data class UpgradeResponse(
    val subscription: UserSubscription,
    val message: String? = null
)

@Serializable
data class CancelSubscriptionResponse(
    val subscription: UserSubscription,
    val message: String? = null
)

@Serializable
data class PortalSessionResponse(
    val url: String
)

@Serializable
data class TagCheckoutResponse(
    val checkout: TagCheckoutData? = null
) {
    @Serializable
    data class TagCheckoutData(
        val id: String? = null,
        val url: String? = null
    )
}


@Serializable
data class InvoicesResponse(
    val invoices: List<Invoice>
)

@Serializable
data class SubscriptionFeaturesResponse(
    val features: SubscriptionFeaturesData? = null,
    val access: SubscriptionAccessData? = null
) {
    @Serializable
    data class SubscriptionFeaturesData(
        @SerialName("plan_name") val planName: String? = null,
        @SerialName("can_create_alerts") val canCreateAlerts: Boolean = false,
        @SerialName("can_receive_vet_alerts") val canReceiveVetAlerts: Boolean = false,
        @SerialName("can_receive_community_alerts") val canReceiveCommunityAlerts: Boolean = false,
        @SerialName("can_use_sms_notifications") val canUseSmsNotifications: Boolean = false,
        @SerialName("max_pets") val maxPets: Int? = null,
        @SerialName("max_photos_per_pet") val maxPhotosPerPet: Int = 10,
        @SerialName("max_emergency_contacts") val maxEmergencyContacts: Int = 1,
        @SerialName("free_tag_replacement") val freeTagReplacement: Boolean = false
    )

    @Serializable
    data class SubscriptionAccessData(
        @SerialName("paid_plan") val paidPlan: Boolean = false,
        val notifications: Boolean = false,
        val sms: Boolean = false,
        @SerialName("multiple_contacts") val multipleContacts: Boolean = false
    )
}

@Serializable
data class ReferralCodeResponse(
    val code: String,
    @SerialName("expires_at") val expiresAt: String? = null
)

@Serializable
data class ReferralStatusResponse(
    val code: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val referrals: List<Referral> = emptyList()
)
