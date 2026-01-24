package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.User
import com.petsafety.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _showBiometricPrompt = MutableStateFlow(false)
    val showBiometricPrompt: StateFlow<Boolean> = _showBiometricPrompt.asStateFlow()

    private val _biometricEnabled = MutableStateFlow(authRepository.isBiometricEnabled())
    val biometricEnabled: StateFlow<Boolean> = _biometricEnabled.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.isAuthenticated.collect { isAuth ->
                _isAuthenticated.value = isAuth
                if (isAuth) {
                    loadCurrentUser()
                }
            }
        }
        // Check if we should show biometric prompt on startup
        checkBiometricOnStartup()
    }

    private fun checkBiometricOnStartup() {
        if (authRepository.hasStoredToken() && authRepository.isBiometricEnabled()) {
            _showBiometricPrompt.value = true
        }
    }

    fun login(email: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                authRepository.login(email)
                onSuccess()
            } catch (ex: Exception) {
                val message = ex.localizedMessage ?: ex.message ?: "Login failed"
                _errorMessage.value = message
                onFailure(message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun verifyOtp(email: String, code: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val user = authRepository.verifyOtp(email, code)
                _currentUser.value = user
                _isAuthenticated.value = true
                onSuccess()
            } catch (ex: Exception) {
                val message = ex.localizedMessage ?: ex.message ?: "Verification failed"
                _errorMessage.value = message
                onFailure(message)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _currentUser.value = null
            _isAuthenticated.value = false
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        authRepository.setBiometricEnabled(enabled)
        _biometricEnabled.value = enabled
    }

    fun onBiometricSuccess() {
        viewModelScope.launch {
            _showBiometricPrompt.value = false
            loadCurrentUser()
            _isAuthenticated.value = true
        }
    }

    fun onBiometricCancelled() {
        _showBiometricPrompt.value = false
        // User cancelled biometric, show normal login
    }

    fun dismissBiometricPrompt() {
        _showBiometricPrompt.value = false
    }

    fun updateProfile(updates: Map<String, Any>, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = authRepository.updateUser(updates)
                _currentUser.value = user
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteAccount(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                authRepository.deleteAccount()
                _currentUser.value = null
                _isAuthenticated.value = false
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadCurrentUser() {
        try {
            _currentUser.value = authRepository.getCurrentUser()
        } catch (_: Exception) {
            // Token might be invalid; let logout handle in UI
        }
    }
}
