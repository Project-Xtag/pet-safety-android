package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.CommunityFoundPet
import com.petsafety.app.data.model.MissingPetAlert
import com.petsafety.app.data.repository.AlertsRepository
import com.petsafety.app.data.repository.CommunityFoundPetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

/**
 * Drives the redesigned Lost & Found board (AlertsTabScreen). Loads
 * /alerts/nearby + /community/found-pets/nearby in parallel, exposes
 * filterable feeds + the centre/radius the map renders against.
 *
 * Kept separate from AlertsViewModel because that VM serves the
 * owner's own alerts (create/mark-found/etc.) — different concern.
 */
@HiltViewModel
class LostAndFoundViewModel @Inject constructor(
    private val alertsRepository: AlertsRepository,
    private val foundPetsRepository: CommunityFoundPetsRepository,
) : ViewModel() {

    enum class SpeciesFilter { ALL, DOG, CAT }
    enum class StatusFilter { ALL, MISSING, COMMUNITY }
    enum class ViewMode { LIST, MAP }

    // Raw feeds
    private val _missing = MutableStateFlow<List<MissingPetAlert>>(emptyList())
    val missing: StateFlow<List<MissingPetAlert>> = _missing.asStateFlow()

    private val _found = MutableStateFlow<List<CommunityFoundPet>>(emptyList())
    val found: StateFlow<List<CommunityFoundPet>> = _found.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _searchCenter = MutableStateFlow<Pair<Double, Double>?>(null)
    val searchCenter: StateFlow<Pair<Double, Double>?> = _searchCenter.asStateFlow()

    // UI state
    val viewMode = MutableStateFlow(ViewMode.LIST)
    val query = MutableStateFlow("")
    val speciesFilter = MutableStateFlow(SpeciesFilter.ALL)
    val statusFilter = MutableStateFlow(StatusFilter.ALL)

    val mapRadiusKm: Double = 25.0
    val notificationRadiusKm: Double = 10.0

    /**
     * Filtered feeds — recomputed whenever the raw feed OR any filter
     * changes. Both feeds share the same query/species; status flips
     * one of them off.
     */
    val filteredMissing: StateFlow<List<MissingPetAlert>> =
        combine(_missing, query, speciesFilter, statusFilter) { list, q, sp, st ->
            if (st == StatusFilter.COMMUNITY) return@combine emptyList()
            val qLower = q.trim().lowercase()
            list.filter { alert ->
                if (sp != SpeciesFilter.ALL) {
                    val species = alert.pet?.species?.lowercase()
                    when (sp) {
                        SpeciesFilter.DOG -> if (species != "dog") return@filter false
                        SpeciesFilter.CAT -> if (species != "cat") return@filter false
                        SpeciesFilter.ALL -> Unit
                    }
                }
                if (qLower.isNotEmpty()) {
                    val haystack = listOfNotNull(
                        alert.pet?.name,
                        alert.pet?.breed,
                        alert.lastSeenLocation,
                    ).joinToString(" ").lowercase()
                    if (!haystack.contains(qLower)) return@filter false
                }
                true
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredFound: StateFlow<List<CommunityFoundPet>> =
        combine(_found, query, speciesFilter, statusFilter) { list, q, sp, st ->
            if (st == StatusFilter.MISSING) return@combine emptyList()
            val qLower = q.trim().lowercase()
            list.filter { report ->
                when (sp) {
                    SpeciesFilter.DOG -> if (report.species != CommunityFoundPet.Species.DOG) return@filter false
                    SpeciesFilter.CAT -> if (report.species != CommunityFoundPet.Species.CAT) return@filter false
                    SpeciesFilter.ALL -> Unit
                }
                if (qLower.isNotEmpty()) {
                    val haystack = listOfNotNull(
                        report.breed,
                        report.color,
                        report.foundAddress,
                        report.description,
                    ).joinToString(" ").lowercase()
                    if (!haystack.contains(qLower)) return@filter false
                }
                true
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * Fetch both feeds in parallel. One branch failing doesn't blank the
     * other — surface the first error instead (Promise.allSettled parity
     * with the web's CommunityBoard).
     */
    fun fetchNearby(latitude: Double, longitude: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _searchCenter.value = latitude to longitude

            val missingDeferred = async {
                runCatching {
                    alertsRepository.fetchNearbyAlerts(latitude, longitude, mapRadiusKm)
                        .filter { it.status == "active" }
                }
            }
            val foundDeferred = async {
                runCatching {
                    foundPetsRepository.fetchNearby(latitude, longitude, mapRadiusKm)
                        .filter { it.status == CommunityFoundPet.Status.ACTIVE }
                }
            }
            val missingResult = missingDeferred.await()
            val foundResult = foundDeferred.await()

            _missing.value = missingResult.getOrDefault(emptyList())
            _found.value = foundResult.getOrDefault(emptyList())
            val firstError = listOf(missingResult, foundResult)
                .firstOrNull { it.isFailure }
                ?.exceptionOrNull()
                ?.localizedMessage
            _errorMessage.value = firstError
            _isLoading.value = false
        }
    }

    /** Prepend a freshly-created report so the user sees it immediately. */
    fun prependLocalFoundReport(report: CommunityFoundPet) {
        _found.value = listOf(report) + _found.value
    }
}
