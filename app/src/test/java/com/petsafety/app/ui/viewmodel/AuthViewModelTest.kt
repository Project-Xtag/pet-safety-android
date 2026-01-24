package com.petsafety.app.ui.viewmodel

import com.petsafety.app.data.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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

    // MARK: - submitSupportRequest Tests

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
