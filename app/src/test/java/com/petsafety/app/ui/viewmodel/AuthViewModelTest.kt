package com.petsafety.app.ui.viewmodel

import app.cash.turbine.test
import com.petsafety.app.data.model.User
import com.petsafety.app.data.network.model.CanDeleteAccountResponse
import com.petsafety.app.data.network.model.MissingPetInfo
import com.petsafety.app.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: AuthViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val testUser = User(
        id = "user-1",
        email = "test@example.com",
        firstName = "John",
        lastName = "Doe"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk(relaxed = true)

        // Setup default mock behavior
        every { authRepository.isAuthenticated } returns flowOf(false)
        every { authRepository.isBiometricEnabled() } returns false
        every { authRepository.hasStoredToken() } returns false

        viewModel = AuthViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== Initial State tests ====================

    @Test
    fun `initial state - not authenticated`() = runTest {
        assertEquals(false, viewModel.isAuthenticated.value)
        assertNull(viewModel.currentUser.value)
        assertEquals(false, viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `biometric enabled - shows prompt on startup`() = runTest {
        every { authRepository.hasStoredToken() } returns true
        every { authRepository.isBiometricEnabled() } returns true
        every { authRepository.isAuthenticated } returns MutableStateFlow(false)

        viewModel = AuthViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.showBiometricPrompt.value)
    }

    @Test
    fun `no stored token - no biometric prompt`() = runTest {
        every { authRepository.hasStoredToken() } returns false
        every { authRepository.isBiometricEnabled() } returns true

        viewModel = AuthViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.showBiometricPrompt.value)
    }

    // ==================== Login tests ====================

    @Test
    fun `login - success - calls onSuccess callback`() = runTest {
        coEvery { authRepository.login("test@example.com") } returns Unit

        var successCalled = false
        var failureMessage: String? = null

        viewModel.login(
            email = "test@example.com",
            onSuccess = { successCalled = true },
            onFailure = { failureMessage = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertNull(failureMessage)
        coVerify { authRepository.login("test@example.com") }
    }

    @Test
    fun `login - failure - calls onFailure callback with error`() = runTest {
        val errorMsg = "Invalid email"
        coEvery { authRepository.login(any()) } throws RuntimeException(errorMsg)

        var successCalled = false
        var failureMessage: String? = null

        viewModel.login(
            email = "test@example.com",
            onSuccess = { successCalled = true },
            onFailure = { failureMessage = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(successCalled)
        assertEquals(errorMsg, failureMessage)
        assertEquals(errorMsg, viewModel.errorMessage.value)
    }

    @Test
    fun `login - shows loading state`() = runTest {
        coEvery { authRepository.login(any()) } returns Unit

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.login("test@example.com", {}, {})
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== Verify OTP tests ====================

    @Test
    fun `verifyOtp - success - sets authenticated and user`() = runTest {
        coEvery { authRepository.verifyOtp("test@example.com", "123456") } returns testUser

        var successCalled = false

        viewModel.verifyOtp(
            email = "test@example.com",
            code = "123456",
            onSuccess = { successCalled = true },
            onFailure = {}
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(successCalled)
        assertTrue(viewModel.isAuthenticated.value)
        assertEquals(testUser, viewModel.currentUser.value)
    }

    @Test
    fun `verifyOtp - failure - calls onFailure callback`() = runTest {
        val errorMsg = "Invalid OTP"
        coEvery { authRepository.verifyOtp(any(), any()) } throws RuntimeException(errorMsg)

        var failureMessage: String? = null

        viewModel.verifyOtp(
            email = "test@example.com",
            code = "wrong",
            onSuccess = {},
            onFailure = { failureMessage = it }
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorMsg, failureMessage)
        assertFalse(viewModel.isAuthenticated.value)
    }

    // ==================== Logout tests ====================

    @Test
    fun `logout - clears user and auth state`() = runTest {
        // First, simulate logged in state
        coEvery { authRepository.verifyOtp(any(), any()) } returns testUser
        viewModel.verifyOtp("test@example.com", "123456", {}, {})
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isAuthenticated.value)

        // Now logout
        viewModel.logout()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isAuthenticated.value)
        assertNull(viewModel.currentUser.value)
        coVerify { authRepository.logout() }
    }

    // ==================== Biometric tests ====================

    @Test
    fun `setBiometricEnabled - updates state and repository`() = runTest {
        viewModel.setBiometricEnabled(true)

        assertTrue(viewModel.biometricEnabled.value)
        verify { authRepository.setBiometricEnabled(true) }
    }

    @Test
    fun `onBiometricSuccess - authenticates user`() = runTest {
        coEvery { authRepository.getCurrentUser() } returns testUser

        viewModel.onBiometricSuccess()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.showBiometricPrompt.value)
        assertTrue(viewModel.isAuthenticated.value)
    }

    @Test
    fun `onBiometricCancelled - dismisses prompt`() = runTest {
        every { authRepository.hasStoredToken() } returns true
        every { authRepository.isBiometricEnabled() } returns true
        viewModel = AuthViewModel(authRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onBiometricCancelled()

        assertFalse(viewModel.showBiometricPrompt.value)
    }

    // ==================== Update Profile tests ====================

    @Test
    fun `updateProfile - success - updates current user`() = runTest {
        val updates = mapOf("first_name" to "Jane" as Any)
        val updatedUser = testUser.copy(firstName = "Jane")
        coEvery { authRepository.updateUser(updates) } returns updatedUser

        var success = false

        viewModel.updateProfile(updates) { s, _ -> success = s }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertEquals("Jane", viewModel.currentUser.value?.firstName)
    }

    @Test
    fun `updateProfile - failure - returns error`() = runTest {
        val updates = mapOf("first_name" to "Jane" as Any)
        coEvery { authRepository.updateUser(any()) } throws RuntimeException("Update failed")

        var success = false
        var error: String? = null

        viewModel.updateProfile(updates) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals("Update failed", error)
    }

    // ==================== Delete Account tests ====================

    @Test
    fun `canDeleteAccount - returns eligibility response`() = runTest {
        val response = CanDeleteAccountResponse(canDelete = true, missingPets = emptyList())
        coEvery { authRepository.canDeleteAccount() } returns response

        var result: CanDeleteAccountResponse? = null

        viewModel.canDeleteAccount { r, _ -> result = r }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(response, result)
        assertTrue(result?.canDelete ?: false)
    }

    @Test
    fun `canDeleteAccount - with missing pets - returns blocked response`() = runTest {
        val response = CanDeleteAccountResponse(
            canDelete = false,
            missingPets = listOf(
                MissingPetInfo(id = "pet-1", name = "Buddy"),
                MissingPetInfo(id = "pet-2", name = "Max")
            )
        )
        coEvery { authRepository.canDeleteAccount() } returns response

        var result: CanDeleteAccountResponse? = null

        viewModel.canDeleteAccount { r, _ -> result = r }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(result?.canDelete ?: true)
        assertEquals(2, result?.missingPets?.size)
    }

    @Test
    fun `deleteAccount - success - clears auth state`() = runTest {
        coEvery { authRepository.deleteAccount() } returns Unit

        var success = false

        viewModel.deleteAccount { s, _ -> success = s }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertFalse(viewModel.isAuthenticated.value)
        assertNull(viewModel.currentUser.value)
    }

    @Test
    fun `deleteAccount - failure - returns error`() = runTest {
        coEvery { authRepository.deleteAccount() } throws RuntimeException("Cannot delete")

        var success = false
        var error: String? = null

        viewModel.deleteAccount { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals("Cannot delete", error)
    }

    // ==================== submitSupportRequest Tests ====================

    @Test
    fun `submitSupportRequest calls onSuccess with ticketId on success`() = runTest {
        // Given
        val expectedTicketId = "SUP-TEST-1234"
        var resultTicketId: String? = null
        var errorMessage: String? = null
        val latch = CountDownLatch(1)

        coEvery {
            authRepository.submitSupportRequest(any(), any(), any())
        } returns expectedTicketId

        // When
        viewModel.submitSupportRequest(
            category = "General",
            subject = "Test subject",
            message = "Test message",
            onSuccess = { ticketId ->
                resultTicketId = ticketId
                latch.countDown()
            },
            onError = { error ->
                errorMessage = error
                latch.countDown()
            }
        )

        // Advance coroutines
        testDispatcher.scheduler.advanceUntilIdle()
        latch.await(1, TimeUnit.SECONDS)

        // Then
        assertEquals(expectedTicketId, resultTicketId)
        assertEquals(null, errorMessage)
    }

    @Test
    fun `submitSupportRequest calls onError on failure`() = runTest {
        // Given
        val expectedError = "Network error"
        var resultTicketId: String? = null
        var errorMessage: String? = null
        val latch = CountDownLatch(1)

        coEvery {
            authRepository.submitSupportRequest(any(), any(), any())
        } throws Exception(expectedError)

        // When
        viewModel.submitSupportRequest(
            category = "General",
            subject = "Test subject",
            message = "Test message",
            onSuccess = { ticketId ->
                resultTicketId = ticketId
                latch.countDown()
            },
            onError = { error ->
                errorMessage = error
                latch.countDown()
            }
        )

        // Advance coroutines
        testDispatcher.scheduler.advanceUntilIdle()
        latch.await(1, TimeUnit.SECONDS)

        // Then
        assertEquals(null, resultTicketId)
        assertEquals(expectedError, errorMessage)
    }

    @Test
    fun `submitSupportRequest passes correct parameters to repository`() = runTest {
        // Given
        val category = "Technical Issue"
        val subject = "App crashes"
        val message = "Detailed description of the issue"
        val latch = CountDownLatch(1)

        coEvery {
            authRepository.submitSupportRequest(any(), any(), any())
        } returns "SUP-TEST-0001"

        // When
        viewModel.submitSupportRequest(
            category = category,
            subject = subject,
            message = message,
            onSuccess = { latch.countDown() },
            onError = { latch.countDown() }
        )

        // Advance coroutines
        testDispatcher.scheduler.advanceUntilIdle()
        latch.await(1, TimeUnit.SECONDS)

        // Then
        coVerify {
            authRepository.submitSupportRequest(category, subject, message)
        }
    }

    @Test
    fun `submitSupportRequest handles null exception message`() = runTest {
        // Given
        var errorMessage: String? = null
        val latch = CountDownLatch(1)

        coEvery {
            authRepository.submitSupportRequest(any(), any(), any())
        } throws Exception()  // Exception with null message

        // When
        viewModel.submitSupportRequest(
            category = "General",
            subject = "Test",
            message = "Test",
            onSuccess = { latch.countDown() },
            onError = { error ->
                errorMessage = error
                latch.countDown()
            }
        )

        // Advance coroutines
        testDispatcher.scheduler.advanceUntilIdle()
        latch.await(1, TimeUnit.SECONDS)

        // Then
        assertEquals("Failed to submit support request", errorMessage)
    }
}
