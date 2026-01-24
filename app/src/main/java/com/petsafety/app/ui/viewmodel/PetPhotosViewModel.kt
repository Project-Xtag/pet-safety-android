package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.PetPhoto
import com.petsafety.app.data.repository.PhotosRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PetPhotosViewModel @Inject constructor(
    private val repository: PhotosRepository
) : ViewModel() {
    private val _photos = MutableStateFlow<List<PetPhoto>>(emptyList())
    val photos: StateFlow<List<PetPhoto>> = _photos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isUploading = MutableStateFlow(false)
    val isUploading: StateFlow<Boolean> = _isUploading.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0f)
    val uploadProgress: StateFlow<Float> = _uploadProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadPhotos(petId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _photos.value = repository.getPetPhotos(petId).sortedByDescending { it.uploadedAt }
            } catch (ex: Exception) {
                _errorMessage.value = ex.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun uploadPhoto(petId: String, bytes: ByteArray, isPrimary: Boolean, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            _uploadProgress.value = 0f
            try {
                val photo = repository.uploadPetPhoto(petId, bytes, isPrimary)
                _photos.value = (listOf(photo) + _photos.value).sortedByDescending { it.uploadedAt }
                _uploadProgress.value = 1f
                onResult(true, null)
            } catch (ex: Exception) {
                _errorMessage.value = ex.localizedMessage
                onResult(false, ex.localizedMessage)
            } finally {
                _isUploading.value = false
            }
        }
    }

    fun uploadPhotos(petId: String, images: List<ByteArray>, onResult: (Int, Int) -> Unit) {
        viewModelScope.launch {
            _isUploading.value = true
            var succeeded = 0
            var failed = 0
            images.forEachIndexed { index, bytes ->
                try {
                    repository.uploadPetPhoto(petId, bytes, false)
                    succeeded++
                } catch (_: Exception) {
                    failed++
                } finally {
                    _uploadProgress.value = (index + 1).toFloat() / images.size
                }
            }
            loadPhotos(petId)
            _isUploading.value = false
            _uploadProgress.value = 0f
            onResult(succeeded, failed)
        }
    }

    fun setPrimaryPhoto(petId: String, photoId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                repository.setPrimaryPhoto(petId, photoId)
                _photos.value = _photos.value.map { it.copy(isPrimary = it.id == photoId) }
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            }
        }
    }

    fun deletePhoto(petId: String, photoId: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                repository.deletePhoto(petId, photoId)
                _photos.value = _photos.value.filterNot { it.id == photoId }
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            }
        }
    }

    fun reorderPhotos(petId: String, photoIds: List<String>, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                repository.reorderPhotos(petId, photoIds)
                loadPhotos(petId)
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            }
        }
    }
}
