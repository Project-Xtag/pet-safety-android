package com.petsafety.app.data.repository

import com.petsafety.app.data.local.OfflineDataManager
import com.petsafety.app.data.model.Vaccination
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.sync.NetworkMonitor
import kotlinx.coroutines.flow.first

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
}
