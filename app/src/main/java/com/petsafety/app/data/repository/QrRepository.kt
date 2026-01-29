package com.petsafety.app.data.repository

import com.petsafety.app.data.model.ScanResponse
import com.petsafety.app.data.model.QrTag
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.ActivateTagRequest
import com.petsafety.app.data.network.model.LocationConsentType
import com.petsafety.app.data.network.model.ShareLocationRequest
import com.petsafety.app.data.network.model.ShareLocationResponse
import kotlin.math.roundToInt

/**
 * Location consent level for sharing location when scanning a tag
 */
enum class LocationConsent {
    /** Don't share any location */
    DECLINE,
    /** Share approximate location (~500m accuracy) */
    APPROXIMATE,
    /** Share precise location */
    PRECISE
}

class QrRepository(private val apiService: ApiService) {
    suspend fun scanQr(code: String): ScanResponse =
        apiService.scanQrCode(code).data ?: error("Missing scan response")

    suspend fun activateTag(qrCode: String, petId: String): QrTag =
        apiService.activateTag(ActivateTagRequest(qrCode, petId)).data?.tag ?: error("Missing tag")

    suspend fun getActiveTag(petId: String): QrTag? =
        apiService.getActiveTag(petId).data?.tag

    /**
     * Share location with 3-tier GDPR consent
     *
     * @param qrCode The scanned QR code
     * @param consent The location consent level
     * @param latitude Current latitude (required if consent is not DECLINE)
     * @param longitude Current longitude (required if consent is not DECLINE)
     * @param accuracyMeters GPS accuracy in meters (optional)
     */
    suspend fun shareLocation(
        qrCode: String,
        consent: LocationConsent,
        latitude: Double? = null,
        longitude: Double? = null,
        accuracyMeters: Double? = null
    ): ShareLocationResponse {
        val request = when (consent) {
            LocationConsent.DECLINE -> {
                // No location shared
                ShareLocationRequest(qrCode = qrCode)
            }
            LocationConsent.APPROXIMATE -> {
                require(latitude != null && longitude != null) {
                    "Location required for approximate consent"
                }
                // Round to 3 decimal places (~111m precision)
                val roundedLat = (latitude * 1000).roundToInt() / 1000.0
                val roundedLng = (longitude * 1000).roundToInt() / 1000.0

                ShareLocationRequest(
                    qrCode = qrCode,
                    latitude = roundedLat,
                    longitude = roundedLng,
                    accuracyMeters = accuracyMeters,
                    isApproximate = true,
                    consentType = LocationConsentType.APPROXIMATE
                )
            }
            LocationConsent.PRECISE -> {
                require(latitude != null && longitude != null) {
                    "Location required for precise consent"
                }
                ShareLocationRequest(
                    qrCode = qrCode,
                    latitude = latitude,
                    longitude = longitude,
                    accuracyMeters = accuracyMeters,
                    isApproximate = false,
                    consentType = LocationConsentType.PRECISE
                )
            }
        }

        return apiService.shareLocation(request).data
            ?: error("Missing share location response")
    }

    /**
     * Legacy method for backward compatibility
     * @deprecated Use shareLocation with LocationConsent instead
     */
    @Deprecated("Use shareLocation with LocationConsent", ReplaceWith("shareLocation(qrCode, LocationConsent.PRECISE, latitude, longitude)"))
    suspend fun shareLocation(
        qrCode: String,
        latitude: Double,
        longitude: Double,
        address: String?
    ): ShareLocationResponse = shareLocation(
        qrCode = qrCode,
        consent = LocationConsent.PRECISE,
        latitude = latitude,
        longitude = longitude
    )
}
