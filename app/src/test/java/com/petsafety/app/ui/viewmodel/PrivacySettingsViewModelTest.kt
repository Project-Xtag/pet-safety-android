package com.petsafety.app.ui.viewmodel

import com.petsafety.app.data.model.User
import com.petsafety.app.data.repository.AuthRepository
import io.mockk.*
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive test suite for Privacy Settings functionality.
 * Tests the AuthViewModel's privacy-related operations including:
 * - Loading user privacy settings
 * - Updating individual privacy fields
 * - Error handling and state management
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrivacySettingsViewModelTest {

    @MockK
    private lateinit var authRepository: AuthRepository

    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()
    private val authStateFlow = MutableStateFlow(false)

    // Test user with default privacy settings
    private val testUser = User(
        id = "user-123",
        email = "test@example.com",
        firstName = "John",
        lastName = "Doe",
        phone = "+1234567890",
        address = "123 Main St",
        city = "Test City",
        postalCode = "12345",
        country = "Hungary",
        showPhonePublicly = true,
        showEmailPublicly = true,
        showAddressPublicly = false,
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z"
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)

        // Setup default mock behavior
        every { authRepository.isBiometricEnabled() } returns false
        every { authRepository.hasStoredToken() } returns false
        every { authRepository.isAuthenticated } returns authStateFlow
        coEvery { authRepository.getCurrentUser() } returns testUser

        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun triggerAuthentication() {
        authStateFlow.value = true
        testDispatcher.scheduler.advanceUntilIdle()
    }

    // ==================== Loading Privacy Settings Tests ====================

    @Test
    fun `loadCurrentUser should populate privacy settings from user profile`() = runTest {
        // When - trigger authentication which loads user
        triggerAuthentication()

        // Then
        val currentUser = viewModel.currentUser.value
        assertNotNull(currentUser)
        assertEquals(true, currentUser?.showPhonePublicly)
        assertEquals(true, currentUser?.showEmailPublicly)
        assertEquals(false, currentUser?.showAddressPublicly)
    }

    @Test
    fun `privacy settings should be null when user has null privacy fields`() = runTest {
        // Given - user with null privacy settings
        val userWithNullSettings = testUser.copy(
            showPhonePublicly = null,
            showEmailPublicly = null,
            showAddressPublicly = null
        )
        coEvery { authRepository.getCurrentUser() } returns userWithNullSettings

        // When
        triggerAuthentication()

        // Then - null values are preserved as returned by API
        val currentUser = viewModel.currentUser.value
        assertNotNull(currentUser)
        assertNull(currentUser?.showPhonePublicly)
        assertNull(currentUser?.showEmailPublicly)
        assertNull(currentUser?.showAddressPublicly)
    }

    // ==================== Update Privacy Settings Tests ====================

    @Test
    fun `updateProfile should update showPhonePublicly successfully`() = runTest {
        // Given
        val updatedUser = testUser.copy(showPhonePublicly = false)
        coEvery { authRepository.updateUser(any()) } returns updatedUser

        triggerAuthentication()

        var callbackSuccess = false
        var callbackError: String? = "not called"

        // When
        viewModel.updateProfile(
            mapOf("show_phone_publicly" to false)
        ) { success, error ->
            callbackSuccess = success
            callbackError = error
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(callbackSuccess)
        assertNull(callbackError)
        assertEquals(false, viewModel.currentUser.value?.showPhonePublicly)
        coVerify { authRepository.updateUser(mapOf("show_phone_publicly" to false)) }
    }

    @Test
    fun `updateProfile should update showEmailPublicly successfully`() = runTest {
        // Given
        val updatedUser = testUser.copy(showEmailPublicly = false)
        coEvery { authRepository.updateUser(any()) } returns updatedUser

        triggerAuthentication()

        var callbackSuccess = false

        // When
        viewModel.updateProfile(
            mapOf("show_email_publicly" to false)
        ) { success, _ ->
            callbackSuccess = success
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(callbackSuccess)
        assertEquals(false, viewModel.currentUser.value?.showEmailPublicly)
    }

    @Test
    fun `updateProfile should update showAddressPublicly successfully`() = runTest {
        // Given
        val updatedUser = testUser.copy(showAddressPublicly = true)
        coEvery { authRepository.updateUser(any()) } returns updatedUser

        triggerAuthentication()

        var callbackSuccess = false

        // When
        viewModel.updateProfile(
            mapOf("show_address_publicly" to true)
        ) { success, _ ->
            callbackSuccess = success
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(callbackSuccess)
        assertEquals(true, viewModel.currentUser.value?.showAddressPublicly)
    }

    @Test
    fun `updateProfile should update multiple privacy settings at once`() = runTest {
        // Given
        val updatedUser = testUser.copy(
            showPhonePublicly = false,
            showEmailPublicly = false,
            showAddressPublicly = true
        )
        coEvery { authRepository.updateUser(any()) } returns updatedUser

        triggerAuthentication()

        var callbackSuccess = false

        // When
        viewModel.updateProfile(
            mapOf(
                "show_phone_publicly" to false,
                "show_email_publicly" to false,
                "show_address_publicly" to true
            )
        ) { success, _ ->
            callbackSuccess = success
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertTrue(callbackSuccess)
        val currentUser = viewModel.currentUser.value
        assertEquals(false, currentUser?.showPhonePublicly)
        assertEquals(false, currentUser?.showEmailPublicly)
        assertEquals(true, currentUser?.showAddressPublicly)
    }

    // ==================== Error Handling Tests ====================

    @Test
    fun `updateProfile should return error on network failure`() = runTest {
        // Given
        coEvery { authRepository.updateUser(any()) } throws Exception("Network error")

        triggerAuthentication()

        var callbackSuccess = true
        var callbackError: String? = null

        // When
        viewModel.updateProfile(
            mapOf("show_phone_publicly" to false)
        ) { success, error ->
            callbackSuccess = success
            callbackError = error
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(callbackSuccess)
        assertNotNull(callbackError)
        assertTrue(callbackError!!.contains("Network error"))
    }

    @Test
    fun `updateProfile should return error on server error`() = runTest {
        // Given
        coEvery { authRepository.updateUser(any()) } throws Exception("Server error: 500")

        triggerAuthentication()

        var callbackSuccess = true
        var callbackError: String? = null

        // When
        viewModel.updateProfile(
            mapOf("show_email_publicly" to false)
        ) { success, error ->
            callbackSuccess = success
            callbackError = error
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(callbackSuccess)
        assertNotNull(callbackError)
    }

    @Test
    fun `updateProfile should not modify currentUser on failure`() = runTest {
        // Given
        coEvery { authRepository.updateUser(any()) } throws Exception("Update failed")

        triggerAuthentication()

        val originalPhoneSetting = viewModel.currentUser.value?.showPhonePublicly

        // When
        viewModel.updateProfile(
            mapOf("show_phone_publicly" to false)
        ) { _, _ -> }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - original value should be preserved
        assertEquals(originalPhoneSetting, viewModel.currentUser.value?.showPhonePublicly)
    }

    // ==================== Loading State Tests ====================

    @Test
    fun `isLoading should be false after updateProfile completion`() = runTest {
        // Given
        coEvery { authRepository.updateUser(any()) } returns testUser.copy(showPhonePublicly = false)

        triggerAuthentication()

        // When
        viewModel.updateProfile(mapOf("show_phone_publicly" to false)) { _, _ -> }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should not be loading after completion
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `isLoading should be false after updateProfile failure`() = runTest {
        // Given
        coEvery { authRepository.updateUser(any()) } throws Exception("Failed")

        triggerAuthentication()

        // When
        viewModel.updateProfile(mapOf("show_phone_publicly" to false)) { _, _ -> }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertFalse(viewModel.isLoading.value)
    }

    // ==================== Edge Cases Tests ====================

    @Test
    fun `updateProfile should handle empty updates map gracefully`() = runTest {
        // Given
        coEvery { authRepository.updateUser(any()) } returns testUser

        triggerAuthentication()

        var callbackSuccess = false

        // When
        viewModel.updateProfile(emptyMap()) { success, _ ->
            callbackSuccess = success
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - should still succeed (backend may reject, but viewModel handles it)
        assertTrue(callbackSuccess)
    }

    @Test
    fun `privacy settings should persist across user reload`() = runTest {
        // Given - user with specific privacy settings
        val userWithSettings = testUser.copy(
            showPhonePublicly = false,
            showEmailPublicly = true,
            showAddressPublicly = true
        )
        coEvery { authRepository.getCurrentUser() } returns userWithSettings

        // When
        triggerAuthentication()

        // Then
        val currentUser = viewModel.currentUser.value
        assertEquals(false, currentUser?.showPhonePublicly)
        assertEquals(true, currentUser?.showEmailPublicly)
        assertEquals(true, currentUser?.showAddressPublicly)
    }
}
