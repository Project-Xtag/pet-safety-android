package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.app.Application
import com.petsafety.app.R
import com.petsafety.app.data.model.ScanResponse
import com.petsafety.app.data.model.TagLookupResponse
import com.petsafety.app.data.repository.LocationConsent
import com.petsafety.app.data.repository.QrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

sealed class TagLookupState {
    data object Idle : TagLookupState()
    data object Loading : TagLookupState()
    data class ActiveWithPet(val lookup: TagLookupResponse, val qrCode: String) : TagLookupState()
    data class NeedsActivation(val qrCode: String) : TagLookupState()
    data class NotActivated(val message: String) : TagLookupState()
    data class NotFound(val message: String) : TagLookupState()
    data class Error(val message: String) : TagLookupState()
    data class PromoClaimAvailable(val code: String, val shelterName: String, val promoDurationMonths: Int) : TagLookupState()
}

@HiltViewModel
class QrScannerViewModel @Inject constructor(
    private val application: Application,
    private val repository: QrRepository
) : ViewModel() {
    private val _scanResult = MutableStateFlow<TagLookupResponse?>(null)
    val scanResult: StateFlow<TagLookupResponse?> = _scanResult.asStateFlow()

    private val _lookupState = MutableStateFlow<TagLookupState>(TagLookupState.Idle)
    val lookupState: StateFlow<TagLookupState> = _lookupState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _processingGuard = AtomicBoolean(false)

    fun lookupAndRoute(code: String) {
        viewModelScope.launch {
            if (!_processingGuard.compareAndSet(false, true)) return@launch
            _lookupState.value = TagLookupState.Loading
            _isLoading.value = true
            try {
                val lookup = repository.lookupTag(code)
                if (lookup.canClaimPromo == true && lookup.promo?.batchExpired != true) {
                    _lookupState.value = TagLookupState.PromoClaimAvailable(
                        code = code,
                        shelterName = lookup.promo?.shelterName ?: "",
                        promoDurationMonths = lookup.promo?.promoDurationMonths ?: 3
                    )
                } else if (!lookup.exists) {
                    _lookupState.value = TagLookupState.NotFound(application.getString(R.string.error_tag_not_found))
                } else if (lookup.status == "active" && lookup.hasPet && lookup.pet != null) {
                    _scanResult.value = lookup
                    _lookupState.value = TagLookupState.ActiveWithPet(lookup, code)
                    // Fire-and-forget scan to log + notify owner
                    launch {
                        try { repository.scanQr(code) } catch (_: Exception) { }
                    }
                } else if (!lookup.hasPet && lookup.canActivate) {
                    _lookupState.value = TagLookupState.NeedsActivation(code)
                } else {
                    _lookupState.value = TagLookupState.NotActivated(
                        application.getString(R.string.error_tag_not_activated)
                    )
                }
            } catch (ex: Exception) {
                _lookupState.value = TagLookupState.Error(
                    ex.localizedMessage ?: application.getString(R.string.error_tag_lookup_failed)
                )
            } finally {
                _isLoading.value = false
                _processingGuard.set(false)
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
        _processingGuard.set(false)
    }
}
