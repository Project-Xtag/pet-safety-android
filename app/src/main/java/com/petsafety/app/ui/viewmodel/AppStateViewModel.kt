package com.petsafety.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.petsafety.app.data.events.SubscriptionEventBus
import com.petsafety.app.data.model.ReferralUsedEvent
import com.petsafety.app.data.model.SightingReportedEvent
import com.petsafety.app.data.model.SubscriptionChangedEvent
import com.petsafety.app.data.model.TagScannedEvent
import com.petsafety.app.data.model.PetFoundEvent
import com.petsafety.app.data.notifications.NotificationHelper
import com.petsafety.app.data.network.SseService
import com.petsafety.app.data.sync.NetworkMonitor
import com.petsafety.app.data.sync.SyncService
import com.petsafety.app.util.StringProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import com.petsafety.app.R

@HiltViewModel
class AppStateViewModel @Inject constructor(
    private val sseService: SseService,
    private val notificationHelper: NotificationHelper,
    private val networkMonitor: NetworkMonitor,
    val syncService: SyncService,
    private val stringProvider: StringProvider,
    private val subscriptionEventBus: SubscriptionEventBus
) : ViewModel() {
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val isConnected: StateFlow<Boolean> = networkMonitor.isConnected

    init {
        setupSseHandlers()
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

    fun connectSse() {
        sseService.connect()
    }

    fun disconnectSse() {
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
            val name = event.refereeName ?: event.refereeEmail ?: "Someone"
            showSuccess(stringProvider.getString(R.string.sse_referral_used, name))
        }
    }
}
