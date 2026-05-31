package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Home-screen vaccination summary (locked A.7 contract). A 200 with these
 * fields means the feature is ON for the user; a 404 means OFF. That 404/200
 * distinction is the ONLY availability signal and is interpreted in exactly
 * one place — see `VaccinationGate`.
 */
@Serializable
data class VaccinationHomeSummary(
    @SerialName("total_pets_with_vaccinations") val totalPetsWithVaccinations: Int = 0,
    @SerialName("expired_count") val expiredCount: Int = 0,
    @SerialName("expiring_30d_count") val expiring30dCount: Int = 0,
    @SerialName("valid_count") val validCount: Int = 0,
    val urgent: List<UrgentVaccination> = emptyList()
)

/**
 * One urgent (expired or expiring) record surfaced on the home card. The
 * server sends an authoritative [status] and a SIGNED [daysUntilExpiry]
 * (negative = overdue) — both consumed verbatim; the UI branches on status
 * and uses the magnitude of the day count for "N days" copy.
 */
@Serializable
data class UrgentVaccination(
    @SerialName("pet_id") val petId: String,
    @SerialName("pet_name") val petName: String,
    @SerialName("pet_profile_image") val petProfileImage: String? = null,
    @SerialName("vaccination_id") val vaccinationId: String,
    @SerialName("vaccine_name") val vaccineName: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("days_until_expiry") val daysUntilExpiry: Int = 0,
    val status: String = "expiring"
) {
    /** Maps the server's status string onto the shared pill vocabulary. */
    val statusEnum: VaccinationStatus
        get() = if (status == "expired") VaccinationStatus.EXPIRED else VaccinationStatus.EXPIRING
}
