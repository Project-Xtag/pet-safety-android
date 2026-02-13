package com.petsafety.app.data.network

import com.petsafety.app.BuildConfig
import com.petsafety.app.data.local.AuthTokenStore
import com.petsafety.app.data.model.AlertCreatedEvent
import com.petsafety.app.data.model.AlertUpdatedEvent
import com.petsafety.app.data.model.ConnectionEvent
import com.petsafety.app.data.model.PetFoundEvent
import com.petsafety.app.data.model.SightingReportedEvent
import com.petsafety.app.data.model.ReferralUsedEvent
import com.petsafety.app.data.model.SubscriptionChangedEvent
import com.petsafety.app.data.model.TagScannedEvent
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

class SseService(private val tokenStore: AuthTokenStore) {
    private val baseUrl = BuildConfig.SSE_BASE_URL
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient()

    var onTagScanned: ((TagScannedEvent) -> Unit)? = null
    var onSightingReported: ((SightingReportedEvent) -> Unit)? = null
    var onPetFound: ((PetFoundEvent) -> Unit)? = null
    var onAlertCreated: ((AlertCreatedEvent) -> Unit)? = null
    var onAlertUpdated: ((AlertUpdatedEvent) -> Unit)? = null
    var onSubscriptionChanged: ((SubscriptionChangedEvent) -> Unit)? = null
    var onReferralUsed: ((ReferralUsedEvent) -> Unit)? = null
    var onConnected: ((ConnectionEvent) -> Unit)? = null

    private var eventSource: EventSource? = null

    fun connect() {
        val token = tokenStore.authToken.value
        if (token.isNullOrBlank()) return

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .build()

        eventSource = EventSources.createFactory(client)
            .newEventSource(request, object : EventSourceListener() {
                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    when (type) {
                        "connected" -> onConnected?.invoke(json.decodeFromString(data))
                        "tag_scanned" -> onTagScanned?.invoke(json.decodeFromString(data))
                        "sighting_reported" -> onSightingReported?.invoke(json.decodeFromString(data))
                        "pet_found" -> onPetFound?.invoke(json.decodeFromString(data))
                        "alert_created" -> onAlertCreated?.invoke(json.decodeFromString(data))
                        "alert_updated" -> onAlertUpdated?.invoke(json.decodeFromString(data))
                        "subscription_changed" -> onSubscriptionChanged?.invoke(json.decodeFromString(data))
                        "referral_used" -> onReferralUsed?.invoke(json.decodeFromString(data))
                    }
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    // Best effort reconnection will be handled by caller if needed.
                }
            })
    }

    fun disconnect() {
        eventSource?.cancel()
        eventSource = null
    }
}
