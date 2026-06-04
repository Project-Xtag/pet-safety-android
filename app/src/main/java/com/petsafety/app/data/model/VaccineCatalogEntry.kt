package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One entry from the locale-aware vaccine catalog (A.4). `code` is OPAQUE —
 * stored/submitted verbatim, never parsed; the picker shows [displayName] and
 * submits [code]. `displayName`/`description` are already resolved server-side
 * for the request's locale.
 */
@Serializable
data class VaccineCatalogEntry(
    val code: String,
    @SerialName("display_name") val displayName: String,
    val description: String? = null,
    @SerialName("is_core") val isCore: Boolean = false,
    @SerialName("default_validity_months") val defaultValidityMonths: Int? = null,
    @SerialName("min_age_weeks") val minAgeWeeks: Int? = null,
    @SerialName("rabies_specific") val rabiesSpecific: Boolean = false,
    @SerialName("sort_order") val sortOrder: Int = 100
)
