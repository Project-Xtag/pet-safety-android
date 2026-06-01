package com.petsafety.app.data.repository

import com.petsafety.app.data.local.OfflineDataManager
import com.petsafety.app.data.model.CreateVaccinationRequest
import com.petsafety.app.data.model.Vaccination
import com.petsafety.app.data.model.VaccineCatalogEntry
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.sync.NetworkMonitor
import com.petsafety.app.util.VaccinationCertificateEncoder
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Per-pet vaccination data. Offline-first reads: online fetches the server
 * truth and refreshes the cache; offline serves the last cached set.
 *
 * This repository is feature-agnostic. A 404 from these endpoints is a
 * GENUINE not-found and is allowed to propagate as an exception — it is NEVER
 * re-interpreted as "feature off". Feature availability is owned solely by
 * VaccinationGate, anchored on the home-summary endpoint.
 */
class VaccinationRepository(
    private val apiService: ApiService,
    private val offlineManager: OfflineDataManager,
    private val networkMonitor: NetworkMonitor
) {
    suspend fun fetchVaccinations(petId: String): List<Vaccination> {
        networkMonitor.refreshStatus()
        return if (networkMonitor.isConnected.first()) {
            val list = apiService.getVaccinations(petId).data?.vaccinations ?: emptyList()
            offlineManager.saveVaccinations(petId, list)
            list
        } else {
            offlineManager.fetchVaccinations(petId)
        }
    }

    /** A.4 catalog — public, online-only (no offline cache; it's hot on form open). */
    suspend fun fetchCatalog(species: String, country: String): List<VaccineCatalogEntry> =
        apiService.getVaccineCatalog(
            species = species.lowercase(),
            country = country.uppercase()
        ).data?.vaccines ?: emptyList()

    /** A.3 create. Refreshes the per-pet cache so the pet-detail section reflects it. */
    suspend fun create(petId: String, request: CreateVaccinationRequest): Vaccination {
        val created = apiService.createVaccination(petId, request).data?.vaccination
            ?: error("Missing vaccination in create response")
        // Re-pull so the cache (and the pet-detail section) include the new row.
        runCatching { fetchVaccinations(petId) }
        return created
    }

    /**
     * A.5 certificate upload. Takes the already-encoded bytes (the caller runs
     * [VaccinationCertificateEncoder] first) so the Part carries a CONCRETE
     * MIME, never a wildcard. Returns the stored certificate URL.
     */
    suspend fun uploadCertificate(
        petId: String,
        vaccinationId: String,
        encoded: VaccinationCertificateEncoder.Encoded
    ): String? {
        val ext = when (encoded.mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "jpg"
        }
        val body = encoded.data.toRequestBody(encoded.mimeType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "cert.$ext", body)
        val url = apiService.uploadVaccinationCertificate(petId, vaccinationId, part).data?.certificateUrl
        runCatching { fetchVaccinations(petId) }
        return url
    }
}
