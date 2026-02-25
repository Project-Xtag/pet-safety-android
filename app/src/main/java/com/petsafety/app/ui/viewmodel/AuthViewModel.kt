package com.petsafety.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.R
import com.petsafety.app.data.model.User
import com.petsafety.app.data.network.TokenAuthenticator
import com.petsafety.app.data.network.model.CanDeleteAccountResponse
import com.petsafety.app.data.fcm.FCMRepository
import com.petsafety.app.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import io.sentry.Sentry
import io.sentry.protocol.User as SentryUser
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val application: Application,
    private val authRepository: AuthRepository,
    private val fcmRepository: FCMRepository
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

    // Event emitted when session expires and user needs to re-authenticate
    private val _sessionExpiredEvent = MutableSharedFlow<String>()
    val sessionExpiredEvent: SharedFlow<String> = _sessionExpiredEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.isAuthenticated.collect { isAuth ->
                _isAuthenticated.value = isAuth
                if (isAuth) {
                    loadCurrentUser()
                    registerFCMToken()
                }
            }
        }
        // Listen for auth expiration events from TokenAuthenticator
        viewModelScope.launch {
            TokenAuthenticator.authExpiredEvent.collect {
                // Perform full logout to unregister FCM token and clean up state
                authRepository.logout()
                _isAuthenticated.value = false
                _currentUser.value = null
                if (Sentry.isEnabled()) { Sentry.setUser(null) }
                _sessionExpiredEvent.emit(application.getString(R.string.error_session_expired_message))
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
                val message = ex.localizedMessage ?: ex.message ?: application.getString(R.string.error_login_failed)
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
                // Set Sentry user context
                if (Sentry.isEnabled()) {
                    Sentry.setUser(SentryUser().apply { id = user.id })
                }
                registerFCMToken()
                onSuccess()
            } catch (ex: Exception) {
                val message = ex.localizedMessage ?: ex.message ?: application.getString(R.string.error_verification_failed)
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
            // Clear Sentry user context
            if (Sentry.isEnabled()) {
                Sentry.setUser(null)
            }
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
            registerFCMToken()
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

    fun canDeleteAccount(onResult: (CanDeleteAccountResponse?, String?) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = authRepository.canDeleteAccount()
                onResult(response, null)
            } catch (ex: Exception) {
                onResult(null, ex.localizedMessage)
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

    fun submitSupportRequest(
        category: String,
        subject: String,
        message: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val ticketId = authRepository.submitSupportRequest(category, subject, message)
                onSuccess(ticketId)
            } catch (ex: Exception) {
                onError(ex.localizedMessage ?: application.getString(R.string.error_submit_support))
            }
        }
    }

    private fun registerFCMToken() {
        viewModelScope.launch {
            try {
                fcmRepository.registerToken()
                Timber.d("FCM token registered after auth")
            } catch (e: Exception) {
                Timber.w("Failed to register FCM token after auth: ${e.message}")
            }
        }
    }

    private suspend fun loadCurrentUser() {
        try {
            val user = authRepository.getCurrentUser()
            _currentUser.value = user
            // Set Sentry user context for error tracking
            if (user != null && Sentry.isEnabled()) {
                Sentry.setUser(SentryUser().apply { id = user.id })
            }
        } catch (_: Exception) {
            // Token might be invalid; let logout handle in UI
        }
    }
}
