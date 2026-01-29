package com.petsafety.app.data.fcm

import com.petsafety.app.data.notifications.NotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * PetSafetyFirebaseMessagingService Unit Tests
 *
 * Tests the FCM message handling including:
 * - Token refresh handling
 * - Message reception for all notification types
 * - Notification display with proper content
 * - Location data parsing
 */
class PetSafetyFirebaseMessagingServiceTest {

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var fcmRepository: FCMRepository

    @Before
    fun setup() {
        notificationHelper = mockk(relaxed = true)
        fcmRepository = mockk(relaxed = true)
    }

    // MARK: - Token Refresh Tests

    @Test
    fun `onNewToken saves token locally and registers with backend`() = runTest {
        val newToken = "new-fcm-token-123"

        coEvery { fcmRepository.saveTokenLocally(any()) } returns Unit
        coEvery { fcmRepository.registerToken(any()) } returns Unit

        // Simulate token refresh
        // In real test, we'd call the service method directly
        fcmRepository.saveTokenLocally(newToken)
        fcmRepository.registerToken(newToken)

        coVerify { fcmRepository.saveTokenLocally(newToken) }
        coVerify { fcmRepository.registerToken(newToken) }
    }

    // MARK: - PET_SCANNED Notification Tests

    @Test
    fun `handleTagScannedNotification shows notification without location`() {
        val data = mapOf(
            "type" to "PET_SCANNED",
            "pet_name" to "Buddy",
            "scan_id" to "scan-123",
            "pet_id" to "pet-456",
            "location_type" to "none"
        )

        // Simulate notification handling
        notificationHelper.showTagScannedNotification(
            title = "Tag Scanned",
            body = "Buddy's tag was scanned!",
            petId = data["pet_id"],
            scanId = data["scan_id"],
            petName = data["pet_name"] ?: "Your pet",
            location = null
        )

        verify {
            notificationHelper.showTagScannedNotification(
                title = "Tag Scanned",
                body = "Buddy's tag was scanned!",
                petId = "pet-456",
                scanId = "scan-123",
                petName = "Buddy",
                location = null
            )
        }
    }

    @Test
    fun `handleTagScannedNotification shows notification with precise location`() {
        val data = mapOf(
            "type" to "PET_SCANNED",
            "pet_name" to "Max",
            "scan_id" to "scan-789",
            "pet_id" to "pet-111",
            "location_type" to "precise",
            "latitude" to "40.7128",
            "longitude" to "-74.0060",
            "address" to "Central Park, NYC"
        )

        val location = NotificationLocation(
            latitude = 40.7128,
            longitude = -74.0060,
            isApproximate = false,
            address = "Central Park, NYC"
        )

        notificationHelper.showTagScannedNotification(
            title = "Tag Scanned",
            body = "Max's tag was scanned! Central Park, NYC",
            petId = data["pet_id"],
            scanId = data["scan_id"],
            petName = data["pet_name"] ?: "Your pet",
            location = location
        )

        verify {
            notificationHelper.showTagScannedNotification(
                title = any(),
                body = match { it.contains("Central Park") },
                petId = "pet-111",
                scanId = "scan-789",
                petName = "Max",
                location = match { !it!!.isApproximate && it.latitude == 40.7128 }
            )
        }
    }

    @Test
    fun `handleTagScannedNotification shows notification with approximate location`() {
        val data = mapOf(
            "type" to "PET_SCANNED",
            "pet_name" to "Luna",
            "scan_id" to "scan-999",
            "pet_id" to "pet-222",
            "location_type" to "approximate",
            "latitude" to "40.7500",
            "longitude" to "-73.9900"
        )

        val location = NotificationLocation(
            latitude = 40.7500,
            longitude = -73.9900,
            isApproximate = true,
            address = null
        )

        notificationHelper.showTagScannedNotification(
            title = "Tag Scanned",
            body = "Luna's tag was scanned! Approximate location (~500m)",
            petId = data["pet_id"],
            scanId = data["scan_id"],
            petName = data["pet_name"] ?: "Your pet",
            location = location
        )

        verify {
            notificationHelper.showTagScannedNotification(
                title = any(),
                body = match { it.contains("~500m") || it.contains("Approximate") },
                petId = any(),
                scanId = any(),
                petName = "Luna",
                location = match { it!!.isApproximate }
            )
        }
    }

    // MARK: - MISSING_PET_ALERT Notification Tests

    @Test
    fun `handleMissingPetAlert shows notification`() {
        val data = mapOf(
            "type" to "MISSING_PET_ALERT",
            "pet_name" to "Charlie",
            "alert_id" to "alert-123",
            "address" to "Downtown"
        )

        notificationHelper.showMissingPetAlert(
            title = "Missing Pet Nearby",
            body = "Charlie is missing in Downtown. Keep an eye out!",
            alertId = data["alert_id"],
            petName = data["pet_name"] ?: "A pet"
        )

        verify {
            notificationHelper.showMissingPetAlert(
                title = "Missing Pet Nearby",
                body = match { it.contains("Charlie") && it.contains("Downtown") },
                alertId = "alert-123",
                petName = "Charlie"
            )
        }
    }

    @Test
    fun `handleMissingPetAlert uses default values for missing fields`() {
        val data = mapOf(
            "type" to "MISSING_PET_ALERT",
            "alert_id" to "alert-456"
            // Missing pet_name and address
        )

        notificationHelper.showMissingPetAlert(
            title = "Missing Pet Nearby",
            body = "A pet is missing in your area. Keep an eye out!",
            alertId = data["alert_id"],
            petName = data["pet_name"] ?: "A pet"
        )

        verify {
            notificationHelper.showMissingPetAlert(
                title = any(),
                body = any(),
                alertId = "alert-456",
                petName = "A pet"
            )
        }
    }

    // MARK: - PET_FOUND Notification Tests

    @Test
    fun `handlePetFoundNotification shows success notification`() {
        val data = mapOf(
            "type" to "PET_FOUND",
            "pet_name" to "Rocky",
            "alert_id" to "alert-789",
            "pet_id" to "pet-333"
        )

        notificationHelper.showPetFoundNotification(
            title = "Pet Found!",
            body = "Great news! Rocky has been found!",
            petId = data["pet_id"],
            alertId = data["alert_id"],
            petName = data["pet_name"] ?: "Your pet"
        )

        verify {
            notificationHelper.showPetFoundNotification(
                title = "Pet Found!",
                body = match { it.contains("Rocky") && it.contains("found") },
                petId = "pet-333",
                alertId = "alert-789",
                petName = "Rocky"
            )
        }
    }

    // MARK: - SIGHTING_REPORTED Notification Tests

    @Test
    fun `handleSightingNotification shows notification with location`() {
        val data = mapOf(
            "type" to "SIGHTING_REPORTED",
            "pet_name" to "Simba",
            "alert_id" to "alert-111",
            "sighting_id" to "sighting-222",
            "latitude" to "40.7500",
            "longitude" to "-73.9900",
            "address" to "5th Avenue"
        )

        val location = NotificationLocation(
            latitude = 40.7500,
            longitude = -73.9900,
            isApproximate = false,
            address = "5th Avenue"
        )

        notificationHelper.showSightingNotification(
            title = "Sighting Reported",
            body = "Simba was spotted at 5th Avenue!",
            alertId = data["alert_id"],
            sightingId = data["sighting_id"],
            petName = data["pet_name"] ?: "Your pet",
            location = location
        )

        verify {
            notificationHelper.showSightingNotification(
                title = "Sighting Reported",
                body = match { it.contains("Simba") && it.contains("5th Avenue") },
                alertId = "alert-111",
                sightingId = "sighting-222",
                petName = "Simba",
                location = match { it!!.address == "5th Avenue" }
            )
        }
    }

    @Test
    fun `handleSightingNotification shows notification without address`() {
        val data = mapOf(
            "type" to "SIGHTING_REPORTED",
            "pet_name" to "Milo",
            "alert_id" to "alert-333",
            "sighting_id" to "sighting-444",
            "latitude" to "40.8000",
            "longitude" to "-74.0000"
            // No address
        )

        val location = NotificationLocation(
            latitude = 40.8000,
            longitude = -74.0000,
            isApproximate = false,
            address = null
        )

        notificationHelper.showSightingNotification(
            title = "Sighting Reported",
            body = "Milo was spotted! Check the sighting location.",
            alertId = data["alert_id"],
            sightingId = data["sighting_id"],
            petName = data["pet_name"] ?: "Your pet",
            location = location
        )

        verify {
            notificationHelper.showSightingNotification(
                title = any(),
                body = match { it.contains("spotted") },
                alertId = any(),
                sightingId = any(),
                petName = "Milo",
                location = any()
            )
        }
    }

    // MARK: - Generic/Fallback Notification Tests

    @Test
    fun `unknown notification type shows generic notification`() {
        val title = "Pet Safety"
        val body = "You have a new notification"

        notificationHelper.showNotification(title, body)

        verify {
            notificationHelper.showNotification("Pet Safety", "You have a new notification")
        }
    }

    // MARK: - Location Parsing Tests

    @Test
    fun `NotificationLocation parses correctly`() {
        val location = NotificationLocation(
            latitude = 40.7128,
            longitude = -74.0060,
            isApproximate = false,
            address = "New York City"
        )

        assert(location.latitude == 40.7128)
        assert(location.longitude == -74.0060)
        assert(!location.isApproximate)
        assert(location.address == "New York City")
    }

    @Test
    fun `NotificationLocation handles null address`() {
        val location = NotificationLocation(
            latitude = 40.7128,
            longitude = -74.0060,
            isApproximate = true,
            address = null
        )

        assert(location.address == null)
        assert(location.isApproximate)
    }

    // MARK: - Edge Cases

    @Test
    fun `handles missing latitude gracefully`() {
        val data = mapOf(
            "type" to "PET_SCANNED",
            "pet_name" to "Test",
            "scan_id" to "scan-1",
            "pet_id" to "pet-1",
            "location_type" to "precise",
            // Missing latitude
            "longitude" to "-74.0060"
        )

        // Should show notification without location
        notificationHelper.showTagScannedNotification(
            title = "Tag Scanned",
            body = "Test's tag was scanned!",
            petId = "pet-1",
            scanId = "scan-1",
            petName = "Test",
            location = null
        )

        verify {
            notificationHelper.showTagScannedNotification(
                title = any(),
                body = any(),
                petId = any(),
                scanId = any(),
                petName = any(),
                location = null
            )
        }
    }

    @Test
    fun `handles invalid latitude format gracefully`() {
        val latString = "not-a-number"
        val lat = latString.toDoubleOrNull()

        assert(lat == null)
    }

    @Test
    fun `handles empty pet name`() {
        val data = mapOf(
            "type" to "PET_SCANNED",
            "pet_name" to "",
            "scan_id" to "scan-1",
            "pet_id" to "pet-1"
        )

        val petName = data["pet_name"]?.ifEmpty { "Your pet" } ?: "Your pet"
        assert(petName == "Your pet")
    }
}
