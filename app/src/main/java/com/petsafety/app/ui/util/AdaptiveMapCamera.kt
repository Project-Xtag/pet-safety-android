package com.petsafety.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Camera-target resolution for the missing-pets and success-stories maps.
 *
 * Priority: pin (first alert / story coord) → user location → user country →
 * world view. We never hardcode a city default — that's how London ended up
 * stuck in front of users in Hungary.
 */

/** Resolved target for [rememberAdaptiveCamera] / map UI. */
data class CameraTarget(val latLng: LatLng, val zoom: Float)

/** Approximate geographic centers for the 12 supported product locales + US/GB fallback. */
object CountryCenters {
    private val centers: Map<String, LatLng> = mapOf(
        "hu" to LatLng(47.1625, 19.5033),
        "sk" to LatLng(48.6690, 19.6990),
        "cz" to LatLng(49.8175, 15.4730),
        "de" to LatLng(51.1657, 10.4515),
        "at" to LatLng(47.5162, 14.5501),
        "es" to LatLng(40.4637, -3.7492),
        "pt" to LatLng(39.3999, -8.2245),
        "ro" to LatLng(45.9432, 24.9668),
        "it" to LatLng(41.8719, 12.5674),
        "fr" to LatLng(46.6034, 1.8883),
        "pl" to LatLng(51.9194, 19.1451),
        "no" to LatLng(60.4720, 8.4689),
        "hr" to LatLng(45.1000, 15.2000),
        "gb" to LatLng(55.3781, -3.4360),
        "us" to LatLng(39.8283, -98.5795),
    )

    fun centerFor(countryCode: String?): LatLng? {
        val code = countryCode?.trim()?.lowercase().orEmpty()
        if (code.isBlank()) return null
        return centers[code]
    }
}

/**
 * Pure target resolution — extracted so it can be unit-tested without a Compose
 * runtime. Returns `null` only when we have nothing — caller falls back to a
 * neutral world view.
 */
fun resolveCameraTarget(
    pinLatLng: LatLng?,
    userLocation: LatLng?,
    userCountryCode: String?,
    pinZoom: Float = 11f,
    userLocationZoom: Float = 11f,
    countryZoom: Float = 6f,
): CameraTarget? {
    return when {
        pinLatLng != null -> CameraTarget(pinLatLng, pinZoom)
        userLocation != null -> CameraTarget(userLocation, userLocationZoom)
        else -> CountryCenters.centerFor(userCountryCode)?.let { CameraTarget(it, countryZoom) }
    }
}

/**
 * Returns a [CameraPositionState] that recenters smoothly when its inputs
 * change. The map starts at the best available target and animates whenever a
 * better one arrives (e.g. user location resolves async, alerts list loads).
 */
@Composable
fun rememberAdaptiveCamera(
    pinLatLng: LatLng?,
    userLocation: LatLng?,
    userCountryCode: String?,
    pinZoom: Float = 11f,
    userLocationZoom: Float = 11f,
    countryZoom: Float = 6f,
    worldZoom: Float = 2f,
): CameraPositionState {
    val initial = resolveCameraTarget(
        pinLatLng = pinLatLng,
        userLocation = userLocation,
        userCountryCode = userCountryCode,
        pinZoom = pinZoom,
        userLocationZoom = userLocationZoom,
        countryZoom = countryZoom,
    )

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            initial?.latLng ?: LatLng(0.0, 0.0),
            initial?.zoom ?: worldZoom,
        )
    }

    LaunchedEffect(pinLatLng, userLocation, userCountryCode) {
        val resolved = resolveCameraTarget(
            pinLatLng = pinLatLng,
            userLocation = userLocation,
            userCountryCode = userCountryCode,
            pinZoom = pinZoom,
            userLocationZoom = userLocationZoom,
            countryZoom = countryZoom,
        ) ?: return@LaunchedEffect
        cameraPositionState.animate(
            update = CameraUpdateFactory.newLatLngZoom(resolved.latLng, resolved.zoom),
            durationMs = 800,
        )
    }

    return cameraPositionState
}
