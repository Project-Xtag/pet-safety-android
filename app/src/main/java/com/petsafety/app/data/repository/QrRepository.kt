package com.petsafety.app.data.repository

import com.petsafety.app.data.model.ScanResponse
import com.petsafety.app.data.model.QrTag
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.ActivateTagRequest
import com.petsafety.app.data.network.model.LocationData
import com.petsafety.app.data.network.model.ShareLocationRequest

class QrRepository(private val apiService: ApiService) {
    suspend fun scanQr(code: String): ScanResponse =
        apiService.scanQrCode(code).data ?: error("Missing scan response")

    suspend fun activateTag(qrCode: String, petId: String): QrTag =
        apiService.activateTag(ActivateTagRequest(qrCode, petId)).data?.tag ?: error("Missing tag")

    suspend fun getActiveTag(petId: String): QrTag? =
        apiService.getActiveTag(petId).data?.tag

    suspend fun shareLocation(
        qrCode: String,
        latitude: Double,
        longitude: Double,
        address: String?
    ) = apiService.shareLocation(
        ShareLocationRequest(
            qrCode = qrCode,
            location = LocationData(latitude, longitude),
            address = address
        )
    ).data ?: error("Missing share location response")
}
