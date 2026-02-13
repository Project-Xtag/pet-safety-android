package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.events.SubscriptionEventBus
import com.petsafety.app.data.model.Invoice
import com.petsafety.app.data.model.Referral
import com.petsafety.app.data.model.ReferralCode
import com.petsafety.app.data.model.SubscriptionPlan
import com.petsafety.app.data.model.UserSubscription
import com.petsafety.app.data.network.model.SubscriptionFeaturesResponse
import com.petsafety.app.data.repository.SubscriptionRepository
import com.petsafety.app.util.StringProvider
import com.petsafety.app.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    private val subscriptionEventBus: SubscriptionEventBus,
    private val stringProvider: StringProvider
) : ViewModel() {

    private val _plans = MutableStateFlow<List<SubscriptionPlan>>(emptyList())
    val plans: StateFlow<List<SubscriptionPlan>> = _plans

    private val _subscription = MutableStateFlow<UserSubscription?>(null)
    val subscription: StateFlow<UserSubscription?> = _subscription

    private val _features = MutableStateFlow<SubscriptionFeaturesResponse?>(null)
    val features: StateFlow<SubscriptionFeaturesResponse?> = _features

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices: StateFlow<List<Invoice>> = _invoices

    private val _referralCode = MutableStateFlow<ReferralCode?>(null)
    val referralCode: StateFlow<ReferralCode?> = _referralCode

    private val _referrals = MutableStateFlow<List<Referral>>(emptyList())
    val referrals: StateFlow<List<Referral>> = _referrals

    private val _checkoutUrl = MutableStateFlow<String?>(null)
    val checkoutUrl: StateFlow<String?> = _checkoutUrl

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val currentPlanName: String get() = _subscription.value?.resolvedPlanName ?: stringProvider.getString(R.string.no_plan)
    val isOnStarterPlan: Boolean get() = _subscription.value?.resolvedPlanName?.lowercase() == "starter"

    init {
        // Auto-refresh when SSE subscription_changed event arrives
        viewModelScope.launch {
            subscriptionEventBus.refreshEvents.collect {
                loadSubscription()
                loadFeatures()
            }
        }
    }

    fun loadPlans() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _plans.value = repository.getPlans()
            } catch (e: Exception) {
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    fun loadSubscription() {
        viewModelScope.launch {
            try {
                _subscription.value = repository.getMySubscription()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadFeatures() {
        viewModelScope.launch {
            try {
                _features.value = repository.getFeatures()
            } catch (e: Exception) {
                _error.value = e.message
            }
        }
    }

    fun loadAll() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _plans.value = repository.getPlans()
                _subscription.value = repository.getMySubscription()
                _features.value = repository.getFeatures()
            } catch (e: Exception) {
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    fun selectPlan(plan: SubscriptionPlan, billingPeriod: String = "monthly", countryCode: String? = null) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            try {
                if (plan.isFree) {
                    _subscription.value = repository.upgradeToStarter()
                } else {
                    val url = repository.createCheckoutSession(plan.name, billingPeriod, countryCode = countryCode)
                    _checkoutUrl.value = url
                }
            } catch (e: Exception) {
                _error.value = e.message
            }
            _isProcessing.value = false
        }
    }

    fun cancelSubscription() {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            try {
                _subscription.value = repository.cancelSubscription()
            } catch (e: Exception) {
                _error.value = e.message
            }
            _isProcessing.value = false
        }
    }

    fun handleCheckoutComplete() {
        _checkoutUrl.value = null
        loadSubscription()
        loadFeatures()
    }

    fun handleCheckoutCancelled() {
        _checkoutUrl.value = null
    }

    // Billing
    fun loadInvoices() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _invoices.value = repository.getInvoices()
            } catch (e: Exception) {
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    fun openPortal(onUrl: (String) -> Unit) {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            try {
                val url = repository.createPortalSession()
                onUrl(url)
            } catch (e: Exception) {
                _error.value = e.message
            }
            _isProcessing.value = false
        }
    }

    // Referrals
    fun loadReferralStatus() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (code, referrals) = repository.getReferralStatus()
                _referralCode.value = code
                _referrals.value = referrals
            } catch (e: Exception) {
                _error.value = e.message
            }
            _isLoading.value = false
        }
    }

    fun generateReferralCode() {
        viewModelScope.launch {
            _isProcessing.value = true
            _error.value = null
            try {
                _referralCode.value = repository.generateReferralCode()
            } catch (e: Exception) {
                _error.value = e.message
            }
            _isProcessing.value = false
        }
    }

    fun clearError() {
        _error.value = null
    }
}
