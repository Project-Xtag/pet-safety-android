package com.petsafety.app.data.repository

import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.model.QrTag
import com.petsafety.app.data.model.ScanResponse
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.ActivateTagRequest
import com.petsafety.app.data.network.model.ActivateTagResponse
import com.petsafety.app.data.network.model.ApiEnvelope
import com.petsafety.app.data.network.model.ShareLocationRequest
import com.petsafety.app.data.network.model.ShareLocationResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QrRepositoryTest {

    private lateinit var apiService: ApiService
    private lateinit var repository: QrRepository

    private val testPet = Pet(
        id = "pet-1",
        ownerId = "user-1",
        name = "Buddy",
        species = "Dog",
        breed = "Golden Retriever",
        isMissing = false,
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z"
    )

    private val testScanResponse = ScanResponse(pet = testPet)

    private val testTag = QrTag(
        id = "tag-1",
        qrCode = "ABC123",
        petId = "pet-1",
        status = "active",
        createdAt = "2024-01-01T00:00:00Z"
    )

    private val testShareLocationResponse = ShareLocationResponse(
        message = "Location shared successfully",
        scanId = "scan-1"
    )

    @Before
    fun setup() {
        apiService = mockk(relaxed = true)
        repository = QrRepository(apiService)
    }

    // ==================== shareLocation - precise GPS path ====================
    //
    // 2026-05-02 missing-pet flow overhaul: precision toggle gone, share
    // is always precise. The legacy is_approximate / consent_type /
    // share_exact_location fields are no longer emitted.

    @Test
    fun `shareLocation - sends precise coordinates wrapped in location object`() = runTest {
        val requestSlot = slot<ShareLocationRequest>()
        coEvery { apiService.shareLocation(capture(requestSlot)) } returns ApiEnvelope(
            success = true,
            data = testShareLocationResponse
        )

        repository.shareLocation("ABC123", 51.507422, -0.127800, 10.0)

        val captured = requestSlot.captured
        assertEquals("ABC123", captured.qrCode)
        assertNotNull(captured.location)
        assertEquals(51.507422, captured.location!!.latitude, 0.0)
        assertEquals(-0.127800, captured.location!!.longitude, 0.0)
        assertEquals(10.0, captured.location!!.accuracyMeters, 0.0)
        assertNull(captured.manualAddress)
    }

    @Test
    fun `shareLocation - defaults accuracy to 0 when not provided`() = runTest {
        val requestSlot = slot<ShareLocationRequest>()
        coEvery { apiService.shareLocation(capture(requestSlot)) } returns ApiEnvelope(
            success = true,
            data = testShareLocationResponse
        )

        repository.shareLocation("ABC123", 51.507422, -0.127800, null)

        assertEquals(0.0, requestSlot.captured.location!!.accuracyMeters, 0.0)
    }

    // ==================== shareLocation - manual-address path ====================

    @Test
    fun `shareManualAddress - sends free-text without location`() = runTest {
        val requestSlot = slot<ShareLocationRequest>()
        coEvery { apiService.shareLocation(capture(requestSlot)) } returns ApiEnvelope(
            success = true,
            data = testShareLocationResponse
        )

        repository.shareManualAddress("ABC123", "123 Pine St, Budapest")

        val captured = requestSlot.captured
        assertEquals("ABC123", captured.qrCode)
        assertNull(captured.location)
        assertEquals("123 Pine St, Budapest", captured.manualAddress)
    }

    // ==================== scanQrCode tests ====================

    @Test
    fun `scanQrCode - success - returns pet data`() = runTest {
        coEvery { apiService.scanQrCode("ABC123") } returns ApiEnvelope(
            success = true,
            data = testScanResponse
        )

        val result = repository.scanQr("ABC123")

        assertEquals(testPet, result.pet)
        assertEquals("pet-1", result.pet?.id)
        assertEquals("Buddy", result.pet?.name)
    }

    @Test
    fun `scanQrCode - failure - throws exception`() = runTest {
        coEvery { apiService.scanQrCode("INVALID") } throws RuntimeException("QR code not found")

        var exception: Exception? = null
        try {
            repository.scanQr("INVALID")
        } catch (e: Exception) {
            exception = e
        }

        assertNotNull(exception)
        assertEquals("QR code not found", exception?.message)
    }

    // ==================== activateTag tests ====================

    @Test
    fun `activateTag - success - returns tag`() = runTest {
        coEvery { apiService.activateTag(any()) } returns ApiEnvelope(
            success = true,
            data = ActivateTagResponse(tag = testTag, message = "Tag activated")
        )

        val result = repository.activateTag("ABC123", "pet-1")

        assertEquals("tag-1", result.id)
        assertEquals("ABC123", result.qrCode)
        assertEquals("pet-1", result.petId)
        assertEquals("active", result.status)
        coVerify {
            apiService.activateTag(ActivateTagRequest(qrCode = "ABC123", petId = "pet-1"))
        }
    }

    @Test
    fun `activateTag - failure - throws exception`() = runTest {
        coEvery { apiService.activateTag(any()) } throws RuntimeException("Tag already activated")

        var exception: Exception? = null
        try {
            repository.activateTag("ABC123", "pet-1")
        } catch (e: Exception) {
            exception = e
        }

        assertNotNull(exception)
        assertEquals("Tag already activated", exception?.message)
    }
}
