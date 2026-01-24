package com.petsafety.app.data.network.model

import com.petsafety.app.data.model.LocationCoordinate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String
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
    val rewardAmount: Double? = null
)

@Serializable
data class CreateAlertRequest(
    val petId: String,
    val lastSeenLocation: LocationCoordinate? = null,
    val lastSeenAddress: String? = null,
    val description: String? = null,
    val rewardAmount: Double? = null,
    val alertRadiusKm: Double? = null
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

@Serializable
data class ShareLocationRequest(
    val qrCode: String,
    val location: LocationData,
    val address: String? = null
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
    val shippingAddress: AddressDetails
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
    @SerialName("pet_id") val petId: String,
    @SerialName("alert_id") val alertId: String? = null,
    @SerialName("reunion_latitude") val reunionLatitude: Double? = null,
    @SerialName("reunion_longitude") val reunionLongitude: Double? = null,
    @SerialName("reunion_city") val reunionCity: String? = null,
    @SerialName("story_text") val storyText: String? = null,
    @SerialName("auto_confirm") val autoConfirm: Boolean? = null
)

@Serializable
data class UpdateSuccessStoryRequest(
    @SerialName("story_text") val storyText: String? = null,
    @SerialName("is_public") val isPublic: Boolean? = null,
    @SerialName("is_confirmed") val isConfirmed: Boolean? = null
)
