package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.NotificationPreferences
import com.petsafety.app.data.repository.NotificationPreferencesRepository
import com.petsafety.app.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.petsafety.app.R

@HiltViewModel
class NotificationPreferencesViewModel @Inject constructor(
    private val repository: NotificationPreferencesRepository,
    private val stringProvider: StringProvider
) : ViewModel() {
    private val _preferences = MutableStateFlow(NotificationPreferences.default)
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    private val _original = MutableStateFlow(NotificationPreferences.default)
    val original: StateFlow<NotificationPreferences> = _original.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showSuccess = MutableStateFlow(false)
    val showSuccess: StateFlow<Boolean> = _showSuccess.asStateFlow()

    val hasChanges: Boolean
        get() = _preferences.value != _original.value

    fun loadPreferences() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val prefs = repository.getPreferences()
                _preferences.value = prefs
                _original.value = prefs
            } catch (ex: Exception) {
                _errorMessage.value = ex.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePreferences(updated: NotificationPreferences) {
        _preferences.value = updated
    }

    fun savePreferences() {
        if (!_preferences.value.isValid) {
            _errorMessage.value = stringProvider.getString(R.string.notification_method_required)
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val prefs = repository.updatePreferences(_preferences.value)
                _preferences.value = prefs
                _original.value = prefs
                _showSuccess.value = true
            } catch (ex: Exception) {
                _errorMessage.value = ex.localizedMessage
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _showSuccess.value = false
    }
}
