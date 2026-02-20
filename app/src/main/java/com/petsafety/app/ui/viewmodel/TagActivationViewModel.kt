package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.model.QrTag
import com.petsafety.app.data.repository.PetsRepository
import com.petsafety.app.data.repository.QrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ActivationState {
    data object Idle : ActivationState()
    data object Loading : ActivationState()
    data class Success(val tag: QrTag, val petName: String) : ActivationState()
    data class Error(val message: String) : ActivationState()
}

@HiltViewModel
class TagActivationViewModel @Inject constructor(
    private val petsRepository: PetsRepository,
    private val qrRepository: QrRepository
) : ViewModel() {
    private val _pets = MutableStateFlow<List<Pet>>(emptyList())
    val pets: StateFlow<List<Pet>> = _pets.asStateFlow()

    private val _isLoadingPets = MutableStateFlow(false)
    val isLoadingPets: StateFlow<Boolean> = _isLoadingPets.asStateFlow()

    private val _activationState = MutableStateFlow<ActivationState>(ActivationState.Idle)
    val activationState: StateFlow<ActivationState> = _activationState.asStateFlow()

    private val _selectedPetId = MutableStateFlow<String?>(null)
    val selectedPetId: StateFlow<String?> = _selectedPetId.asStateFlow()

    fun fetchPets() {
        viewModelScope.launch {
            _isLoadingPets.value = true
            try {
                val result = petsRepository.fetchPets()
                _pets.value = result.first
            } catch (_: Exception) {
                _pets.value = emptyList()
            } finally {
                _isLoadingPets.value = false
            }
        }
    }

    fun selectPet(petId: String) {
        _selectedPetId.value = petId
    }

    fun activateTag(qrCode: String) {
        val petId = _selectedPetId.value ?: return
        val petName = _pets.value.firstOrNull { it.id == petId }?.name ?: ""
        viewModelScope.launch {
            _activationState.value = ActivationState.Loading
            try {
                val tag = qrRepository.activateTag(qrCode, petId)
                _activationState.value = ActivationState.Success(tag, petName)
            } catch (ex: Exception) {
                _activationState.value = ActivationState.Error(
                    ex.localizedMessage ?: "Failed to activate tag"
                )
            }
        }
    }

    fun resetActivation() {
        _activationState.value = ActivationState.Idle
    }
}
