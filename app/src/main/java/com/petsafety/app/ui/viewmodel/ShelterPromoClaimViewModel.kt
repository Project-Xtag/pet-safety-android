package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.ClaimPromoTagResponse
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.network.model.CreatePetRequest
import com.petsafety.app.data.repository.PetsRepository
import com.petsafety.app.data.repository.QrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShelterPromoClaimViewModel @Inject constructor(
    private val qrRepository: QrRepository,
    private val petsRepository: PetsRepository
) : ViewModel() {

    sealed class ClaimState {
        data object Idle : ClaimState()
        data object Loading : ClaimState()
        data class Success(val response: ClaimPromoTagResponse) : ClaimState()
        data class Error(val message: String) : ClaimState()
    }

    private val _claimState = MutableStateFlow<ClaimState>(ClaimState.Idle)
    val claimState: StateFlow<ClaimState> = _claimState.asStateFlow()

    private val _pets = MutableStateFlow<List<Pet>>(emptyList())
    val pets: StateFlow<List<Pet>> = _pets.asStateFlow()

    private val _isLoadingPets = MutableStateFlow(false)
    val isLoadingPets: StateFlow<Boolean> = _isLoadingPets.asStateFlow()

    fun loadPets() {
        viewModelScope.launch {
            _isLoadingPets.value = true
            try {
                _pets.value = petsRepository.fetchPets().first
            } catch (_: Exception) {
                // Non-blocking
            }
            _isLoadingPets.value = false
        }
    }

    fun claimWithNewPet(qrCode: String, petData: CreatePetRequest) {
        viewModelScope.launch {
            _claimState.value = ClaimState.Loading
            try {
                val response = qrRepository.claimPromoTag(qrCode, pet = petData)
                _claimState.value = ClaimState.Success(response)
            } catch (e: Exception) {
                _claimState.value = ClaimState.Error(e.message ?: "Failed to claim tag")
            }
        }
    }

    fun claimWithExistingPet(qrCode: String, petId: String) {
        viewModelScope.launch {
            _claimState.value = ClaimState.Loading
            try {
                val response = qrRepository.claimPromoTag(qrCode, petId = petId)
                _claimState.value = ClaimState.Success(response)
            } catch (e: Exception) {
                _claimState.value = ClaimState.Error(e.message ?: "Failed to claim tag")
            }
        }
    }

    fun reset() {
        _claimState.value = ClaimState.Idle
    }
}
