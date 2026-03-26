package com.petsafety.app.ui.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.petsafety.app.R
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.model.TagLookupResponse
import com.petsafety.app.data.repository.LocationConsent
import com.petsafety.app.data.repository.QrRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
    private lateinit var application: Application
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

    private val testLookupActiveWithPet = TagLookupResponse(
        exists = true,
        status = "active",
        hasPet = true,
        isOwner = false,
        canActivate = false,
        pet = testPet
    )

    private val testLookupNeedsActivation = TagLookupResponse(
        exists = true,
        status = "shipped",
        hasPet = false,
        isOwner = true,
        canActivate = true,
        pet = null
    )

    private val testLookupNotFound = TagLookupResponse(
        exists = false,
        status = null,
        hasPet = false,
        isOwner = false,
        canActivate = false,
        pet = null
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        application = mockk(relaxed = true)
        every { application.getString(R.string.error_tag_not_found) } returns "This tag was not found."
        every { application.getString(R.string.error_tag_not_activated) } returns "This tag has not been activated yet."
        every { application.getString(R.string.error_tag_lookup_failed) } returns "Failed to look up tag."
        viewModel = QrScannerViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== lookupAndRoute tests ====================

    @Test
    fun `lookupAndRoute - active tag with pet - sets ActiveWithPet state`() = runTest {
        coEvery { repository.lookupTag("ABC123") } returns testLookupActiveWithPet

        viewModel.lookupAndRoute("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.lookupState.value
        assertTrue(state is TagLookupState.ActiveWithPet)
        assertEquals(testPet, (state as TagLookupState.ActiveWithPet).lookup.pet)
    }

    @Test
    fun `lookupAndRoute - active tag with pet - fires scan for notification`() = runTest {
        coEvery { repository.lookupTag("ABC123") } returns testLookupActiveWithPet

        viewModel.lookupAndRoute("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.scanQr("ABC123") }
    }

    @Test
    fun `lookupAndRoute - active tag with pet - sets scanResult`() = runTest {
        coEvery { repository.lookupTag("ABC123") } returns testLookupActiveWithPet

        viewModel.lookupAndRoute("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testLookupActiveWithPet, viewModel.scanResult.value)
    }

    @Test
    fun `lookupAndRoute - needs activation - sets NeedsActivation state`() = runTest {
        coEvery { repository.lookupTag("UNACTIVATED") } returns testLookupNeedsActivation

        viewModel.lookupAndRoute("UNACTIVATED")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.lookupState.value
        assertTrue(state is TagLookupState.NeedsActivation)
        assertEquals("UNACTIVATED", (state as TagLookupState.NeedsActivation).qrCode)
    }

    @Test
    fun `lookupAndRoute - not found - sets NotFound state`() = runTest {
        coEvery { repository.lookupTag("UNKNOWN") } returns testLookupNotFound

        viewModel.lookupAndRoute("UNKNOWN")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.lookupState.value
        assertTrue(state is TagLookupState.NotFound)
    }

    @Test
    fun `lookupAndRoute - network error - sets Error state`() = runTest {
        coEvery { repository.lookupTag(any()) } throws RuntimeException("Network error")

        viewModel.lookupAndRoute("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.lookupState.value
        assertTrue(state is TagLookupState.Error)
    }

    @Test
    fun `lookupAndRoute - shows loading state`() = runTest {
        coEvery { repository.lookupTag(any()) } returns testLookupActiveWithPet

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.lookupAndRoute("ABC123")
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `lookupAndRoute - scan failure does not affect lookup result`() = runTest {
        coEvery { repository.lookupTag("ABC123") } returns testLookupActiveWithPet
        coEvery { repository.scanQr("ABC123") } throws RuntimeException("Scan failed")

        viewModel.lookupAndRoute("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        // State should still be ActiveWithPet despite scan failure
        val state = viewModel.lookupState.value
        assertTrue(state is TagLookupState.ActiveWithPet)
    }

    // ==================== shareLocation tests ====================

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
    fun `reset - clears all state`() = runTest {
        coEvery { repository.lookupTag("ABC123") } returns testLookupActiveWithPet

        viewModel.lookupAndRoute("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify state has values
        assertEquals(testLookupActiveWithPet, viewModel.scanResult.value)

        // Reset
        viewModel.reset()

        // Verify state is cleared
        assertNull(viewModel.scanResult.value)
        assertNull(viewModel.errorMessage.value)
        assertEquals(TagLookupState.Idle, viewModel.lookupState.value)
    }

    // ==================== Initial state tests ====================

    @Test
    fun `initial state - has default values`() {
        assertNull(viewModel.scanResult.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
        assertEquals(TagLookupState.Idle, viewModel.lookupState.value)
    }
}
