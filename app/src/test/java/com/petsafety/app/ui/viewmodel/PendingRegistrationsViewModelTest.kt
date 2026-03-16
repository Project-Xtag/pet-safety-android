package com.petsafety.app.ui.viewmodel

import com.petsafety.app.data.model.PendingRegistration
import com.petsafety.app.data.repository.OrdersRepository
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
class PendingRegistrationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: OrdersRepository
    private lateinit var viewModel: PendingRegistrationsViewModel

    private val shippedRegistration = PendingRegistration(
        orderItemId = "oi-1",
        orderId = "order-1",
        petName = "Buddy",
        createdAt = "2026-03-01T00:00:00Z",
        orderStatus = "shipped",
        mplTrackingNumber = "MPL123456",
        deliveryMethod = "home_delivery"
    )

    private val deliveredRegistration = PendingRegistration(
        orderItemId = "oi-2",
        orderId = "order-1",
        petName = "Luna",
        createdAt = "2026-03-01T00:00:00Z",
        orderStatus = "delivered"
    )

    private val processingRegistration = PendingRegistration(
        orderItemId = "oi-3",
        orderId = "order-2",
        petName = "Max",
        createdAt = "2026-03-10T00:00:00Z",
        orderStatus = "processing"
    )

    private val pendingRegistration = PendingRegistration(
        orderItemId = "oi-4",
        orderId = "order-3",
        petName = "Whiskers",
        createdAt = "2026-03-12T00:00:00Z",
        orderStatus = "pending"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = PendingRegistrationsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // MARK: - Initial State

    @Test
    fun `initial state - registrations is empty`() {
        assertTrue(viewModel.registrations.value.isEmpty())
    }

    @Test
    fun `initial state - isLoading is false`() {
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `initial state - errorMessage is null`() {
        assertNull(viewModel.errorMessage.value)
    }

    // MARK: - fetchPendingRegistrations

    @Test
    fun `fetchPendingRegistrations - success - updates registrations`() = runTest {
        val registrations = listOf(shippedRegistration, processingRegistration)
        coEvery { repository.getPendingRegistrations() } returns registrations

        viewModel.fetchPendingRegistrations()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(registrations, viewModel.registrations.value)
        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `fetchPendingRegistrations - empty list - sets empty list`() = runTest {
        coEvery { repository.getPendingRegistrations() } returns emptyList()

        viewModel.fetchPendingRegistrations()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.registrations.value.isEmpty())
    }

    @Test
    fun `fetchPendingRegistrations - error - sets error message`() = runTest {
        coEvery { repository.getPendingRegistrations() } throws RuntimeException("Network error")

        viewModel.fetchPendingRegistrations()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Network error", viewModel.errorMessage.value)
        assertTrue(viewModel.registrations.value.isEmpty())
    }

    @Test
    fun `fetchPendingRegistrations - sets loading state`() = runTest {
        coEvery { repository.getPendingRegistrations() } coAnswers {
            assertTrue(viewModel.isLoading.value)
            listOf(shippedRegistration)
        }

        viewModel.fetchPendingRegistrations()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `fetchPendingRegistrations - calls repository`() = runTest {
        coEvery { repository.getPendingRegistrations() } returns emptyList()

        viewModel.fetchPendingRegistrations()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.getPendingRegistrations() }
    }

    // MARK: - refresh

    @Test
    fun `refresh - success - updates registrations`() = runTest {
        val registrations = listOf(deliveredRegistration)
        coEvery { repository.getPendingRegistrations() } returns registrations

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(registrations, viewModel.registrations.value)
        assertFalse(viewModel.isRefreshing.value)
    }

    @Test
    fun `refresh - error - sets error message`() = runTest {
        coEvery { repository.getPendingRegistrations() } throws RuntimeException("Refresh failed")

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Refresh failed", viewModel.errorMessage.value)
        assertFalse(viewModel.isRefreshing.value)
    }

    // MARK: - Model data integrity

    @Test
    fun `registration with tracking number - preserves tracking data`() = runTest {
        coEvery { repository.getPendingRegistrations() } returns listOf(shippedRegistration)

        viewModel.fetchPendingRegistrations()
        testDispatcher.scheduler.advanceUntilIdle()

        val reg = viewModel.registrations.value[0]
        assertEquals("MPL123456", reg.mplTrackingNumber)
        assertEquals("home_delivery", reg.deliveryMethod)
    }

    @Test
    fun `registration without tracking number - null tracking fields`() = runTest {
        coEvery { repository.getPendingRegistrations() } returns listOf(processingRegistration)

        viewModel.fetchPendingRegistrations()
        testDispatcher.scheduler.advanceUntilIdle()

        val reg = viewModel.registrations.value[0]
        assertNull(reg.mplTrackingNumber)
        assertNull(reg.deliveryMethod)
    }

    @Test
    fun `mixed registrations - all statuses preserved`() = runTest {
        val all = listOf(shippedRegistration, deliveredRegistration, processingRegistration, pendingRegistration)
        coEvery { repository.getPendingRegistrations() } returns all

        viewModel.fetchPendingRegistrations()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(4, viewModel.registrations.value.size)
        assertEquals("shipped", viewModel.registrations.value[0].orderStatus)
        assertEquals("delivered", viewModel.registrations.value[1].orderStatus)
        assertEquals("processing", viewModel.registrations.value[2].orderStatus)
        assertEquals("pending", viewModel.registrations.value[3].orderStatus)
    }
}
