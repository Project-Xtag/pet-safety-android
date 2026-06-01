package com.petsafety.app.data.network.model

import com.petsafety.app.data.model.Vaccination
import com.petsafety.app.data.model.VaccinationHomeSummary
import com.petsafety.app.data.model.VaccineCatalogEntry
import kotlinx.serialization.Serializable

/**
 * Envelope payloads for the vaccination endpoints. The shared [ApiEnvelope]
 * wraps these as `data.{...}`, so e.g. the list arrives as
 * `data.vaccinations` and the summary as `data.summary` — matching the
 * locked A.3 / A.4 / A.5 / A.7 wire contracts.
 */
@Serializable
data class VaccinationsResponse(
    val vaccinations: List<Vaccination> = emptyList()
)

@Serializable
data class VaccinationSummaryResponse(
    val summary: VaccinationHomeSummary
)

/** A.3 create/update → `data.vaccination`. */
@Serializable
data class VaccinationResponse(
    val vaccination: Vaccination
)

/** A.4 catalog → `data.vaccines`. */
@Serializable
data class VaccineCatalogResponse(
    val vaccines: List<VaccineCatalogEntry> = emptyList()
)

/** A.5 certificate upload → `data.certificate_url`. */
@Serializable
data class CertificateUploadResponse(
    @kotlinx.serialization.SerialName("certificate_url") val certificateUrl: String? = null
)
