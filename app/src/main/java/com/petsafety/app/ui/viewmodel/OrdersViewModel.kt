package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.model.Order
import com.petsafety.app.data.network.model.CreateReplacementOrderRequest
import com.petsafety.app.data.network.model.CreateTagOrderRequest
import com.petsafety.app.data.repository.OrdersRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val repository: OrdersRepository
) : ViewModel() {
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun fetchOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _orders.value = repository.getOrders()
            } catch (ex: Exception) {
                _errorMessage.value = ex.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                _orders.value = repository.getOrders()
            } catch (ex: Exception) {
                _errorMessage.value = ex.localizedMessage
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun createOrder(
        request: CreateTagOrderRequest,
        onResult: (com.petsafety.app.data.network.model.CreateTagOrderResponse?, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = repository.createTagOrder(request)
                onResult(response, null)
            } catch (ex: Exception) {
                onResult(null, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createReplacementOrder(
        petId: String,
        request: CreateReplacementOrderRequest,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                repository.createReplacementOrder(petId, request)
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createPaymentIntent(
        orderId: String,
        amount: Double,
        email: String?,
        paymentMethod: String?,
        currency: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.createPaymentIntent(orderId, amount, email, paymentMethod, currency)
                onResult(true, null)
            } catch (ex: Exception) {
                onResult(false, ex.localizedMessage)
            }
        }
    }
}
