package com.petsafety.app.ui.viewmodel

import app.cash.turbine.test
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.model.ScanResponse
import com.petsafety.app.data.repository.LocationConsent
import com.petsafety.app.data.repository.QrRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QrScannerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: QrRepository
    private lateinit var viewModel: QrScannerViewModel

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

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = QrScannerViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== scanQr tests ====================

    @Test
    fun `scanQr - success - updates scanResult`() = runTest {
        coEvery { repository.scanQr("ABC123") } returns testScanResponse

        viewModel.scanQr("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testScanResponse, viewModel.scanResult.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `scanQr - failure - sets error message`() = runTest {
        val errorMsg = "QR code not found"
        coEvery { repository.scanQr("INVALID") } throws RuntimeException(errorMsg)

        viewModel.scanQr("INVALID")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.scanResult.value)
        assertEquals(errorMsg, viewModel.errorMessage.value)
    }

    @Test
    fun `scanQr - shows loading state`() = runTest {
        coEvery { repository.scanQr(any()) } returns testScanResponse

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.scanQr("ABC123")
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `scanQr - prevents duplicate scans while loading`() = runTest {
        coEvery { repository.scanQr(any()) } returns testScanResponse

        // First scan
        viewModel.scanQr("ABC123")

        // Try to scan again while loading
        viewModel.scanQr("DEF456")
        testDispatcher.scheduler.advanceUntilIdle()

        // Only the first scan should have been executed
        coVerify(exactly = 1) { repository.scanQr(any()) }
    }

    @Test
    fun `scanQr - prevents duplicate scans when result exists`() = runTest {
        coEvery { repository.scanQr("ABC123") } returns testScanResponse

        // First scan
        viewModel.scanQr("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Try to scan again when result already exists
        viewModel.scanQr("DEF456")
        testDispatcher.scheduler.advanceUntilIdle()

        // Only the first scan should have been executed
        coVerify(exactly = 1) { repository.scanQr(any()) }
    }

    // ==================== shareLocation tests (with consent) ====================

    @Test
    fun `shareLocation - PRECISE consent - success`() = runTest {
        coEvery {
            repository.shareLocation("ABC123", LocationConsent.PRECISE, 51.5074, -0.1278, 10.0)
        } returns mockk()

        var success = false
        var error: String? = null

        viewModel.shareLocation(
            qrCode = "ABC123",
            consent = LocationConsent.PRECISE,
            latitude = 51.5074,
            longitude = -0.1278,
            accuracyMeters = 10.0
        ) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
        coVerify {
            repository.shareLocation("ABC123", LocationConsent.PRECISE, 51.5074, -0.1278, 10.0)
        }
    }

    @Test
    fun `shareLocation - APPROXIMATE consent - success`() = runTest {
        coEvery {
            repository.shareLocation("ABC123", LocationConsent.APPROXIMATE, 51.5, -0.12, null)
        } returns mockk()

        var success = false

        viewModel.shareLocation(
            qrCode = "ABC123",
            consent = LocationConsent.APPROXIMATE,
            latitude = 51.5,
            longitude = -0.12
        ) { s, _ ->
            success = s
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        coVerify {
            repository.shareLocation("ABC123", LocationConsent.APPROXIMATE, 51.5, -0.12, null)
        }
    }

    @Test
    fun `shareLocation - DECLINE consent - success without location`() = runTest {
        coEvery {
            repository.shareLocation("ABC123", LocationConsent.DECLINE, null, null, null)
        } returns mockk()

        var success = false

        viewModel.shareLocation(
            qrCode = "ABC123",
            consent = LocationConsent.DECLINE
        ) { s, _ ->
            success = s
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        coVerify {
            repository.shareLocation("ABC123", LocationConsent.DECLINE, null, null, null)
        }
    }

    @Test
    fun `shareLocation - failure - returns error`() = runTest {
        val errorMsg = "Location sharing failed"
        coEvery {
            repository.shareLocation(any(), any(), any(), any(), any())
        } throws RuntimeException(errorMsg)

        var success = false
        var error: String? = null

        viewModel.shareLocation(
            qrCode = "ABC123",
            consent = LocationConsent.PRECISE,
            latitude = 51.5074,
            longitude = -0.1278
        ) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
    }

    // ==================== reset tests ====================

    @Test
    fun `reset - clears scanResult and errorMessage`() = runTest {
        // Setup state with scan result
        coEvery { repository.scanQr("ABC123") } returns testScanResponse
        viewModel.scanQr("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify state has values
        assertEquals(testScanResponse, viewModel.scanResult.value)

        // Reset
        viewModel.reset()

        // Verify state is cleared
        assertNull(viewModel.scanResult.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `reset - clears error state`() = runTest {
        // Setup state with error
        coEvery { repository.scanQr("INVALID") } throws RuntimeException("Error")
        viewModel.scanQr("INVALID")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify error is set
        assertEquals("Error", viewModel.errorMessage.value)

        // Reset
        viewModel.reset()

        // Verify error is cleared
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `reset - allows new scan after reset`() = runTest {
        coEvery { repository.scanQr("ABC123") } returns testScanResponse

        // First scan
        viewModel.scanQr("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(testScanResponse, viewModel.scanResult.value)

        // Reset
        viewModel.reset()

        // New scan should work
        val newPet = testPet.copy(id = "pet-2", name = "Whiskers")
        val newResponse = ScanResponse(pet = newPet)
        coEvery { repository.scanQr("DEF456") } returns newResponse

        viewModel.scanQr("DEF456")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(newResponse, viewModel.scanResult.value)
        coVerify(exactly = 2) { repository.scanQr(any()) }
    }

    // ==================== Initial state tests ====================

    @Test
    fun `initial state - has default values`() {
        assertNull(viewModel.scanResult.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
    }

    // ==================== Edge cases ====================

    @Test
    fun `scanQr - with missing pet - handles response`() = runTest {
        val missingPet = testPet.copy(isMissing = true)
        val scanResponse = ScanResponse(pet = missingPet)
        coEvery { repository.scanQr("LOST123") } returns scanResponse

        viewModel.scanQr("LOST123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.scanResult.value?.pet?.isMissing ?: false)
    }
}
