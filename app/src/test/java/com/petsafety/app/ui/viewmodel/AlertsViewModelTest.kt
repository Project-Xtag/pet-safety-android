package com.petsafety.app.ui.viewmodel

import app.cash.turbine.test
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.MissingPetAlert
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.repository.AlertsRepository
import com.petsafety.app.data.repository.OfflineQueuedException
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
class AlertsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AlertsRepository
    private lateinit var viewModel: AlertsViewModel

    private val testPet = Pet(
        id = "pet-1",
        ownerId = "user-1",
        name = "Buddy",
        species = "Dog",
        isMissing = true,
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z"
    )

    private val testAlert = MissingPetAlert(
        id = "alert-1",
        petId = "pet-1",
        userId = "user-1",
        status = "active",
        lastSeenLocation = "Central Park",
        lastSeenLatitude = 40.7829,
        lastSeenLongitude = -73.9654,
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z",
        pet = testPet
    )

    private val foundAlert = MissingPetAlert(
        id = "alert-2",
        petId = "pet-2",
        userId = "user-1",
        status = "found",
        lastSeenLocation = "Times Square",
        createdAt = "2024-01-02T00:00:00Z",
        updatedAt = "2024-01-03T00:00:00Z"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = AlertsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== fetchAlerts tests ====================

    @Test
    fun `fetchAlerts - success - updates alerts list`() = runTest {
        val alerts = listOf(testAlert, foundAlert)
        coEvery { repository.fetchAlerts() } returns (alerts to null)

        viewModel.fetchAlerts()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(alerts, viewModel.alerts.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `fetchAlerts - with error - sets error message`() = runTest {
        val errorMsg = "Network error"
        coEvery { repository.fetchAlerts() } returns (emptyList<MissingPetAlert>() to errorMsg)

        viewModel.fetchAlerts()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorMsg, viewModel.errorMessage.value)
    }

    @Test
    fun `fetchAlerts - shows loading state`() = runTest {
        coEvery { repository.fetchAlerts() } returns (listOf(testAlert) to null)

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.fetchAlerts()
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== fetchNearbyAlerts tests ====================

    @Test
    fun `fetchNearbyAlerts - success - separates active and found`() = runTest {
        val alerts = listOf(testAlert, foundAlert)
        coEvery { repository.fetchNearbyAlerts(any(), any(), any()) } returns alerts

        viewModel.fetchNearbyAlerts(51.5074, -0.1278, 10.0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.missingAlerts.value.size)
        assertEquals(1, viewModel.foundAlerts.value.size)
        assertEquals("active", viewModel.missingAlerts.value.first().status)
        assertEquals("found", viewModel.foundAlerts.value.first().status)
    }

    @Test
    fun `fetchNearbyAlerts - failure - sets error message`() = runTest {
        val errorMsg = "Location error"
        coEvery { repository.fetchNearbyAlerts(any(), any(), any()) } throws RuntimeException(errorMsg)

        viewModel.fetchNearbyAlerts(51.5074, -0.1278, 10.0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorMsg, viewModel.errorMessage.value)
    }

    @Test
    fun `fetchNearbyAlerts - stores last location for refresh`() = runTest {
        val alerts = listOf(testAlert)
        coEvery { repository.fetchNearbyAlerts(51.5, -0.12, 15.0) } returns alerts

        viewModel.fetchNearbyAlerts(51.5, -0.12, 15.0)
        testDispatcher.scheduler.advanceUntilIdle()

        // Now refresh and verify same coordinates are used
        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { repository.fetchNearbyAlerts(51.5, -0.12, 15.0) }
    }

    // ==================== refresh tests ====================

    @Test
    fun `refresh - updates alerts and shows refreshing state`() = runTest {
        val alerts = listOf(testAlert)
        coEvery { repository.fetchNearbyAlerts(any(), any(), any()) } returns alerts

        viewModel.isRefreshing.test {
            assertEquals(false, awaitItem())

            viewModel.refresh()
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }

        assertEquals(1, viewModel.missingAlerts.value.size)
    }

    @Test
    fun `refresh - failure - sets error but completes`() = runTest {
        coEvery { repository.fetchNearbyAlerts(any(), any(), any()) } throws RuntimeException("Refresh failed")

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isRefreshing.value)
        assertEquals("Refresh failed", viewModel.errorMessage.value)
    }

    // ==================== createAlert tests ====================

    @Test
    fun `createAlert - success - calls callback with success`() = runTest {
        val location = LocationCoordinate(51.5074, -0.1278)
        coEvery { repository.createAlert("pet-1", "London", location, "Lost in park") } returns Result.success(testAlert)

        var success = false
        var error: String? = null

        viewModel.createAlert("pet-1", "London", location, "Lost in park") { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
    }

    @Test
    fun `createAlert - offline queued - still returns success`() = runTest {
        coEvery { repository.createAlert(any(), any(), any(), any()) } returns Result.failure(OfflineQueuedException())

        var success = false

        viewModel.createAlert("pet-1", "London", null, null) { s, _ -> success = s }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
    }

    @Test
    fun `createAlert - failure - returns error`() = runTest {
        val errorMsg = "Alert creation failed"
        coEvery { repository.createAlert(any(), any(), any(), any()) } returns Result.failure(RuntimeException(errorMsg))

        var success = false
        var error: String? = null

        viewModel.createAlert("pet-1", "London", null, null) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
    }

    @Test
    fun `createAlert - shows loading state`() = runTest {
        coEvery { repository.createAlert(any(), any(), any(), any()) } returns Result.success(testAlert)

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.createAlert("pet-1", "London", null, null) { _, _ -> }
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== updateAlertStatus tests ====================

    @Test
    fun `updateAlertStatus - success - calls callback`() = runTest {
        coEvery { repository.updateAlertStatus("alert-1", "found") } returns foundAlert

        var success = false

        viewModel.updateAlertStatus("alert-1", "found") { s, _ -> success = s }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        coVerify { repository.updateAlertStatus("alert-1", "found") }
    }

    @Test
    fun `updateAlertStatus - failure - returns error`() = runTest {
        val errorMsg = "Update failed"
        coEvery { repository.updateAlertStatus(any(), any()) } throws RuntimeException(errorMsg)

        var success = false
        var error: String? = null

        viewModel.updateAlertStatus("alert-1", "found") { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
    }

    // ==================== reportSighting tests ====================

    @Test
    fun `reportSighting - success - calls callback`() = runTest {
        val location = LocationCoordinate(51.5, -0.12)
        coEvery {
            repository.reportSighting(
                "alert-1",
                "John Doe",
                "123456789",
                "john@example.com",
                "Hyde Park",
                location,
                "Saw the dog near fountain"
            )
        } returns Result.success(Unit)

        var success = false

        viewModel.reportSighting(
            alertId = "alert-1",
            reporterName = "John Doe",
            reporterPhone = "123456789",
            reporterEmail = "john@example.com",
            location = "Hyde Park",
            coordinate = location,
            notes = "Saw the dog near fountain"
        ) { s, _ -> success = s }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
    }

    @Test
    fun `reportSighting - offline queued - still returns success`() = runTest {
        coEvery {
            repository.reportSighting(any(), any(), any(), any(), any(), any(), any())
        } returns Result.failure(OfflineQueuedException())

        var success = false

        viewModel.reportSighting(
            alertId = "alert-1",
            reporterName = null,
            reporterPhone = null,
            reporterEmail = null,
            location = "Park",
            coordinate = null,
            notes = null
        ) { s, _ -> success = s }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
    }

    @Test
    fun `reportSighting - failure - returns error`() = runTest {
        val errorMsg = "Report failed"
        coEvery {
            repository.reportSighting(any(), any(), any(), any(), any(), any(), any())
        } returns Result.failure(RuntimeException(errorMsg))

        var success = false
        var error: String? = null

        viewModel.reportSighting(
            alertId = "alert-1",
            reporterName = null,
            reporterPhone = null,
            reporterEmail = null,
            location = "Park",
            coordinate = null,
            notes = null
        ) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
    }

    @Test
    fun `reportSighting - with all optional fields null - succeeds`() = runTest {
        coEvery {
            repository.reportSighting("alert-1", null, null, null, null, null, null)
        } returns Result.success(Unit)

        var success = false

        viewModel.reportSighting(
            alertId = "alert-1",
            reporterName = null,
            reporterPhone = null,
            reporterEmail = null,
            location = null,
            coordinate = null,
            notes = null
        ) { s, _ -> success = s }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        coVerify { repository.reportSighting("alert-1", null, null, null, null, null, null) }
    }

    // ==================== Edge case tests ====================

    @Test
    fun `fetchNearbyAlerts - empty response - sets empty lists`() = runTest {
        coEvery { repository.fetchNearbyAlerts(any(), any(), any()) } returns emptyList()

        viewModel.fetchNearbyAlerts(51.5074, -0.1278, 10.0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.missingAlerts.value.isEmpty())
        assertTrue(viewModel.foundAlerts.value.isEmpty())
    }

    @Test
    fun `fetchNearbyAlerts - all active - found list empty`() = runTest {
        val activeAlerts = listOf(
            testAlert,
            testAlert.copy(id = "alert-3", status = "active")
        )
        coEvery { repository.fetchNearbyAlerts(any(), any(), any()) } returns activeAlerts

        viewModel.fetchNearbyAlerts(51.5074, -0.1278, 10.0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.missingAlerts.value.size)
        assertTrue(viewModel.foundAlerts.value.isEmpty())
    }

    @Test
    fun `fetchNearbyAlerts - all found - missing list empty`() = runTest {
        val foundAlerts = listOf(
            foundAlert,
            foundAlert.copy(id = "alert-3")
        )
        coEvery { repository.fetchNearbyAlerts(any(), any(), any()) } returns foundAlerts

        viewModel.fetchNearbyAlerts(51.5074, -0.1278, 10.0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.missingAlerts.value.isEmpty())
        assertEquals(2, viewModel.foundAlerts.value.size)
    }
}
