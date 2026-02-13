package com.petsafety.app.ui.viewmodel

import com.petsafety.app.R
import com.petsafety.app.data.events.SubscriptionEventBus
import com.petsafety.app.data.model.PetFoundEvent
import com.petsafety.app.data.model.SightingReportedEvent
import com.petsafety.app.data.model.TagScannedEvent
import com.petsafety.app.data.notifications.NotificationHelper
import com.petsafety.app.data.network.SseService
import com.petsafety.app.data.sync.NetworkMonitor
import com.petsafety.app.data.sync.SyncService
import com.petsafety.app.util.StringProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
class AppStateViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sseService: SseService
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var syncService: SyncService
    private lateinit var stringProvider: StringProvider
    private lateinit var viewModel: AppStateViewModel

    private val isConnectedFlow = MutableStateFlow(true)

    // Captured SSE handlers
    private var capturedTagScannedHandler: ((TagScannedEvent) -> Unit)? = null
    private var capturedSightingReportedHandler: ((SightingReportedEvent) -> Unit)? = null
    private var capturedPetFoundHandler: ((PetFoundEvent) -> Unit)? = null

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        sseService = mockk(relaxed = true)
        notificationHelper = mockk(relaxed = true)
        networkMonitor = mockk(relaxed = true)
        syncService = mockk(relaxed = true)
        stringProvider = mockk(relaxed = true)

        every { networkMonitor.isConnected } returns isConnectedFlow

        // Capture SSE handlers when they are set
        every { sseService.onTagScanned = any() } answers {
            capturedTagScannedHandler = firstArg()
        }
        every { sseService.onSightingReported = any() } answers {
            capturedSightingReportedHandler = firstArg()
        }
        every { sseService.onPetFound = any() } answers {
            capturedPetFoundHandler = firstArg()
        }

        // Setup string provider with vararg support
        every { stringProvider.getString(R.string.unknown_location) } returns "Unknown location"
        every { stringProvider.getString(R.string.tag_scanned_message, *anyVararg()) } answers {
            val args = secondArg<Array<out Any>>()
            "Tag for ${args[0]} was scanned at ${args[1]}"
        }
        every { stringProvider.getString(R.string.notification_tag_scanned_title, *anyVararg()) } answers {
            val args = secondArg<Array<out Any>>()
            "${args[0]} Tag Scanned"
        }
        every { stringProvider.getString(R.string.sighting_message_with_location, *anyVararg()) } answers {
            val args = secondArg<Array<out Any>>()
            "${args[0]} was spotted at ${args[1]}"
        }
        every { stringProvider.getString(R.string.sighting_message_no_location, *anyVararg()) } answers {
            val args = secondArg<Array<out Any>>()
            "${args[0]} was spotted"
        }
        every { stringProvider.getString(R.string.notification_sighting_title, *anyVararg()) } answers {
            val args = secondArg<Array<out Any>>()
            "Sighting: ${args[0]}"
        }
        every { stringProvider.getString(R.string.pet_found_message, *anyVararg()) } answers {
            val args = secondArg<Array<out Any>>()
            "${args[0]} has been found!"
        }
        every { stringProvider.getString(R.string.notification_pet_found_title) } returns "Pet Found!"

        viewModel = AppStateViewModel(
            sseService = sseService,
            notificationHelper = notificationHelper,
            networkMonitor = networkMonitor,
            syncService = syncService,
            stringProvider = stringProvider,
            subscriptionEventBus = SubscriptionEventBus()
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== showError tests ====================

    @Test
    fun `showError - sets snackbar message`() {
        viewModel.showError("Something went wrong")

        assertEquals("Something went wrong", viewModel.snackbarMessage.value)
    }

    // ==================== showSuccess tests ====================

    @Test
    fun `showSuccess - sets snackbar message`() {
        viewModel.showSuccess("Operation completed successfully")

        assertEquals("Operation completed successfully", viewModel.snackbarMessage.value)
    }

    // ==================== clearMessage tests ====================

    @Test
    fun `clearMessage - clears snackbar message`() {
        viewModel.showError("Error message")
        assertEquals("Error message", viewModel.snackbarMessage.value)

        viewModel.clearMessage()

        assertNull(viewModel.snackbarMessage.value)
    }

    // ==================== setLoading tests ====================

    @Test
    fun `setLoading - updates loading state to true`() {
        viewModel.setLoading(true)

        assertTrue(viewModel.isLoading.value)
    }

    @Test
    fun `setLoading - updates loading state to false`() {
        viewModel.setLoading(true)
        viewModel.setLoading(false)

        assertFalse(viewModel.isLoading.value)
    }

    // ==================== connectSse tests ====================

    @Test
    fun `connectSse - calls sseService connect`() {
        viewModel.connectSse()

        verify { sseService.connect() }
    }

    // ==================== disconnectSse tests ====================

    @Test
    fun `disconnectSse - calls sseService disconnect`() {
        viewModel.disconnectSse()

        verify { sseService.disconnect() }
    }

    // ==================== isConnected tests ====================

    @Test
    fun `isConnected - reflects network monitor state`() = runTest {
        assertTrue(viewModel.isConnected.value)

        isConnectedFlow.value = false
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isConnected.value)
    }

    // ==================== SSE handler tests ====================

    @Test
    fun `onTagScanned - shows snackbar and notification with address`() {
        val event = TagScannedEvent(
            petId = "pet-1",
            petName = "Buddy",
            qrCode = "ABC123",
            location = TagScannedEvent.Location(51.5074, -0.1278),
            address = "Central Park, London",
            scannedAt = "2024-01-01T12:00:00Z"
        )

        capturedTagScannedHandler?.invoke(event)

        assertEquals("Tag for Buddy was scanned at Central Park, London", viewModel.snackbarMessage.value)
        verify { notificationHelper.showNotification(any(), any()) }
    }

    @Test
    fun `onTagScanned - uses unknown location when address is null`() {
        val event = TagScannedEvent(
            petId = "pet-1",
            petName = "Buddy",
            qrCode = "ABC123",
            location = TagScannedEvent.Location(51.5074, -0.1278),
            address = null,
            scannedAt = "2024-01-01T12:00:00Z"
        )

        capturedTagScannedHandler?.invoke(event)

        assertEquals("Tag for Buddy was scanned at Unknown location", viewModel.snackbarMessage.value)
    }

    @Test
    fun `onSightingReported - shows snackbar and notification with address`() {
        val event = SightingReportedEvent(
            alertId = "alert-1",
            petId = "pet-1",
            petName = "Whiskers",
            sightingId = "sighting-1",
            location = SightingReportedEvent.Location(51.5074, -0.1278),
            address = "Hyde Park",
            reportedAt = "2024-01-01T12:00:00Z",
            reporterName = "John"
        )

        capturedSightingReportedHandler?.invoke(event)

        assertEquals("Whiskers was spotted at Hyde Park", viewModel.snackbarMessage.value)
        verify { notificationHelper.showNotification(any(), any()) }
    }

    @Test
    fun `onSightingReported - shows message without location when address is blank`() {
        val event = SightingReportedEvent(
            alertId = "alert-1",
            petId = "pet-1",
            petName = "Whiskers",
            sightingId = "sighting-1",
            location = SightingReportedEvent.Location(51.5074, -0.1278),
            address = "",
            reportedAt = "2024-01-01T12:00:00Z"
        )

        capturedSightingReportedHandler?.invoke(event)

        assertEquals("Whiskers was spotted", viewModel.snackbarMessage.value)
    }

    @Test
    fun `onSightingReported - shows message without location when address is null`() {
        val event = SightingReportedEvent(
            alertId = "alert-1",
            petId = "pet-1",
            petName = "Max",
            sightingId = "sighting-1",
            location = SightingReportedEvent.Location(51.5074, -0.1278),
            address = null,
            reportedAt = "2024-01-01T12:00:00Z"
        )

        capturedSightingReportedHandler?.invoke(event)

        assertEquals("Max was spotted", viewModel.snackbarMessage.value)
    }

    @Test
    fun `onPetFound - shows snackbar and notification`() {
        val event = PetFoundEvent(
            petId = "pet-1",
            petName = "Buddy",
            alertId = "alert-1",
            foundAt = "2024-01-05T12:00:00Z"
        )

        capturedPetFoundHandler?.invoke(event)

        assertEquals("Buddy has been found!", viewModel.snackbarMessage.value)
        verify { notificationHelper.showNotification("Pet Found!", "Buddy has been found!") }
    }

    // ==================== Initial state tests ====================

    @Test
    fun `initial state - has default values`() {
        assertNull(viewModel.snackbarMessage.value)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `init - sets up SSE handlers`() {
        // Verify handlers were captured during initialization
        verify { sseService.onTagScanned = any() }
        verify { sseService.onSightingReported = any() }
        verify { sseService.onPetFound = any() }
    }

    // ==================== syncService exposure tests ====================

    @Test
    fun `syncService - is accessible via viewModel`() {
        assertEquals(syncService, viewModel.syncService)
    }
}
