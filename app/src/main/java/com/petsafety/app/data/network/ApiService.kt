package com.petsafety.app.data.network

import com.petsafety.app.data.model.ScanResponse
import com.petsafety.app.data.model.SuccessStory
import com.petsafety.app.data.model.SuccessStoryPhoto
import com.petsafety.app.data.network.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    // Auth
    @POST("auth/send-otp")
    suspend fun login(@Body request: LoginRequest): ApiEnvelope<LoginResponse>

    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): ApiEnvelope<VerifyOtpResponse>

    // User
    @GET("users/me")
    suspend fun getCurrentUser(): ApiEnvelope<UserResponse>

    @PATCH("users/me")
    suspend fun updateUser(@Body request: kotlinx.serialization.json.JsonObject): ApiEnvelope<UserResponse>

    @GET("users/me/notification-preferences")
    suspend fun getNotificationPreferences(): ApiEnvelope<NotificationPreferencesResponse>

    @PUT("users/me/notification-preferences")
    suspend fun updateNotificationPreferences(
        @Body request: com.petsafety.app.data.model.NotificationPreferences
    ): ApiEnvelope<NotificationPreferencesResponse>

    @GET("users/me/can-delete")
    suspend fun canDeleteAccount(): ApiEnvelope<CanDeleteAccountResponse>

    @DELETE("users/me")
    suspend fun deleteAccount(): ApiEnvelope<EmptyResponse>

    // Pets
    @GET("pets")
    suspend fun getPets(): ApiEnvelope<PetsResponse>

    @GET("pets/{id}")
    suspend fun getPet(@Path("id") id: String): ApiEnvelope<PetResponse>

    @POST("pets")
    suspend fun createPet(@Body request: CreatePetRequest): ApiEnvelope<PetResponse>

    @PUT("pets/{id}")
    suspend fun updatePet(@Path("id") id: String, @Body request: UpdatePetRequest): ApiEnvelope<PetResponse>

    @DELETE("pets/{id}")
    suspend fun deletePet(@Path("id") id: String): ApiEnvelope<EmptyResponse>

    @POST("pets/{id}/mark-missing")
    suspend fun markPetMissing(@Path("id") id: String, @Body request: MarkMissingRequest): ApiEnvelope<MarkMissingResponse>

    // Upload profile image
    @Multipart
    @POST("pets/{id}/image")
    suspend fun uploadPetImage(
        @Path("id") id: String,
        @retrofit2.http.Part image: MultipartBody.Part
    ): ApiEnvelope<ImageUploadResponse>

    // Pet Photos
    @GET("pets/{id}/photos")
    suspend fun getPetPhotos(@Path("id") id: String): ApiEnvelope<PetPhotosResponse>

    @Multipart
    @POST("pets/{id}/photos")
    suspend fun uploadPetPhoto(
        @Path("id") id: String,
        @retrofit2.http.Part photo: MultipartBody.Part,
        @retrofit2.http.Part("isPrimary") isPrimary: RequestBody
    ): ApiEnvelope<PhotoUploadResponse>

    @PUT("pets/{id}/photos/{photoId}/primary")
    suspend fun setPrimaryPhoto(
        @Path("id") id: String,
        @Path("photoId") photoId: String
    ): ApiEnvelope<PhotoOperationResponse>

    @DELETE("pets/{id}/photos/{photoId}")
    suspend fun deletePetPhoto(
        @Path("id") id: String,
        @Path("photoId") photoId: String
    ): ApiEnvelope<PhotoOperationResponse>

    @PUT("pets/{id}/photos/reorder")
    suspend fun reorderPetPhotos(
        @Path("id") id: String,
        @Body request: PhotoReorderRequest
    ): ApiEnvelope<PhotoReorderResponse>

    // Alerts
    @GET("alerts")
    suspend fun getAlerts(): ApiEnvelope<AlertsResponse>

    @GET("alerts/nearby")
    suspend fun getNearbyAlerts(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radiusKm: Double
    ): ApiEnvelope<NearbyAlertsResponse>

    @POST("alerts/missing")
    suspend fun createAlert(@Body request: CreateAlertRequest): ApiEnvelope<AlertResponse>

    @POST("alerts/{id}/found")
    suspend fun updateAlertStatus(@Path("id") id: String): ApiEnvelope<AlertResponse>

    @POST("alerts/{id}/sightings")
    suspend fun reportSighting(
        @Path("id") id: String,
        @Body request: ReportSightingRequest
    ): ApiEnvelope<SightingResponse>

    // QR Tags
    @GET("qr-tags/scan/{code}")
    suspend fun scanQrCode(@Path("code") code: String): ApiEnvelope<ScanResponse>

    @POST("qr-tags/activate")
    suspend fun activateTag(@Body request: ActivateTagRequest): ApiEnvelope<ActivateTagResponse>

    @GET("qr-tags/pet/{petId}")
    suspend fun getActiveTag(@Path("petId") petId: String): ApiEnvelope<GetTagResponse>

    @POST("qr-tags/share-location")
    suspend fun shareLocation(@Body request: ShareLocationRequest): ApiEnvelope<ShareLocationResponse>

    // Orders
    @POST("orders")
    suspend fun createTagOrder(@Body request: CreateTagOrderRequest): ApiEnvelope<CreateTagOrderResponse>

    @GET("orders")
    suspend fun getOrders(): ApiEnvelope<OrdersResponse>

    @POST("orders/replacement/{petId}")
    suspend fun createReplacementOrder(
        @Path("petId") petId: String,
        @Body request: CreateReplacementOrderRequest
    ): ApiEnvelope<ReplacementOrderResponse>

    // Payments
    @POST("payments/intent")
    suspend fun createPaymentIntent(@Body request: CreatePaymentIntentRequest): ApiEnvelope<PaymentIntentResponse>

    @GET("payments/intent/{id}")
    suspend fun getPaymentIntent(@Path("id") id: String): ApiEnvelope<PaymentIntentStatusResponse>

    // Success stories
    @GET("success-stories")
    suspend fun getPublicSuccessStories(
        @Query("lat") latitude: Double,
        @Query("lng") longitude: Double,
        @Query("radius") radiusKm: Double,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): ApiEnvelope<SuccessStoriesResponse>

    @GET("success-stories/pet/{petId}")
    suspend fun getSuccessStoriesForPet(@Path("petId") petId: String): ApiEnvelope<List<SuccessStory>>

    @POST("success-stories")
    suspend fun createSuccessStory(@Body request: CreateSuccessStoryRequest): ApiEnvelope<SuccessStory>

    @PATCH("success-stories/{id}")
    suspend fun updateSuccessStory(
        @Path("id") id: String,
        @Body request: UpdateSuccessStoryRequest
    ): ApiEnvelope<SuccessStory>

    @DELETE("success-stories/{id}")
    suspend fun deleteSuccessStory(@Path("id") id: String): ApiEnvelope<EmptyResponse>

    @Multipart
    @POST("success-stories/{id}/photos")
    suspend fun uploadSuccessStoryPhoto(
        @Path("id") id: String,
        @retrofit2.http.Part photo: MultipartBody.Part
    ): ApiEnvelope<SuccessStoryPhoto>

    // Breeds
    @GET("breeds")
    suspend fun getBreeds(@Query("search") search: String? = null): ApiEnvelope<BreedsResponse>

    @GET("breeds/dog")
    suspend fun getDogBreeds(): ApiEnvelope<BreedsResponse>

    @GET("breeds/cat")
    suspend fun getCatBreeds(): ApiEnvelope<BreedsResponse>

    // Contact Support
    @POST("contact/support")
    suspend fun submitSupportRequest(@Body request: SupportRequest): ApiEnvelope<SupportRequestResponse>

    // FCM Token Management
    @POST("users/fcm-tokens")
    suspend fun registerFCMToken(@Body request: FCMTokenRequest): ApiEnvelope<FCMTokenResponse>

    @DELETE("users/fcm-tokens/{token}")
    suspend fun removeFCMToken(@Path("token") token: String): ApiEnvelope<EmptyResponse>

    // Subscriptions
    @GET("subscriptions/plans")
    suspend fun getSubscriptionPlans(): ApiEnvelope<SubscriptionPlansResponse>

    @GET("subscriptions/my-subscription")
    suspend fun getMySubscription(): ApiEnvelope<MySubscriptionResponse>

    @POST("subscriptions/checkout")
    suspend fun createSubscriptionCheckout(@Body request: CreateCheckoutRequest): ApiEnvelope<CheckoutResponse>

    @POST("subscriptions/upgrade-starter")
    suspend fun upgradeToStarter(): ApiEnvelope<UpgradeResponse>

    @POST("subscriptions/cancel")
    suspend fun cancelSubscription(): ApiEnvelope<CancelSubscriptionResponse>

    @GET("subscriptions/features")
    suspend fun getSubscriptionFeatures(): ApiEnvelope<SubscriptionFeaturesResponse>

    // Billing
    @POST("billing/portal-session")
    suspend fun createPortalSession(): ApiEnvelope<PortalSessionResponse>

    @GET("billing/invoices")
    suspend fun getInvoices(@Query("limit") limit: Int = 24): ApiEnvelope<InvoicesResponse>

    // Referrals
    @POST("referrals/generate-code")
    suspend fun generateReferralCode(): ApiEnvelope<ReferralCodeResponse>

    @POST("referrals/apply")
    suspend fun applyReferralCode(@Body request: ApplyReferralRequest): ApiEnvelope<EmptyResponse>

    @GET("referrals/status")
    suspend fun getReferralStatus(): ApiEnvelope<ReferralStatusResponse>
}
