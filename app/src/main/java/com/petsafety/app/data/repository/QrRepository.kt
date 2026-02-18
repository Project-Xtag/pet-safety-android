package com.petsafety.app.data.repository

import com.petsafety.app.data.model.ScanResponse
import com.petsafety.app.data.model.QrTag
import com.petsafety.app.data.model.TagLookupResponse
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.ActivateTagRequest
import com.petsafety.app.data.network.model.LocationConsentType
import com.petsafety.app.data.network.model.ShareLocationRequest
import com.petsafety.app.data.network.model.ShareLocationResponse
import kotlin.math.roundToInt

/**
 * Location consent level for sharing location when scanning a tag.
 * 2-tier model: precise (toggle ON) or approximate (toggle OFF).
 */
enum class LocationConsent {
    /** Share approximate location (~500m accuracy) — toggle OFF */
    APPROXIMATE,
    /** Share precise/exact location — toggle ON (default) */
    PRECISE
}

class QrRepository(private val apiService: ApiService) {
    suspend fun lookupTag(code: String): TagLookupResponse =
        apiService.lookupTag(code).data ?: error("Missing lookup response")

    suspend fun scanQr(code: String): ScanResponse =
        apiService.scanQrCode(code).data ?: error("Missing scan response")

    suspend fun activateTag(qrCode: String, petId: String): QrTag =
        apiService.activateTag(ActivateTagRequest(qrCode, petId)).data?.tag ?: error("Missing tag")

    suspend fun getActiveTag(petId: String): QrTag? =
        apiService.getActiveTag(petId).data?.tag

    /**
     * Share location with 2-tier consent (toggle ON = precise, toggle OFF = approximate).
     *
     * @param qrCode The scanned QR code
     * @param consent The location consent level (PRECISE or APPROXIMATE)
     * @param latitude Current latitude (required)
     * @param longitude Current longitude (required)
     * @param accuracyMeters GPS accuracy in meters (optional)
     */
    suspend fun shareLocation(
        qrCode: String,
        consent: LocationConsent,
        latitude: Double? = null,
        longitude: Double? = null,
        accuracyMeters: Double? = null
    ): ShareLocationResponse {
        val shareExact = consent == LocationConsent.PRECISE

        val request = if (latitude != null && longitude != null) {
            when (consent) {
                LocationConsent.APPROXIMATE -> {
                    // Round to 3 decimal places (~111m precision)
                    val roundedLat = (latitude * 1000).roundToInt() / 1000.0
                    val roundedLng = (longitude * 1000).roundToInt() / 1000.0

                    ShareLocationRequest(
                        qrCode = qrCode,
                        latitude = roundedLat,
                        longitude = roundedLng,
                        accuracyMeters = accuracyMeters,
                        isApproximate = true,
                        consentType = LocationConsentType.APPROXIMATE,
                        shareExactLocation = false
                    )
                }
                LocationConsent.PRECISE -> {
                    ShareLocationRequest(
                        qrCode = qrCode,
                        latitude = latitude,
                        longitude = longitude,
                        accuracyMeters = accuracyMeters,
                        isApproximate = false,
                        consentType = LocationConsentType.PRECISE,
                        shareExactLocation = true
                    )
                }
            }
        } else {
            // No coordinates available — send consent preference without location data;
            // backend will still record the scan event
            ShareLocationRequest(
                qrCode = qrCode,
                shareExactLocation = shareExact
            )
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
