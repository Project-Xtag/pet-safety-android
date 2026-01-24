package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.MissingPetAlert
import com.petsafety.app.data.repository.AlertsRepository
import com.petsafety.app.data.repository.OfflineQueuedException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val repository: AlertsRepository
) : ViewModel() {
    private val _alerts = MutableStateFlow<List<MissingPetAlert>>(emptyList())
    val alerts: StateFlow<List<MissingPetAlert>> = _alerts.asStateFlow()

    private val _missingAlerts = MutableStateFlow<List<MissingPetAlert>>(emptyList())
    val missingAlerts: StateFlow<List<MissingPetAlert>> = _missingAlerts.asStateFlow()

    private val _foundAlerts = MutableStateFlow<List<MissingPetAlert>>(emptyList())
    val foundAlerts: StateFlow<List<MissingPetAlert>> = _foundAlerts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var lastLatitude: Double = 51.5074
    private var lastLongitude: Double = -0.1278
    private var lastRadiusKm: Double = 10.0

    fun fetchAlerts() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.fetchAlerts()
            _alerts.value = result.first
            _errorMessage.value = result.second
            _isLoading.value = false
        }
    }

    fun fetchNearbyAlerts(latitude: Double, longitude: Double, radiusKm: Double) {
        lastLatitude = latitude
        lastLongitude = longitude
        lastRadiusKm = radiusKm
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val alerts = repository.fetchNearbyAlerts(latitude, longitude, radiusKm)
                _missingAlerts.value = alerts.filter { it.status == "active" }
                _foundAlerts.value = alerts.filter { it.status == "found" }
            } catch (ex: Exception) {
                _errorMessage.value = ex.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val alerts = repository.fetchNearbyAlerts(lastLatitude, lastLongitude, lastRadiusKm)
                _missingAlerts.value = alerts.filter { it.status == "active" }
                _foundAlerts.value = alerts.filter { it.status == "found" }
            } catch (ex: Exception) {
                _errorMessage.value = ex.localizedMessage
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun createAlert(
        petId: String,
        location: String?,
        coordinate: LocationCoordinate?,
        additionalInfo: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.createAlert(petId, location, coordinate, additionalInfo).getOrThrow()
                onResult(true, null)
            } catch (ex: OfflineQueuedException) {
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateAlertStatus(id: String, status: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.updateAlertStatus(id, status)
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reportSighting(
        alertId: String,
        reporterName: String?,
        reporterPhone: String?,
        reporterEmail: String?,
        location: String?,
        coordinate: LocationCoordinate?,
        notes: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.reportSighting(
                    alertId,
                    reporterName,
                    reporterPhone,
                    reporterEmail,
                    location,
                    coordinate,
                    notes
                ).getOrThrow()
                onResult(true, null)
            } catch (ex: OfflineQueuedException) {
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
