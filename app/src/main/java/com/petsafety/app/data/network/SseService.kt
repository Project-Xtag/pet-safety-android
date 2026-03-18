package com.petsafety.app.data.network

import android.os.Handler
import android.os.Looper
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONObject
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
    private var reconnectHandler: Handler? = null
    private var isRefreshingToken = false

    // Watchdog: if no data arrives within 3× server keep-alive (30s), force reconnect
    private val watchdogHandler = Handler(Looper.getMainLooper())
    private var watchdogRunnable: Runnable? = null
    private val WATCHDOG_INTERVAL_MS = 90_000L

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
                    resetWatchdog()
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    resetWatchdog()
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
                        Timber.w("SSE: Unauthorized — attempting token refresh")
                        if (!isRefreshingToken) {
                            isRefreshingToken = true
                            attemptTokenRefreshAndReconnect()
                        }
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
            reconnectHandler = Handler(Looper.getMainLooper())
        }
        reconnectHandler?.postDelayed({ connect() }, delayMs)
    }

    fun disconnect() {
        shouldReconnect = false
        reconnectHandler?.removeCallbacksAndMessages(null)
        cancelWatchdog()
        eventSource?.cancel()
        eventSource = null
        reconnectAttempts = 0
    }

    private fun resetWatchdog() {
        watchdogRunnable?.let { watchdogHandler.removeCallbacks(it) }
        watchdogRunnable = Runnable {
            Timber.w("SSE: Watchdog timeout — no data in ${WATCHDOG_INTERVAL_MS}ms, reconnecting")
            eventSource?.cancel()
            eventSource = null
            scheduleReconnect()
        }
        watchdogHandler.postDelayed(watchdogRunnable!!, WATCHDOG_INTERVAL_MS)
    }

    private fun cancelWatchdog() {
        watchdogRunnable?.let { watchdogHandler.removeCallbacks(it) }
        watchdogRunnable = null
    }

    private fun attemptTokenRefreshAndReconnect() {
        val refreshToken = tokenStore.refreshToken.value
        if (refreshToken.isNullOrEmpty()) {
            Timber.w("SSE: No refresh token available, giving up")
            isRefreshingToken = false
            shouldReconnect = false
            onConnectionLost?.invoke()
            return
        }

        Thread {
            try {
                val refreshClient = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS)
                    .build()

                val body = """{"refreshToken":"$refreshToken"}"""
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("${BuildConfig.API_BASE_URL}auth/refresh")
                    .post(body)
                    .build()

                val response = refreshClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    response.close()
                    val json = JSONObject(responseBody ?: "")
                    val data = json.optJSONObject("data")
                    val newToken = data?.optString("token")
                    val newRefreshToken = data?.optString("refreshToken")

                    if (!newToken.isNullOrEmpty() && !newRefreshToken.isNullOrEmpty()) {
                        tokenStore.saveTokensSync(newToken, newRefreshToken)
                        Timber.i("SSE: Token refresh successful, reconnecting")
                        reconnectAttempts = 0
                        isRefreshingToken = false
                        watchdogHandler.post { connect() }
                        return@Thread
                    }
                } else {
                    response.close()
                }

                Timber.w("SSE: Token refresh failed")
                isRefreshingToken = false
                shouldReconnect = false
                onConnectionLost?.invoke()
            } catch (e: Exception) {
                Timber.e(e, "SSE: Token refresh error")
                isRefreshingToken = false
                shouldReconnect = false
                onConnectionLost?.invoke()
            }
        }.start()
    }
}
