package com.petsafety.app.ui.viewmodel

import app.cash.turbine.test
import com.petsafety.app.R
import com.petsafety.app.data.model.NotificationPreferences
import com.petsafety.app.data.repository.NotificationPreferencesRepository
import com.petsafety.app.util.StringProvider
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
class NotificationPreferencesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: NotificationPreferencesRepository
    private lateinit var stringProvider: StringProvider
    private lateinit var viewModel: NotificationPreferencesViewModel

    private val defaultPreferences = NotificationPreferences(
        notifyByEmail = true,
        notifyBySms = true,
        notifyByPush = true
    )

    private val customPreferences = NotificationPreferences(
        notifyByEmail = true,
        notifyBySms = false,
        notifyByPush = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        stringProvider = mockk(relaxed = true)
        every { stringProvider.getString(R.string.notification_method_required) } returns "At least one notification method is required"
        viewModel = NotificationPreferencesViewModel(repository, stringProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== loadPreferences tests ====================

    @Test
    fun `loadPreferences - success - updates preferences and original`() = runTest {
        coEvery { repository.getPreferences() } returns customPreferences

        viewModel.loadPreferences()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(customPreferences, viewModel.preferences.value)
        assertEquals(customPreferences, viewModel.original.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `loadPreferences - failure - sets error message`() = runTest {
        val errorMsg = "Failed to load preferences"
        coEvery { repository.getPreferences() } throws RuntimeException(errorMsg)

        viewModel.loadPreferences()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorMsg, viewModel.errorMessage.value)
    }

    @Test
    fun `loadPreferences - shows loading state`() = runTest {
        coEvery { repository.getPreferences() } returns defaultPreferences

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.loadPreferences()
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== updatePreferences (local) tests ====================

    @Test
    fun `updatePreferences - updates preferences state`() = runTest {
        viewModel.updatePreferences(customPreferences)

        assertEquals(customPreferences, viewModel.preferences.value)
    }

    @Test
    fun `updatePreferences - does not update original`() = runTest {
        val original = viewModel.original.value

        viewModel.updatePreferences(customPreferences)

        assertEquals(original, viewModel.original.value)
    }

    // ==================== hasChanges tests ====================

    @Test
    fun `hasChanges - returns true when preferences differ from original`() = runTest {
        // Load initial preferences
        coEvery { repository.getPreferences() } returns defaultPreferences
        viewModel.loadPreferences()
        testDispatcher.scheduler.advanceUntilIdle()

        // Make local changes
        viewModel.updatePreferences(customPreferences)

        assertTrue(viewModel.hasChanges)
    }

    @Test
    fun `hasChanges - returns false when preferences match original`() = runTest {
        // Load initial preferences
        coEvery { repository.getPreferences() } returns defaultPreferences
        viewModel.loadPreferences()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.hasChanges)
    }

    @Test
    fun `hasChanges - returns false after reverting changes`() = runTest {
        // Load initial preferences
        coEvery { repository.getPreferences() } returns defaultPreferences
        viewModel.loadPreferences()
        testDispatcher.scheduler.advanceUntilIdle()

        // Make changes
        viewModel.updatePreferences(customPreferences)
        assertTrue(viewModel.hasChanges)

        // Revert changes
        viewModel.updatePreferences(defaultPreferences)
        assertFalse(viewModel.hasChanges)
    }

    // ==================== savePreferences tests ====================

    @Test
    fun `savePreferences - success - updates preferences and original`() = runTest {
        // Setup initial state
        coEvery { repository.getPreferences() } returns defaultPreferences
        viewModel.loadPreferences()
        testDispatcher.scheduler.advanceUntilIdle()

        // Make changes
        viewModel.updatePreferences(customPreferences)

        // Save
        coEvery { repository.updatePreferences(customPreferences) } returns customPreferences
        viewModel.savePreferences()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(customPreferences, viewModel.preferences.value)
        assertEquals(customPreferences, viewModel.original.value)
        assertTrue(viewModel.showSuccess.value)
    }

    @Test
    fun `savePreferences - failure - sets error message`() = runTest {
        // Setup
        coEvery { repository.getPreferences() } returns defaultPreferences
        viewModel.loadPreferences()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updatePreferences(customPreferences)

        // Save fails
        val errorMsg = "Save failed"
        coEvery { repository.updatePreferences(any()) } throws RuntimeException(errorMsg)
        viewModel.savePreferences()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorMsg, viewModel.errorMessage.value)
        assertFalse(viewModel.showSuccess.value)
    }

    @Test
    fun `savePreferences - invalid preferences - sets validation error`() = runTest {
        // Update with invalid preferences (all disabled)
        val invalidPreferences = NotificationPreferences(
            notifyByEmail = false,
            notifyBySms = false,
            notifyByPush = false
        )
        viewModel.updatePreferences(invalidPreferences)

        viewModel.savePreferences()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("At least one notification method is required", viewModel.errorMessage.value)
        coVerify(exactly = 0) { repository.updatePreferences(any()) }
    }

    @Test
    fun `savePreferences - shows saving state`() = runTest {
        coEvery { repository.getPreferences() } returns defaultPreferences
        viewModel.loadPreferences()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.updatePreferences(customPreferences)
        coEvery { repository.updatePreferences(customPreferences) } returns customPreferences

        viewModel.isSaving.test {
            assertEquals(false, awaitItem())

            viewModel.savePreferences()
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== clearMessages tests ====================

    @Test
    fun `clearMessages - clears error and success messages`() = runTest {
        // Setup error state
        coEvery { repository.getPreferences() } throws RuntimeException("Error")
        viewModel.loadPreferences()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Error", viewModel.errorMessage.value)

        // Clear messages
        viewModel.clearMessages()

        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.showSuccess.value)
    }

    @Test
    fun `clearMessages - clears success state`() = runTest {
        // Setup success state
        coEvery { repository.getPreferences() } returns defaultPreferences
        viewModel.loadPreferences()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.updatePreferences(any()) } returns defaultPreferences
        viewModel.savePreferences()
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.showSuccess.value)

        // Clear messages
        viewModel.clearMessages()

        assertFalse(viewModel.showSuccess.value)
    }

    // ==================== Initial state tests ====================

    @Test
    fun `initial state - has default values`() {
        assertEquals(NotificationPreferences.default, viewModel.preferences.value)
        assertEquals(NotificationPreferences.default, viewModel.original.value)
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isSaving.value)
        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.showSuccess.value)
        assertFalse(viewModel.hasChanges)
    }

    // ==================== NotificationPreferences validation tests ====================

    @Test
    fun `preferences isValid - true when at least one method enabled`() {
        val validPrefs = NotificationPreferences(
            notifyByEmail = true,
            notifyBySms = false,
            notifyByPush = false
        )
        assertTrue(validPrefs.isValid)
    }

    @Test
    fun `preferences isValid - false when all methods disabled`() {
        val invalidPrefs = NotificationPreferences(
            notifyByEmail = false,
            notifyBySms = false,
            notifyByPush = false
        )
        assertFalse(invalidPrefs.isValid)
    }

    @Test
    fun `preferences enabledCount - counts enabled methods`() {
        val prefs = NotificationPreferences(
            notifyByEmail = true,
            notifyBySms = false,
            notifyByPush = true
        )
        assertEquals(2, prefs.enabledCount)
    }
}
