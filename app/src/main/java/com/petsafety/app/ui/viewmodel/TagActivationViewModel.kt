package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.model.QrTag
import com.petsafety.app.data.model.UnactivatedOrderItem
import com.petsafety.app.data.repository.OrdersRepository
import com.petsafety.app.data.repository.PetsRepository
import com.petsafety.app.data.repository.QrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
    private val qrRepository: QrRepository,
    private val ordersRepository: OrdersRepository
) : ViewModel() {
    private val _pets = MutableStateFlow<List<Pet>>(emptyList())
    val pets: StateFlow<List<Pet>> = _pets.asStateFlow()

    private val _isLoadingPets = MutableStateFlow(false)
    val isLoadingPets: StateFlow<Boolean> = _isLoadingPets.asStateFlow()

    private val _activationState = MutableStateFlow<ActivationState>(ActivationState.Idle)
    val activationState: StateFlow<ActivationState> = _activationState.asStateFlow()

    private val _selectedPetId = MutableStateFlow<String?>(null)
    val selectedPetId: StateFlow<String?> = _selectedPetId.asStateFlow()

    private val _orderItems = MutableStateFlow<List<UnactivatedOrderItem>>(emptyList())
    val orderItems: StateFlow<List<UnactivatedOrderItem>> = _orderItems.asStateFlow()

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

    fun loadActivationData(qrCode: String) {
        viewModelScope.launch {
            _isLoadingPets.value = true
            try {
                val petsDeferred = async { petsRepository.fetchPets() }
                val orderDeferred = async {
                    try { ordersRepository.getUnactivatedTagsForQRCode(qrCode) } catch (_: Exception) { emptyList() }
                }
                _pets.value = petsDeferred.await().first
                _orderItems.value = orderDeferred.await()
            } catch (_: Exception) {
                _pets.value = emptyList()
                _orderItems.value = emptyList()
            } finally {
                _isLoadingPets.value = false
            }
        }
    }

    fun selectPet(petId: String) {
        _selectedPetId.value = petId
    }

    fun activateTag(qrCode: String) {
        val petId = _selectedPetId.value ?: run {
            _activationState.value = ActivationState.Error("Please select a pet first")
            return
        }
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

    fun getMatchingPets(): List<Pet> {
        val orderNames = _orderItems.value.mapNotNull { it.petName?.lowercase() }
        return _pets.value.filter { it.name.lowercase() in orderNames }
    }

    fun getUnmatchedOrderNames(): List<String> {
        val petNames = _pets.value.map { it.name.lowercase() }
        return _orderItems.value.mapNotNull { it.petName }.filter { it.lowercase() !in petNames }
    }

    fun getOtherPets(): List<Pet> {
        val orderNames = _orderItems.value.mapNotNull { it.petName?.lowercase() }
        return _pets.value.filter { it.name.lowercase() !in orderNames }
    }

    fun hasOrderContext(): Boolean = _orderItems.value.isNotEmpty()

    /// Refresh pets after creation and auto-activate the tag for the newly created pet.
    fun refreshAndAutoActivate(qrCode: String, previousPetIds: Set<String>) {
        viewModelScope.launch {
            try {
                val result = petsRepository.fetchPets()
                _pets.value = result.first
                val newPet = _pets.value.firstOrNull { it.id !in previousPetIds }
                if (newPet != null) {
                    _selectedPetId.value = newPet.id
                    _activationState.value = ActivationState.Loading
                    try {
                        val tag = qrRepository.activateTag(qrCode, newPet.id)
                        _activationState.value = ActivationState.Success(tag, newPet.name)
                    } catch (ex: Exception) {
                        _activationState.value = ActivationState.Error(
                            ex.localizedMessage ?: "Failed to activate tag"
                        )
                    }
                }
            } catch (_: Exception) {
                // Pet refresh failed — user can still activate manually
            }
        }
    }
}
