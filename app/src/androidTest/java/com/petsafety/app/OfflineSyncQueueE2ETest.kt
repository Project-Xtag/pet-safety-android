package com.petsafety.app

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.petsafety.app.data.local.AppDatabase
import com.petsafety.app.data.local.OfflineDataManager
import com.petsafety.app.data.model.MissingPetAlert
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.model.User
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.*
import com.petsafety.app.data.sync.NetworkMonitor
import com.petsafety.app.data.sync.SyncService
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineSyncQueueE2ETest {
    @Test
    fun offlineAlertQueuedAndSynced() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val offlineManager = OfflineDataManager(db)
        val networkMonitor = NetworkMonitor(context)
        networkMonitor.overrideMode = NetworkMonitor.OverrideMode.OFFLINE
        val apiService = FakeApiService()
        val syncService = SyncService(apiService, offlineManager, networkMonitor)

        networkMonitor.refreshStatus()
        syncService.queueAction(
            SyncService.ActionType.CREATE_ALERT,
            mapOf("petId" to "pet_1", "lastSeenAddress" to "Test Street")
        )

        val pending = offlineManager.getPendingActions()
        assertEquals(1, pending.size)

        // Simulate online sync
        networkMonitor.overrideMode = NetworkMonitor.OverrideMode.ONLINE
        networkMonitor.refreshStatus()
        syncService.performFullSync()
        val remaining = offlineManager.getPendingActions()
        assertEquals(0, remaining.size)
    }
}

private class FakeApiService : ApiService {
    override suspend fun login(request: LoginRequest) = ApiEnvelope(true, LoginResponse("ok"))
    override suspend fun verifyOtp(request: VerifyOtpRequest) =
        ApiEnvelope(true, VerifyOtpResponse("token", User(id = "1", email = "a@b.com")))

    override suspend fun getCurrentUser() = ApiEnvelope(true, UserResponse(User(id = "1", email = "a@b.com")))
    override suspend fun updateUser(request: kotlinx.serialization.json.JsonObject) = getCurrentUser()
    override suspend fun getNotificationPreferences() =
        ApiEnvelope(true, NotificationPreferencesResponse(com.petsafety.app.data.model.NotificationPreferences.default))
    override suspend fun updateNotificationPreferences(request: com.petsafety.app.data.model.NotificationPreferences) =
        ApiEnvelope(true, NotificationPreferencesResponse(request))

    override suspend fun getPets() = ApiEnvelope(true, PetsResponse(emptyList()))
    override suspend fun getPet(id: String) = ApiEnvelope(true, PetResponse(fakePet(id)))
    override suspend fun createPet(request: CreatePetRequest) = ApiEnvelope(true, PetResponse(fakePet("pet_1")))
    override suspend fun updatePet(id: String, request: UpdatePetRequest) = ApiEnvelope(true, PetResponse(fakePet(id)))
    override suspend fun deletePet(id: String) = ApiEnvelope(true, EmptyResponse())
    override suspend fun markPetMissing(id: String, request: MarkMissingRequest) =
        ApiEnvelope(true, MarkMissingResponse(fakePet(id), AlertInfo("alert_1"), "ok"))

    override suspend fun uploadPetImage(id: String, image: okhttp3.MultipartBody.Part) =
        ApiEnvelope(true, ImageUploadResponse("url", ImageUploadResponse.PartialPet(null)))

    override suspend fun getPetPhotos(id: String) = ApiEnvelope(true, PetPhotosResponse(emptyList()))
    override suspend fun uploadPetPhoto(id: String, photo: okhttp3.MultipartBody.Part, isPrimary: okhttp3.RequestBody) =
        ApiEnvelope(true, PhotoUploadResponse(com.petsafety.app.data.model.PetPhoto("1", id, "url", false, 0, "")))
    override suspend fun setPrimaryPhoto(id: String, photoId: String) = ApiEnvelope(true, PhotoOperationResponse())
    override suspend fun deletePetPhoto(id: String, photoId: String) = ApiEnvelope(true, PhotoOperationResponse())
    override suspend fun reorderPetPhotos(id: String, request: PhotoReorderRequest) = ApiEnvelope(true, PhotoReorderResponse())

    override suspend fun getAlerts() = ApiEnvelope(true, AlertsResponse(emptyList()))
    override suspend fun getNearbyAlerts(latitude: Double, longitude: Double, radiusKm: Double) =
        ApiEnvelope(true, NearbyAlertsResponse(emptyList(), 0))
    override suspend fun createAlert(request: CreateAlertRequest) =
        ApiEnvelope(true, AlertResponse(MissingPetAlert("alert_1", request.petId, "user_1", "active", null, null, null, null, "", "")))
    override suspend fun updateAlertStatus(id: String) =
        ApiEnvelope(true, AlertResponse(MissingPetAlert(id, "pet_1", "user_1", "found", null, null, null, null, "", "")))
    override suspend fun reportSighting(id: String, request: ReportSightingRequest) =
        ApiEnvelope(true, SightingResponse(com.petsafety.app.data.model.Sighting("s1", id, createdAt = "")))

    override suspend fun scanQrCode(code: String) =
        ApiEnvelope(true, com.petsafety.app.data.model.ScanResponse(fakePet("pet_1")))
    override suspend fun activateTag(request: ActivateTagRequest) =
        ApiEnvelope(true, ActivateTagResponse(com.petsafety.app.data.model.QrTag("1", request.qrCode, request.petId, "active", "")))
    override suspend fun getActiveTag(petId: String) = ApiEnvelope(true, GetTagResponse(null))
    override suspend fun shareLocation(request: ShareLocationRequest) =
        ApiEnvelope(true, ShareLocationResponse("ok", "s1", true, true))

    override suspend fun createTagOrder(request: CreateTagOrderRequest) =
        ApiEnvelope(true, CreateTagOrderResponse(fakeOrder(), true, "1", "ok"))
    override suspend fun getOrders() = ApiEnvelope(true, OrdersResponse(emptyList()))
    override suspend fun createReplacementOrder(petId: String, request: CreateReplacementOrderRequest) =
        ApiEnvelope(true, ReplacementOrderResponse(fakeOrder(), "ok"))

    override suspend fun createPaymentIntent(request: CreatePaymentIntentRequest) =
        ApiEnvelope(true, PaymentIntentResponse(com.petsafety.app.data.model.PaymentIntent("pi_1", null, 3.9, "gbp")))
    override suspend fun getPaymentIntent(id: String) =
        ApiEnvelope(true, PaymentIntentStatusResponse(com.petsafety.app.data.model.PaymentIntent(id, null, 3.9, "gbp")))

    override suspend fun getPublicSuccessStories(latitude: Double, longitude: Double, radiusKm: Double, page: Int, limit: Int) =
        ApiEnvelope(true, SuccessStoriesResponse(emptyList(), 0, false, page, limit))
    override suspend fun getSuccessStoriesForPet(petId: String) = ApiEnvelope(true, emptyList())
    override suspend fun createSuccessStory(request: CreateSuccessStoryRequest) =
        ApiEnvelope(true, com.petsafety.app.data.model.SuccessStory("1", null, request.petId, "owner", null, null, null, null, true, true, null, "", "", "", null))
    override suspend fun updateSuccessStory(id: String, request: UpdateSuccessStoryRequest) =
        ApiEnvelope(true, com.petsafety.app.data.model.SuccessStory(id, null, "pet", "owner", null, null, null, null, true, true, null, "", "", "", null))
    override suspend fun deleteSuccessStory(id: String) = ApiEnvelope(true, EmptyResponse())
    override suspend fun uploadSuccessStoryPhoto(id: String, photo: okhttp3.MultipartBody.Part) =
        ApiEnvelope(true, com.petsafety.app.data.model.SuccessStoryPhoto("1", id, "url", 0))

    private fun fakePet(id: String) = Pet(
        id = id,
        ownerId = "owner",
        name = "Max",
        species = "Dog",
        isMissing = false,
        createdAt = "",
        updatedAt = ""
    )

    private fun fakeOrder() = com.petsafety.app.data.model.Order(
        id = "order_1",
        userId = null,
        petName = "Max",
        totalAmount = 3.9,
        shippingCost = 3.9,
        shippingAddress = null,
        billingAddress = null,
        paymentMethod = "free",
        paymentStatus = "pending",
        orderStatus = "pending",
        createdAt = "",
        updatedAt = ""
    )
}
