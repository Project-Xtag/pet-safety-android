package com.petsafety.app.data.network.model

import com.petsafety.app.data.model.LocationCoordinate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val locale: String? = null
)

@Serializable
data class VerifyOtpRequest(
    val email: String,
    @SerialName("otp") val otp: String
)

@Serializable
data class CreatePetRequest(
    val name: String,
    val species: String,
    val breed: String? = null,
    val color: String? = null,
    val age: String? = null,
    val weight: Double? = null,
    @SerialName("microchip_number") val microchipNumber: String? = null,
    @SerialName("medical_notes") val medicalNotes: String? = null,
    val allergies: String? = null,
    val medications: String? = null,
    val notes: String? = null,
    @SerialName("unique_features") val uniqueFeatures: String? = null,
    val sex: String? = null,
    @SerialName("is_neutered") val isNeutered: Boolean? = null
)

@Serializable
data class UpdatePetRequest(
    val name: String? = null,
    val species: String? = null,
    val breed: String? = null,
    val color: String? = null,
    val age: String? = null,
    val weight: Double? = null,
    @SerialName("microchip_number") val microchipNumber: String? = null,
    @SerialName("medical_notes") val medicalNotes: String? = null,
    val allergies: String? = null,
    val medications: String? = null,
    val notes: String? = null,
    @SerialName("unique_features") val uniqueFeatures: String? = null,
    val sex: String? = null,
    @SerialName("is_neutered") val isNeutered: Boolean? = null,
    @SerialName("is_missing") val isMissing: Boolean? = null
)

@Serializable
data class MarkMissingRequest(
    val lastSeenLocation: LocationCoordinate? = null,
    val lastSeenAddress: String? = null,
    val description: String? = null,
    val rewardAmount: String? = null,
    val notificationCenterSource: String? = null,
    val notificationCenterLocation: LocationCoordinate? = null,
    val notificationCenterAddress: String? = null
)

@Serializable
data class CreateAlertRequest(
    val petId: String,
    val lastSeenLocation: LocationCoordinate? = null,
    val lastSeenAddress: String? = null,
    val description: String? = null,
    val rewardAmount: String? = null,
    val alertRadiusKm: Double? = null
)

@Serializable
data class UpdateAlertRequest(
    val description: String? = null,
    val lastSeenAddress: String? = null,
    val rewardAmount: String? = null
)

@Serializable
data class ReportSightingRequest(
    val reporterName: String? = null,
    val reporterPhone: String? = null,
    val reporterEmail: String? = null,
    val location: LocationCoordinate? = null,
    val address: String? = null,
    val description: String? = null,
    val photoUrl: String? = null,
    val sightedAt: String? = null
)

@Serializable
data class ActivateTagRequest(
    val qrCode: String,
    val petId: String
)

@Serializable
data class LocationData(
    val lat: Double,
    val lng: Double
)

/**
 * Location consent type for 2-tier GDPR compliance
 */
@Serializable
enum class LocationConsentType {
    @SerialName("approximate") APPROXIMATE,
    @SerialName("precise") PRECISE
}

/**
 * Share location request with 2-tier consent (toggle)
 * - Toggle ON (default): consent_type = "precise", exact GPS coordinates
 * - Toggle OFF: consent_type = "approximate", coordinates rounded to 3 decimals (~500m)
 */
@Serializable
data class ShareLocationRequest(
    @SerialName("qr_code") val qrCode: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("accuracy_meters") val accuracyMeters: Double? = null,
    @SerialName("is_approximate") val isApproximate: Boolean? = null,
    @SerialName("consent_type") val consentType: LocationConsentType? = null,
    @SerialName("share_exact_location") val shareExactLocation: Boolean? = null
)

/**
 * FCM token registration request
 */
@Serializable
data class FCMTokenRequest(
    val token: String,
    @SerialName("device_name") val deviceName: String? = null,
    val platform: String = "android"
)

@Serializable
data class CreateOrderRequest(
    val petNames: List<String>,
    val ownerName: String,
    val email: String,
    val shippingAddress: AddressDetails,
    val billingAddress: AddressDetails? = null,
    val paymentMethod: String? = null,
    val shippingCost: Double? = null
)

@Serializable
data class CreateTagOrderRequest(
    val petNames: List<String>,
    val ownerName: String,
    val email: String,
    val shippingAddress: AddressDetails,
    val billingAddress: AddressDetails? = null,
    val paymentMethod: String? = null,
    val shippingCost: Double? = null
)

@Serializable
data class AddressDetails(
    val street1: String,
    val street2: String? = null,
    val city: String,
    val province: String? = null,
    val postCode: String,
    val country: String,
    val phone: String? = null
)

@Serializable
data class CreateReplacementOrderRequest(
    val shippingAddress: AddressDetails,
    val platform: String = "android",
    val deliveryMethod: String? = null,
    val postapointDetails: PostaPointDetails? = null
)

@Serializable
data class CreatePaymentIntentRequest(
    val orderId: String,
    val amount: Double,
    val paymentMethod: String? = null,
    val currency: String? = null,
    val email: String? = null
)

@Serializable
data class PhotoReorderRequest(
    @SerialName("photo_order") val photoOrder: List<String>
)

@Serializable
data class CreateSuccessStoryRequest(
    val petId: String,
    val alertId: String? = null,
    val reunionLatitude: Double? = null,
    val reunionLongitude: Double? = null,
    val reunionCity: String? = null,
    val storyText: String? = null,
    val autoConfirm: Boolean? = null
)

@Serializable
data class UpdateSuccessStoryRequest(
    @SerialName("story_text") val storyText: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("is_confirmed") val isConfirmed: Boolean? = null
)

@Serializable
data class SupportRequest(
    val category: String,
    val subject: String,
    val message: String
)

// Subscription requests
@Serializable
data class CreateCheckoutRequest(
    @SerialName("plan_name") val planName: String,
    @SerialName("billing_period") val billingPeriod: String,
    val platform: String = "android",
    @SerialName("promo_code") val promoCode: String? = null,
    @SerialName("country_code") val countryCode: String? = null
)

// Tag order checkout request (Stripe Checkout redirect flow)
@Serializable
data class CreateTagCheckoutRequest(
    val quantity: Int,
    @SerialName("country_code") val countryCode: String? = null,
    val platform: String = "android",
    val deliveryMethod: String? = null,
    val postapointDetails: PostaPointDetails? = null
)

@Serializable
data class PostaPointDetails(
    val id: String,
    val name: String,
    val address: String? = null
)

@Serializable
data class ApplyReferralRequest(
    val code: String
)
