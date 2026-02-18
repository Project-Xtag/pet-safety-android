package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.ScanResponse
import com.petsafety.app.data.repository.LocationConsent
import com.petsafety.app.data.repository.QrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TagLookupState {
    data object Idle : TagLookupState()
    data object Loading : TagLookupState()
    data class ActiveWithPet(val scanResponse: ScanResponse) : TagLookupState()
    data class NeedsActivation(val qrCode: String) : TagLookupState()
    data class NotActivated(val message: String) : TagLookupState()
    data class NotFound(val message: String) : TagLookupState()
    data class Error(val message: String) : TagLookupState()
}

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val repository: QrRepository
) : ViewModel() {
    private val _scanResult = MutableStateFlow<ScanResponse?>(null)
    val scanResult: StateFlow<ScanResponse?> = _scanResult.asStateFlow()

    private val _lookupState = MutableStateFlow<TagLookupState>(TagLookupState.Idle)
    val lookupState: StateFlow<TagLookupState> = _lookupState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun lookupAndRoute(code: String) {
        viewModelScope.launch {
            if (_isLoading.value || _lookupState.value is TagLookupState.Loading) return@launch
            _lookupState.value = TagLookupState.Loading
            _isLoading.value = true
            try {
                val lookup = repository.lookupTag(code)
                if (!lookup.exists) {
                    _lookupState.value = TagLookupState.NotFound("This tag was not found.")
                } else if (lookup.status == "active" && lookup.hasPet) {
                    try {
                        val scanResponse = repository.scanQr(code)
                        _scanResult.value = scanResponse
                        _lookupState.value = TagLookupState.ActiveWithPet(scanResponse)
                    } catch (ex: Exception) {
                        _lookupState.value = TagLookupState.Error(
                            ex.localizedMessage ?: "Failed to load pet profile"
                        )
                    }
                } else if (!lookup.hasPet && lookup.isOwner) {
                    _lookupState.value = TagLookupState.NeedsActivation(code)
                } else {
                    _lookupState.value = TagLookupState.NotActivated(
                        "This tag has not been activated yet."
                    )
                }
            } catch (ex: Exception) {
                _lookupState.value = TagLookupState.Error(
                    ex.localizedMessage ?: "Failed to look up tag"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun scanQr(code: String) {
        viewModelScope.launch {
            if (_isLoading.value || _scanResult.value != null) return@launch
            _isLoading.value = true
            try {
                _scanResult.value = repository.scanQr(code)
            } catch (ex: Exception) {
                _errorMessage.value = ex.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun shareLocation(
        qrCode: String,
        consent: LocationConsent,
        latitude: Double? = null,
        longitude: Double? = null,
        accuracyMeters: Double? = null,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.shareLocation(
                    qrCode = qrCode,
                    consent = consent,
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = accuracyMeters
                )
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            }
        }
    }

    @Deprecated("Use shareLocation with LocationConsent", ReplaceWith("shareLocation(qrCode, LocationConsent.PRECISE, latitude, longitude)"))
    fun shareLocation(
        qrCode: String,
        latitude: Double,
        longitude: Double,
        address: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        shareLocation(
            qrCode = qrCode,
            consent = LocationConsent.PRECISE,
            latitude = latitude,
            longitude = longitude,
            onResult = onResult
        )
    }

    fun reset() {
        _scanResult.value = null
        _errorMessage.value = null
        _lookupState.value = TagLookupState.Idle
    }
}
