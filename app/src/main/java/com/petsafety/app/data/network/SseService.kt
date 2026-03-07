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
import kotlinx.serialization.json.Json
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SseService(private val tokenStore: AuthTokenStore) {
    private val baseUrl = BuildConfig.SSE_BASE_URL
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 5
        private const val BASE_RECONNECT_DELAY_MS = 1000L // 1 second
    }

    /**
     * SSE client with certificate pinning matching the main API client.
     * Uses longer read timeout (0 = infinite) since SSE connections are long-lived,
     * but has a finite connect timeout to fail fast on network issues.
     */
    private val client: OkHttpClient = OkHttpClient.Builder().apply {
        connectTimeout(30, TimeUnit.SECONDS)
        writeTimeout(30, TimeUnit.SECONDS)
        readTimeout(0, TimeUnit.SECONDS) // SSE connections are long-lived
        if (!BuildConfig.DEBUG) {
            certificatePinner(
                CertificatePinner.Builder()
                    .add("api.senra.pet", "sha256/DxH4tt40L+eduF6szpY6TONlxhZhBd+pJ9wbHlQ2fuw=")
                    .add("api.senra.pet", "sha256/++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=")
                    .build()
            )
        }
    }.build()

    var onTagScanned: ((TagScannedEvent) -> Unit)? = null
    var onSightingReported: ((SightingReportedEvent) -> Unit)? = null
    var onPetFound: ((PetFoundEvent) -> Unit)? = null
    var onAlertCreated: ((AlertCreatedEvent) -> Unit)? = null
    var onAlertUpdated: ((AlertUpdatedEvent) -> Unit)? = null
    var onSubscriptionChanged: ((SubscriptionChangedEvent) -> Unit)? = null
    var onReferralUsed: ((ReferralUsedEvent) -> Unit)? = null
    var onConnected: ((ConnectionEvent) -> Unit)? = null

    /** Called when the SSE connection is lost after exhausting reconnection attempts. */
    var onConnectionLost: (() -> Unit)? = null

    private var eventSource: EventSource? = null
    private var shouldReconnect = false
    private var reconnectAttempts = 0
    private var reconnectHandler: android.os.Handler? = null

    fun connect() {
        val token = tokenStore.authToken.value
        if (token.isNullOrBlank()) {
            Timber.w("SSE: No auth token available, skipping connection")
            return
        }

        // Cancel any pending reconnect
        reconnectHandler?.removeCallbacksAndMessages(null)

        // Disconnect existing connection
        eventSource?.cancel()

        shouldReconnect = true

        val request = Request.Builder()
            .url(baseUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .build()

        Timber.d("SSE: Connecting...")

        eventSource = EventSources.createFactory(client)
            .newEventSource(request, object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: Response) {
                    Timber.d("SSE: Connection opened")
                    reconnectAttempts = 0 // Reset on successful connection
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    try {
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
                    } catch (e: Exception) {
                        Timber.w("SSE: Failed to decode event type=$type: ${e.message}")
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    Timber.d("SSE: Connection closed by server")
                    scheduleReconnect()
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: Response?
                ) {
                    Timber.w("SSE: Connection failed (code=${response?.code}): ${t?.message}")

                    if (response?.code == 401) {
                        Timber.w("SSE: Unauthorized — stopping reconnection (token may be expired)")
                        shouldReconnect = false
                        onConnectionLost?.invoke()
                        return
                    }

                    scheduleReconnect()
                }
            })
    }

    private fun scheduleReconnect() {
        if (!shouldReconnect) return

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Timber.w("SSE: Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached")
            onConnectionLost?.invoke()
            return
        }

        reconnectAttempts++
        // Exponential backoff: 1s, 2s, 4s, 8s, 16s
        val delayMs = BASE_RECONNECT_DELAY_MS * (1L shl (reconnectAttempts - 1))
        Timber.d("SSE: Scheduling reconnect attempt $reconnectAttempts in ${delayMs}ms")

        if (reconnectHandler == null) {
            reconnectHandler = android.os.Handler(android.os.Looper.getMainLooper())
        }
        reconnectHandler?.postDelayed({ connect() }, delayMs)
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectHandler?.removeCallbacksAndMessages(null)
        eventSource?.cancel()
        eventSource = null
        reconnectAttempts = 0
    }
}
