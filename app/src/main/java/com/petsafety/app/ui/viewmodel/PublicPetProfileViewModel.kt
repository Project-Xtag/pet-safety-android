package com.petsafety.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.R
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.repository.QrRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    fun loadPublicProfile(qrCode: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val response = qrRepository.scanQr(qrCode)
                _pet.value = response.pet
            } catch (ex: Exception) {
                _errorMessage.value = ex.localizedMessage ?: application.getString(R.string.error_load_pet_profile)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
