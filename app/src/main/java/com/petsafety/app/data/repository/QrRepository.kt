package com.petsafety.app.data.repository

import com.petsafety.app.data.model.ClaimPromoTagRequest
import com.petsafety.app.data.model.ClaimPromoTagResponse
import com.petsafety.app.data.model.ScanResponse
import com.petsafety.app.data.model.QrTag
import com.petsafety.app.data.model.TagLookupResponse
import com.petsafety.app.data.network.model.CreatePetRequest
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.ActivateTagRequest
import com.petsafety.app.data.network.model.LocationPayload
import com.petsafety.app.data.network.model.ShareLocationRequest
import com.petsafety.app.data.network.model.ShareLocationResponse

class QrRepository(private val apiService: ApiService) {
    suspend fun lookupTag(code: String): TagLookupResponse =
        apiService.lookupTag(code).data ?: error("Missing lookup response")

    suspend fun scanQr(code: String): ScanResponse =
        apiService.scanQrCode(code).data ?: error("Missing scan response")

    suspend fun activateTag(qrCode: String, petId: String): QrTag =
        apiService.activateTag(ActivateTagRequest(qrCode, petId)).data?.tag ?: error("Missing tag")

    suspend fun getActiveTag(petId: String): QrTag? =
        apiService.getActiveTag(petId).data?.tag

    suspend fun claimPromoTag(
        qrCode: String,
        pet: CreatePetRequest? = null,
        petId: String? = null
    ): ClaimPromoTagResponse =
        apiService.claimPromoTag(ClaimPromoTagRequest(qrCode, pet, petId)).data
            ?: error("Missing claim promo response")

    /**
     * Share precise GPS location with the pet owner.
     *
     * 2026-05-02 missing-pet flow overhaul: precision toggle + 3-decimal
     * rounding gone; share is always precise. The backend rejects payloads
     * with the legacy is_approximate / consent_type / share_exact_location
     * fields. Use [shareManualAddress] when the finder denies GPS or
     * prefers to type the address.
     */
    suspend fun shareLocation(
        qrCode: String,
        latitude: Double,
        longitude: Double,
        accuracyMeters: Double? = null
    ): ShareLocationResponse {
        val request = ShareLocationRequest(
            qrCode = qrCode,
            location = LocationPayload(
                latitude = latitude,
                longitude = longitude,
                accuracyMeters = accuracyMeters ?: 0.0
            )
        )
        return apiService.shareLocation(request).data
            ?: error("Missing share location response")
    }

    /**
     * Share a manually-typed address as the GPS-denial / no-coverage
     * fallback. The backend geocodes via Google Places → Nominatim;
     * on geocoding failure the owner gets the typed text with a
     * "no map coordinates" note rather than nothing.
     */
    suspend fun shareManualAddress(
        qrCode: String,
        manualAddress: String
    ): ShareLocationResponse {
        val request = ShareLocationRequest(
            qrCode = qrCode,
            manualAddress = manualAddress
        )
        return apiService.shareLocation(request).data
            ?: error("Missing share location response")
    }
}
