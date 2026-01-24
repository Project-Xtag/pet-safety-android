package com.petsafety.app.data.repository

import com.petsafety.app.data.local.AuthTokenStore
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.ApiEnvelope
import com.petsafety.app.data.network.model.SupportRequest
import com.petsafety.app.data.network.model.SupportRequestResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var apiService: ApiService
    private lateinit var tokenStore: AuthTokenStore
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        apiService = mockk()
        tokenStore = mockk(relaxed = true)
        authRepository = AuthRepository(apiService, tokenStore)
    }

    // MARK: - submitSupportRequest Tests

    @Test
    fun `submitSupportRequest returns ticketId on success`() = runTest {
        // Given
        val category = "Technical Issue"
        val subject = "App crashes on startup"
        val message = "The app crashes every time I open it."
        val expectedTicketId = "SUP-ABC123-XYZ9"

        coEvery {
            apiService.submitSupportRequest(any())
        } returns ApiEnvelope(
            success = true,
            data = SupportRequestResponse(
                ticketId = expectedTicketId,
                message = "Support request submitted successfully."
            ),
            error = null
        )

        // When
        val result = authRepository.submitSupportRequest(category, subject, message)

        // Then
        assertEquals(expectedTicketId, result)
        coVerify {
            apiService.submitSupportRequest(
                SupportRequest(
                    category = category,
                    subject = subject,
                    message = message
                )
            )
        }
    }

    @Test
    fun `submitSupportRequest throws exception on API error`() = runTest {
        // Given
        val errorMessage = "Subject and message are required"
        coEvery {
            apiService.submitSupportRequest(any())
        } returns ApiEnvelope(
            success = false,
            data = null,
            error = errorMessage
        )

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            kotlinx.coroutines.runBlocking {
                authRepository.submitSupportRequest("General", "Test", "Test message")
            }
        }
        assertEquals(errorMessage, exception.message)
    }

    @Test
    fun `submitSupportRequest throws exception when ticketId is missing`() = runTest {
        // Given
        coEvery {
            apiService.submitSupportRequest(any())
        } returns ApiEnvelope(
            success = true,
            data = null,
            error = null
        )

        // When/Then
        val exception = assertThrows(Exception::class.java) {
            kotlinx.coroutines.runBlocking {
                authRepository.submitSupportRequest("General", "Test", "Test message")
            }
        }
        assertEquals("Missing ticket ID", exception.message)
    }

    @Test
    fun `submitSupportRequest sends correct request body`() = runTest {
        // Given
        val category = "Billing"
        val subject = "Subscription issue"
        val message = "I was charged twice for my subscription."

        coEvery {
            apiService.submitSupportRequest(any())
        } returns ApiEnvelope(
            success = true,
            data = SupportRequestResponse(
                ticketId = "SUP-TEST-1234",
                message = "Success"
            ),
            error = null
        )

        // When
        authRepository.submitSupportRequest(category, subject, message)

        // Then
        coVerify {
            apiService.submitSupportRequest(
                match { request ->
                    request.category == category &&
                    request.subject == subject &&
                    request.message == message
                }
            )
        }
    }
}
