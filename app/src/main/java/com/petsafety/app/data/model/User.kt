package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String,
    val email: String,
    val role: String? = null,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    @SerialName("postal_code") val postalCode: String? = null,
    val country: String? = null,
    @SerialName("is_service_provider") val isServiceProvider: Boolean? = null,
    @SerialName("service_provider_type") val serviceProviderType: String? = null,
    @SerialName("organization_name") val organizationName: String? = null,
    @SerialName("vet_license_number") val vetLicenseNumber: String? = null,
    @SerialName("is_verified") val isVerified: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("show_phone_publicly") val showPhonePublicly: Boolean? = null,
    @SerialName("show_email_publicly") val showEmailPublicly: Boolean? = null
) {
    val fullName: String
        get() = listOfNotNull(firstName, lastName).joinToString(" ")
}
