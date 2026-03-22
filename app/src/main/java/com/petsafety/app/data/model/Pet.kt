package com.petsafety.app.data.model

import com.petsafety.app.R
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Pet(
    val id: String,
    @SerialName("owner_id") val ownerId: String = "",
    val name: String,
    val species: String = "",
    val breed: String? = null,
    val color: String? = null,
    val weight: Double? = null,
    @SerialName("microchip_number") val microchipNumber: String? = null,
    @SerialName("medical_notes") val medicalNotes: String? = null,
    val notes: String? = null,
    @SerialName("profile_image") private val profileImageField: String? = null,
    @SerialName("photo_url") private val photoUrlField: String? = null,
    @SerialName("is_missing") val isMissing: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("age_years") val ageYears: Int? = null,
    @SerialName("age_months") val ageMonths: Int? = null,
    @SerialName("age_text") val ageText: String? = null,
    @SerialName("age_is_approximate") val ageIsApproximate: Boolean? = null,
    val allergies: String? = null,
    val medications: String? = null,
    @SerialName("unique_features") val uniqueFeatures: String? = null,
    val sex: String? = null,
    @SerialName("is_neutered") val isNeutered: Boolean? = null,
    @SerialName("qr_code") val qrCode: String? = null,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("dob_is_approximate") val dobIsApproximate: Boolean? = null,
    @SerialName("owner_name") val ownerName: String? = null,
    @SerialName("owner_phone") val ownerPhone: String? = null,
    @SerialName("owner_secondary_phone") val ownerSecondaryPhone: String? = null,
    @SerialName("owner_email") val ownerEmail: String? = null,
    @SerialName("owner_secondary_email") val ownerSecondaryEmail: String? = null,
    @SerialName("owner_address") val ownerAddress: String? = null,
    @SerialName("owner_address_line_2") val ownerAddressLine2: String? = null,
    @SerialName("owner_city") val ownerCity: String? = null,
    @SerialName("owner_postal_code") val ownerPostalCode: String? = null,
    @SerialName("owner_country") val ownerCountry: String? = null
) {
    // Use whichever field is set (API may return either profile_image or photo_url)
    val profileImage: String?
        get() = profileImageField ?: photoUrlField
    // Age formatted from numeric fields using localized strings
    fun localizedAge(resources: android.content.res.Resources): String? {
        val prefix = if (ageIsApproximate == true) "~" else ""
        if (ageYears != null && ageYears > 0) {
            val yearsStr = resources.getQuantityString(R.plurals.age_years, ageYears, ageYears)
            return if (ageMonths != null && ageMonths > 0) {
                val monthsStr = resources.getQuantityString(R.plurals.age_months, ageMonths, ageMonths)
                "$prefix$yearsStr $monthsStr"
            } else {
                "$prefix$yearsStr"
            }
        }
        if (ageMonths != null && ageMonths > 0) {
            val monthsStr = resources.getQuantityString(R.plurals.age_months, ageMonths, ageMonths)
            return "$prefix$monthsStr"
        }
        return null
    }

    // Fallback age (non-localized, for non-UI contexts)
    val age: String?
        get() {
            val prefix = if (ageIsApproximate == true) "~" else ""
            if (ageYears != null && ageYears > 0) {
                return if (ageMonths != null && ageMonths > 0) {
                    "$prefix${ageYears}y ${ageMonths}m"
                } else {
                    "$prefix${ageYears}y"
                }
            }
            if (ageMonths != null && ageMonths > 0) {
                return "$prefix${ageMonths}m"
            }
            return null
        }
}
