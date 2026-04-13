package com.petsafety.app.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petsafety.app.data.events.SubscriptionEventBus
import com.petsafety.app.data.model.ReferralUsedEvent
import com.petsafety.app.data.model.SightingReportedEvent
import com.petsafety.app.data.model.SubscriptionChangedEvent
import com.petsafety.app.data.model.TagScannedEvent
import com.petsafety.app.data.model.PetFoundEvent
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.AppConfig
import com.petsafety.app.data.notifications.NotificationHelper
import com.petsafety.app.data.network.SseService
import com.petsafety.app.data.sync.NetworkMonitor
import com.petsafety.app.data.sync.SyncService
import com.petsafety.app.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.petsafety.app.R

@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val sseService: SseService,
    private val notificationHelper: NotificationHelper,
    private val networkMonitor: NetworkMonitor,
    val syncService: SyncService,
    private val stringProvider: StringProvider,
    private val subscriptionEventBus: SubscriptionEventBus,
    private val apiService: ApiService
) : ViewModel() {
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val isConnected: StateFlow<Boolean> = networkMonitor.isConnected

    private val _navigateToAlertId = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigateToAlertId: SharedFlow<String> = _navigateToAlertId.asSharedFlow()

    private val _navigateToSubscription = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val navigateToSubscriptionEvent: SharedFlow<Unit> = _navigateToSubscription.asSharedFlow()

    /**
     * Public runtime config (currently just feature flags). Null while the
     * first fetch is in flight or if it failed entirely. Consumers gating UI
     * on this should treat null as "do not enable the gated action" — see
     * the tagsAvailable convenience below for the canonical fail-closed read.
     */
    private val _appConfig = MutableStateFlow<AppConfig?>(null)
    val appConfig: StateFlow<AppConfig?> = _appConfig.asStateFlow()

    /**
     * True only when the server has confirmed tags are in stock. Loading
     * (null) and any fetch failure both return false — fail-closed, matching
     * the backend gate that returns 503 TAGS_UNAVAILABLE.
     */
    val tagsAvailable: StateFlow<Boolean> = MutableStateFlow(false).also { sf ->
        viewModelScope.launch {
            _appConfig.collect { sf.value = it?.tagsAvailable == true }
        }
    }.asStateFlow()

    init {
        setupSseHandlers()
        // Fire-and-forget config fetch on construction. Views observe
        // appConfig / tagsAvailable and re-render when the value lands.
        refreshAppConfig()
    }

    /**
     * Re-fetches /api/config. Safe to call repeatedly (e.g. when the app
     * resumes from background) so a stockout flip reaches users without a
     * full app restart. Failure leaves the previous value in place rather
     * than regressing a known-good config to null.
     */
    fun refreshAppConfig() {
        viewModelScope.launch {
            try {
                _appConfig.value = apiService.getAppConfig()
            } catch (e: Exception) {
                Log.w("AppStateViewModel", "Failed to load app config", e)
                // Leave _appConfig as-is. If still null we stay fail-closed.
            }
        }
    }

    fun showError(message: String) {
        _snackbarMessage.value = message
    }

    fun showSuccess(message: String) {
        _snackbarMessage.value = message
    }

    fun clearMessage() {
        _snackbarMessage.value = null
    }

    fun setLoading(isLoading: Boolean) {
        _isLoading.value = isLoading
    }

    fun navigateToAlert(alertId: String) {
        _navigateToAlertId.tryEmit(alertId)
    }

    fun navigateToSubscription() {
        _navigateToSubscription.tryEmit(Unit)
    }

    fun refreshSubscription() {
        subscriptionEventBus.requestRefresh()
    }

    fun connectSse() {
        sseService.connect()
    }

    fun disconnectSse() {
        sseService.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        // Clean up SSE handlers to prevent memory leaks and stale callbacks
        sseService.onTagScanned = null
        sseService.onSightingReported = null
        sseService.onPetFound = null
        sseService.onAlertCreated = null
        sseService.onAlertUpdated = null
        sseService.onSubscriptionChanged = null
        sseService.onReferralUsed = null
        sseService.onConnected = null
        sseService.onConnectionLost = null
        sseService.disconnect()
    }

    private fun setupSseHandlers() {
        sseService.onTagScanned = { event: TagScannedEvent ->
            val location = event.address ?: stringProvider.getString(R.string.unknown_location)
            val message = stringProvider.getString(R.string.tag_scanned_message, event.petName, location)
            showSuccess(message)
            notificationHelper.showNotification(
                stringProvider.getString(R.string.notification_tag_scanned_title, event.petName),
                message
            )
        }
        sseService.onSightingReported = { event: SightingReportedEvent ->
            val message = if (!event.address.isNullOrBlank()) {
                stringProvider.getString(R.string.sighting_message_with_location, event.petName, event.address)
            } else {
                stringProvider.getString(R.string.sighting_message_no_location, event.petName)
            }
            showSuccess(message)
            notificationHelper.showNotification(
                stringProvider.getString(R.string.notification_sighting_title, event.petName),
                message
            )
        }
        sseService.onPetFound = { event: PetFoundEvent ->
            val message = stringProvider.getString(R.string.pet_found_message, event.petName)
            showSuccess(message)
            notificationHelper.showNotification(
                stringProvider.getString(R.string.notification_pet_found_title),
                message
            )
        }
        sseService.onSubscriptionChanged = { event: SubscriptionChangedEvent ->
            val message = if (event.status == "active") {
                stringProvider.getString(R.string.sse_subscription_activated, event.planName)
            } else {
                stringProvider.getString(R.string.sse_subscription_cancelled, event.planName)
            }
            showSuccess(message)
            subscriptionEventBus.requestRefresh()
        }
        sseService.onReferralUsed = { event: ReferralUsedEvent ->
            val name = event.refereeName ?: event.refereeEmail ?: stringProvider.getString(R.string.someone_fallback)
            showSuccess(stringProvider.getString(R.string.sse_referral_used, name))
        }
        sseService.onConnectionLost = {
            showError(stringProvider.getString(R.string.sse_connection_lost))
        }
    }
}
