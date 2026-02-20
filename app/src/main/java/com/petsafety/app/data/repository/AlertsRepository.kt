package com.petsafety.app.data.repository

import com.petsafety.app.data.local.OfflineDataManager
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.MissingPetAlert
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.CreateAlertRequest
import com.petsafety.app.data.network.model.ReportSightingRequest
import com.petsafety.app.data.network.model.UpdateAlertRequest
import com.petsafety.app.data.sync.NetworkMonitor
import com.petsafety.app.data.sync.SyncService
import kotlinx.coroutines.flow.first

class AlertsRepository(
    private val apiService: ApiService,
    private val offlineManager: OfflineDataManager,
    private val networkMonitor: NetworkMonitor,
    private val syncService: SyncService
) {
    suspend fun fetchAlerts(): Pair<List<MissingPetAlert>, String?> {
        networkMonitor.refreshStatus()
        return if (networkMonitor.isConnected.first()) {
            val alerts = apiService.getAlerts().data?.alerts ?: emptyList()
            alerts.forEach { offlineManager.saveAlert(it) }
            alerts to null
        } else {
            offlineManager.fetchAlerts() to "Showing cached data (offline)"
        }
    }

    suspend fun fetchNearbyAlerts(
        latitude: Double,
        longitude: Double,
        radiusKm: Double
    ): List<MissingPetAlert> {
        return apiService.getNearbyAlerts(latitude, longitude, radiusKm).data?.alerts ?: emptyList()
    }

    suspend fun createAlert(
        petId: String,
        location: String?,
        coordinate: LocationCoordinate?,
        additionalInfo: String?
    ): Result<MissingPetAlert> {
        networkMonitor.refreshStatus()
        if (!networkMonitor.isConnected.first()) {
            val actionData = mutableMapOf<String, Any?>("petId" to petId)
            if (coordinate != null) {
                actionData["latitude"] = coordinate.lat
                actionData["longitude"] = coordinate.lng
            }
            if (!location.isNullOrBlank()) actionData["lastSeenAddress"] = location
            if (!additionalInfo.isNullOrBlank()) actionData["description"] = additionalInfo
            val localAlertId = "offline-${java.util.UUID.randomUUID()}"
            actionData["localAlertId"] = localAlertId
            syncService.queueAction(SyncService.ActionType.CREATE_ALERT, actionData)

            val localAlert = MissingPetAlert(
                id = localAlertId,
                petId = petId,
                userId = "unknown",
                status = "pending-sync",
                lastSeenLocation = location,
                lastSeenLatitude = coordinate?.lat,
                lastSeenLongitude = coordinate?.lng,
                additionalInfo = additionalInfo,
                createdAt = java.time.Instant.now().toString(),
                updatedAt = java.time.Instant.now().toString()
            )
            offlineManager.saveAlert(localAlert)
            return Result.failure(OfflineQueuedException())
        }

        val request = CreateAlertRequest(
            petId = petId,
            lastSeenLocation = coordinate,
            lastSeenAddress = location,
            description = additionalInfo
        )
        val alert = apiService.createAlert(request).data?.alert ?: error("Missing alert")
        offlineManager.saveAlert(alert)
        return Result.success(alert)
    }

    suspend fun updateAlert(
        id: String,
        description: String?,
        lastSeenAddress: String?,
        rewardAmount: String?
    ): MissingPetAlert {
        val alert = apiService.updateAlert(id, UpdateAlertRequest(
            description = description,
            lastSeenAddress = lastSeenAddress,
            rewardAmount = rewardAmount
        )).data?.alert ?: error("Missing alert")
        offlineManager.saveAlert(alert)
        return alert
    }

    suspend fun updateAlertStatus(id: String, status: String): MissingPetAlert {
        val alert = apiService.updateAlertStatus(id).data?.alert ?: error("Missing alert")
        offlineManager.saveAlert(alert)
        return alert
    }

    suspend fun reportSighting(
        alertId: String,
        reporterName: String?,
        reporterPhone: String?,
        reporterEmail: String?,
        location: String?,
        coordinate: LocationCoordinate?,
        notes: String?
    ): Result<Unit> {
        networkMonitor.refreshStatus()
        if (!networkMonitor.isConnected.first()) {
            val actionData = mutableMapOf<String, Any?>("alertId" to alertId)
            if (!reporterName.isNullOrBlank()) actionData["reporterName"] = reporterName
            if (!reporterPhone.isNullOrBlank()) actionData["reporterPhone"] = reporterPhone
            if (!reporterEmail.isNullOrBlank()) actionData["reporterEmail"] = reporterEmail
            if (!location.isNullOrBlank()) actionData["address"] = location
            if (coordinate != null) {
                actionData["latitude"] = coordinate.lat
                actionData["longitude"] = coordinate.lng
            }
            if (!notes.isNullOrBlank()) actionData["description"] = notes
            syncService.queueAction(SyncService.ActionType.REPORT_SIGHTING, actionData)
            return Result.failure(OfflineQueuedException())
        }

        val request = ReportSightingRequest(
            reporterName = reporterName,
            reporterPhone = reporterPhone,
            reporterEmail = reporterEmail,
            location = coordinate,
            address = location,
            description = notes
        )
        apiService.reportSighting(alertId, request)
        return Result.success(Unit)
    }
}
