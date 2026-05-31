package com.petsafety.app.data.network.model

import com.petsafety.app.data.model.Vaccination
import com.petsafety.app.data.model.VaccinationHomeSummary
import kotlinx.serialization.Serializable

/**
 * Envelope payloads for the vaccination endpoints. The shared [ApiEnvelope]
 * wraps these as `data.{...}`, so e.g. the list arrives as
 * `data.vaccinations` and the summary as `data.summary` — matching the
 * locked A.3 / A.7 wire contracts.
 */
@Serializable
data class VaccinationsResponse(
    val vaccinations: List<Vaccination> = emptyList()
)

@Serializable
data class VaccinationSummaryResponse(
    val summary: VaccinationHomeSummary
)
