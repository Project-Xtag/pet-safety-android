package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.PendingRegistration
import com.petsafety.app.data.repository.OrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PendingRegistrationsViewModel @Inject constructor(
    private val ordersRepository: OrdersRepository
) : ViewModel() {

    private val _registrations = MutableStateFlow<List<PendingRegistration>>(emptyList())
    val registrations = _registrations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun fetchPendingRegistrations() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                _registrations.value = ordersRepository.getPendingRegistrations()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load pending registrations"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            try {
                _registrations.value = ordersRepository.getPendingRegistrations()
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load pending registrations"
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
