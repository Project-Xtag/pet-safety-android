package com.petsafety.app.data.sync

import com.petsafety.app.data.local.OfflineDataManager
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.CreateAlertRequest
import com.petsafety.app.data.network.model.MarkMissingRequest
import com.petsafety.app.data.network.model.ReportSightingRequest
import com.petsafety.app.data.network.model.UpdatePetRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

class SyncService(
    private val apiService: ApiService,
    private val offlineDataManager: OfflineDataManager,
    private val networkMonitor: NetworkMonitor
) {
    enum class ActionType(val value: String) {
        MARK_PET_LOST("markPetLost"),
        MARK_PET_FOUND("markPetFound"),
        REPORT_SIGHTING("reportSighting"),
        CREATE_ALERT("createAlert"),
        UPDATE_PET("updatePet")
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    sealed class SyncStatus {
        data object Idle : SyncStatus()
        data object Syncing : SyncStatus()
        data object Completed : SyncStatus()
        data class Failed(val reason: String) : SyncStatus()
        data object NoConnection : SyncStatus()
    }

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    suspend fun performFullSync() {
        networkMonitor.refreshStatus()
        if (!networkMonitor.isConnected.first()) {
            _syncStatus.value = SyncStatus.NoConnection
            return
        }
        if (_isSyncing.value) return

        _isSyncing.value = true
        _syncStatus.value = SyncStatus.Syncing
        try {
            processQueuedActions()
            fetchRemoteData()
            _syncStatus.value = SyncStatus.Completed
        } catch (ex: Exception) {
            _syncStatus.value = SyncStatus.Failed(ex.localizedMessage.orEmpty())
        }
        _isSyncing.value = false
        updatePendingCount()
    }

    suspend fun queueAction(type: ActionType, data: Map<String, Any?>): String {
        val id = offlineDataManager.queueAction(type.value, data)
        updatePendingCount()
        networkMonitor.refreshStatus()
        if (networkMonitor.isConnected.first()) {
            performFullSync()
        }
        return id
    }

    private suspend fun processQueuedActions() {
        val actions = offlineDataManager.getPendingActions()
        if (actions.isEmpty()) return

        for (action in actions) {
            try {
                processAction(action.actionType, action.actionDataJson)
                offlineDataManager.completeAction(action.id)
            } catch (ex: Exception) {
                offlineDataManager.failAction(action.id, ex.localizedMessage ?: "Unknown error")
            }
        }
    }

    private suspend fun processAction(type: String, dataJson: String) {
        val data = json.parseToJsonElement(dataJson) as? JsonObject ?: JsonObject(emptyMap())

        when (type) {
            ActionType.MARK_PET_LOST.value -> processMarkPetLost(data)
            ActionType.MARK_PET_FOUND.value -> processMarkPetFound(data)
            ActionType.REPORT_SIGHTING.value -> processReportSighting(data)
            ActionType.CREATE_ALERT.value -> processCreateAlert(data)
            ActionType.UPDATE_PET.value -> processUpdatePet(data)
        }
    }

    private suspend fun processMarkPetLost(data: JsonObject) {
        val petId = data.string("petId") ?: return
        val latitude = data.double("latitude")
        val longitude = data.double("longitude")
        val request = MarkMissingRequest(
            lastSeenLocation = if (latitude != null && longitude != null) {
                LocationCoordinate(latitude, longitude)
            } else null,
            lastSeenAddress = data.string("lastSeenAddress"),
            description = data.string("description")
        )
        apiService.markPetMissing(petId, request)
    }

    private suspend fun processMarkPetFound(data: JsonObject) {
        val petId = data.string("petId") ?: return
        val request = UpdatePetRequest(isMissing = false)
        apiService.updatePet(petId, request)
    }

    private suspend fun processReportSighting(data: JsonObject) {
        val alertId = data.string("alertId") ?: return
        val latitude = data.double("latitude")
        val longitude = data.double("longitude")
        val request = ReportSightingRequest(
            reporterName = data.string("reporterName"),
            reporterPhone = data.string("reporterPhone"),
            reporterEmail = data.string("reporterEmail"),
            location = if (latitude != null && longitude != null) {
                LocationCoordinate(latitude, longitude)
            } else null,
            address = data.string("address"),
            description = data.string("description")
        )
        apiService.reportSighting(alertId, request)
    }

    private suspend fun processCreateAlert(data: JsonObject) {
        val petId = data.string("petId") ?: return
        val latitude = data.double("latitude")
        val longitude = data.double("longitude")
        val request = CreateAlertRequest(
            petId = petId,
            lastSeenLocation = if (latitude != null && longitude != null) {
                LocationCoordinate(latitude, longitude)
            } else null,
            lastSeenAddress = data.string("lastSeenAddress"),
            description = data.string("description")
        )
        val response = apiService.createAlert(request)
        val localAlertId = data.string("localAlertId")
        if (localAlertId != null) {
            offlineDataManager.deleteAlert(localAlertId)
        }
        response.data?.alert?.let { offlineDataManager.saveAlert(it) }
    }

    private suspend fun processUpdatePet(data: JsonObject) {
        val petId = data.string("petId") ?: return
        val request = UpdatePetRequest(
            name = data.string("name"),
            species = data.string("species"),
            breed = data.string("breed"),
            color = data.string("color"),
            age = data.string("age"),
            weight = data.double("weight"),
            microchipNumber = data.string("microchipNumber"),
            medicalNotes = data.string("medicalNotes"),
            allergies = data.string("allergies"),
            medications = data.string("medications"),
            notes = data.string("notes"),
            uniqueFeatures = data.string("uniqueFeatures"),
            sex = data.string("sex"),
            isNeutered = data.bool("isNeutered"),
            isMissing = data.bool("isMissing")
        )
        apiService.updatePet(petId, request)
    }

    private suspend fun fetchRemoteData() {
        val petsResponse = apiService.getPets()
        petsResponse.data?.pets?.let { offlineDataManager.savePets(it) }

        val alertsResponse = apiService.getAlerts()
        alertsResponse.data?.alerts?.forEach { offlineDataManager.saveAlert(it) }
    }

    suspend fun updatePendingCount() {
        _pendingCount.value = offlineDataManager.getPendingCount()
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.double(key: String): Double? =
        this[key]?.jsonPrimitive?.doubleOrNull

    private fun JsonObject.bool(key: String): Boolean? =
        this[key]?.jsonPrimitive?.booleanOrNull
}
