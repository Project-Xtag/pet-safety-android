package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Create-vaccination body (A.3 POST). Matches the server Zod schema. `expires_at`
 * omitted → the server derives it from the catalog's default_validity_months
 * (so we send null/omit rather than computing a client-side preview).
 * `vaccine_code` is opaque (the catalog `code`, submitted verbatim).
 *
 * Optional fields are nullable so kotlinx.serialization omits nulls only if
 * configured; the repository sends this as-is and the server treats absent /
 * null uniformly per the schema.
 */
@Serializable
data class CreateVaccinationRequest(
    @SerialName("vaccine_code") val vaccineCode: String,
    // Free-text name — set ONLY for an "Egyéb" (is_freetext) pick; the server
    // freezes it as the snapshot. null (dropped via explicitNulls=false) otherwise.
    @SerialName("vaccine_name") val vaccineName: String? = null,
    @SerialName("administered_at") val administeredAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("batch_number") val batchNumber: String? = null,
    @SerialName("vet_name") val vetName: String? = null,
    @SerialName("vet_clinic") val vetClinic: String? = null,
    val notes: String? = null
)

/**
 * Edit body (A.3 PUT). **Excludes `vaccine_code`** — it's immutable (change =
 * delete + re-add). Text fields are sent as their current value INCLUDING empty
 * strings, so clearing a field persists (mirrors iOS). `expires_at` is sent when
 * the record had one or the user toggled one on, else omitted (left unchanged).
 *
 * The converter has explicitNulls=false, so a null `expires_at` is dropped (the
 * server leaves it unchanged); the non-null text fields are always serialized.
 */
@Serializable
data class UpdateVaccinationRequest(
    @SerialName("administered_at") val administeredAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("batch_number") val batchNumber: String,
    @SerialName("vet_name") val vetName: String,
    @SerialName("vet_clinic") val vetClinic: String,
    val notes: String
)
