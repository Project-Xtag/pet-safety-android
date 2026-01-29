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

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val repository: QrRepository
) : ViewModel() {
    private val _scanResult = MutableStateFlow<ScanResponse?>(null)
    val scanResult: StateFlow<ScanResponse?> = _scanResult.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

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

    /**
     * Share location with 3-tier GDPR consent
     *
     * @param qrCode The scanned QR code
     * @param consent The user's location consent choice
     * @param latitude Current latitude (optional for DECLINE)
     * @param longitude Current longitude (optional for DECLINE)
     * @param accuracyMeters GPS accuracy in meters (optional)
     * @param onResult Callback with success status and optional error message
     */
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

    /**
     * Legacy method for backward compatibility
     * @deprecated Use shareLocation with LocationConsent
     */
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
    }
}
