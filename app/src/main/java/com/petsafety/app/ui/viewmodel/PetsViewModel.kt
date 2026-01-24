package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.Breed
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.network.model.CreatePetRequest
import com.petsafety.app.data.network.model.UpdatePetRequest
import com.petsafety.app.data.repository.OfflineQueuedException
import com.petsafety.app.data.repository.PetsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PetsViewModel @Inject constructor(
    private val repository: PetsRepository
) : ViewModel() {
    private val _pets = MutableStateFlow<List<Pet>>(emptyList())
    val pets: StateFlow<List<Pet>> = _pets.asStateFlow()

    private val _breeds = MutableStateFlow<List<Breed>>(emptyList())
    val breeds: StateFlow<List<Breed>> = _breeds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchPets() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.fetchPets()
            _pets.value = result.first
            _errorMessage.value = result.second
            _isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.fetchPets()
            _pets.value = result.first
            _errorMessage.value = result.second
            _isRefreshing.value = false
        }
    }

    fun fetchBreeds(species: String) {
        viewModelScope.launch {
            try {
                _breeds.value = repository.getBreedsBySpecies(species)
            } catch (ex: Exception) {
                _breeds.value = emptyList()
            }
        }
    }

    fun createPet(request: CreatePetRequest, onResult: (Pet?, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val pet = repository.createPet(request)
                _pets.value = _pets.value + pet
                onResult(pet, null)
            } catch (ex: Exception) {
                onResult(null, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePet(id: String, request: UpdatePetRequest, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val pet = repository.updatePet(id, request)
                _pets.value = _pets.value.map { if (it.id == id) pet else it }
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePet(id: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.deletePet(id)
                _pets.value = _pets.value.filterNot { it.id == id }
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadPhoto(petId: String, bytes: ByteArray, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val pet = repository.uploadProfilePhoto(petId, bytes)
                _pets.value = _pets.value.map { if (it.id == petId) pet else it }
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markPetMissing(
        petId: String,
        location: LocationCoordinate?,
        address: String?,
        description: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.markPetMissing(petId, location, address, description)
                result.getOrThrow()
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

    fun markPetFound(petId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.markPetFound(petId)
                result.getOrThrow()
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
