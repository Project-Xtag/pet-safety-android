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
    @SerialName("administered_at") val administeredAt: String,
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("batch_number") val batchNumber: String? = null,
    @SerialName("vet_name") val vetName: String? = null,
    @SerialName("vet_clinic") val vetClinic: String? = null,
    val notes: String? = null
)
