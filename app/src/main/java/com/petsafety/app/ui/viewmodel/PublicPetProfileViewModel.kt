package com.petsafety.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.R
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.repository.LocationConsent
import com.petsafety.app.data.repository.QrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PublicPetProfileViewModel @Inject constructor(
    private val application: Application,
    private val qrRepository: QrRepository
) : ViewModel() {
    private val _pet = MutableStateFlow<Pet?>(null)
    val pet: StateFlow<Pet?> = _pet.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSharing = MutableStateFlow(false)
    val isSharing: StateFlow<Boolean> = _isSharing.asStateFlow()

    private val _shareSuccess = MutableStateFlow(false)
    val shareSuccess: StateFlow<Boolean> = _shareSuccess.asStateFlow()

    private val _shareError = MutableStateFlow<String?>(null)
    val shareError: StateFlow<String?> = _shareError.asStateFlow()

    fun loadPublicProfile(qrCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val lookup = qrRepository.lookupTag(qrCode)
                _pet.value = lookup.pet

                // Log scan and notify owner (fire-and-forget)
                if (lookup.hasPet && lookup.pet != null) {
                    launch {
                        try { qrRepository.scanQr(qrCode) } catch (e: Exception) { Timber.w(e, "scanQr log failed") }
                    }
                }
            } catch (ex: Exception) {
                _errorMessage.value = ex.localizedMessage ?: application.getString(R.string.error_load_pet_profile)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun shareLocation(
        qrCode: String,
        consent: LocationConsent,
        latitude: Double?,
        longitude: Double?,
        accuracyMeters: Double? = null
    ) {
        viewModelScope.launch {
            _isSharing.value = true
            _shareError.value = null
            try {
                val response = qrRepository.shareLocation(qrCode, consent, latitude, longitude, accuracyMeters)
                _shareSuccess.value = true
            } catch (e: Exception) {
                _shareError.value = e.localizedMessage ?: application.getString(R.string.share_location_error)
            } finally {
                _isSharing.value = false
            }
        }
    }

    fun clearShareState() {
        _shareSuccess.value = false
        _shareError.value = null
    }
}
