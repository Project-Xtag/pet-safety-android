package com.petsafety.app.data.repository

import com.petsafety.app.data.fcm.FCMRepository
import com.petsafety.app.data.local.AuthTokenStore
import com.petsafety.app.data.model.User
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.TokenAuthenticator
import com.petsafety.app.data.network.model.ApiEnvelope
import com.petsafety.app.data.network.model.SupportRequest
import com.petsafety.app.data.network.model.SupportRequestResponse
import com.petsafety.app.data.network.model.VerifyOtpResponse
import io.mockk.Called
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
    private lateinit var fcmRepository: FCMRepository
    private lateinit var authRepository: AuthRepository

    @Before
    fun setup() {
        apiService = mockk()
        tokenStore = mockk(relaxed = true)
        fcmRepository = mockk(relaxed = true)
        authRepository = AuthRepository(apiService, tokenStore, fcmRepository)
        TokenAuthenticator.resetExpiryGuard()
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

    // ============================================================
    // Logout semantics — regression coverage for the April 3 loop.
    //
    // Before the fix, logout() unconditionally called unregisterFCMToken()
    // even in the TOKEN_EXPIRED path. That DELETE fires with just-cleared
    // credentials → 401 → TokenAuthenticator → re-emit expiry → logout()
    // again → another DELETE → another 401… Production logs showed a device
    // flooding the backend with ~600 DELETE /users/me/fcm-tokens/{...} 401s
    // in ~3 minutes. These tests pin the behaviour that prevents it.
    // ============================================================

    @Test
    fun `logout USER_INITIATED unregisters FCM token`() = runTest {
        authRepository.logout(LogoutReason.USER_INITIATED)

        coVerify { fcmRepository.removeToken() }
        coVerify { tokenStore.clearAuthToken() }
        coVerify { tokenStore.clearRefreshToken() }
        coVerify { tokenStore.clearUserInfo() }
    }

    @Test
    fun `logout TOKEN_EXPIRED skips FCM unregister to avoid the 401 cascade`() = runTest {
        authRepository.logout(LogoutReason.TOKEN_EXPIRED)

        coVerify(exactly = 0) { fcmRepository.removeToken() }
        // Local state is still fully cleared.
        coVerify { tokenStore.clearAuthToken() }
        coVerify { tokenStore.clearRefreshToken() }
        coVerify { tokenStore.clearUserInfo() }
    }

    @Test
    fun `logout defaults to USER_INITIATED for backwards compatibility`() = runTest {
        authRepository.logout()

        coVerify { fcmRepository.removeToken() }
    }

    @Test
    fun `verifyOtp does not call FCM register itself - ViewModel observer handles it`() = runTest {
        val user = User(id = "u1", email = "a@b.com", firstName = "A", lastName = "B")
        coEvery { apiService.verifyOtp(any()) } returns ApiEnvelope(
            success = true,
            data = VerifyOtpResponse(token = "t", refreshToken = "r", user = user, isNewUser = false),
            error = null
        )

        authRepository.verifyOtp("a@b.com", "123456", null, null)

        // Repository shouldn't touch FCM — AuthViewModel observes
        // `isAuthenticated` and calls registerToken() itself. Before the fix,
        // both paths fired, registering the same token twice per login.
        coVerify { fcmRepository wasNot Called }
    }

    @Test
    fun `verifyOtp persists tokens and user info`() = runTest {
        val user = User(id = "u1", email = "a@b.com", firstName = "A", lastName = "B")
        coEvery { apiService.verifyOtp(any()) } returns ApiEnvelope(
            success = true,
            data = VerifyOtpResponse(token = "access-xyz", refreshToken = "refresh-xyz", user = user, isNewUser = false),
            error = null
        )

        val result = authRepository.verifyOtp("a@b.com", "123456", null, null)

        assertEquals(user, result.user)
        coVerify { tokenStore.saveAuthToken("access-xyz") }
        coVerify { tokenStore.saveRefreshToken("refresh-xyz") }
        coVerify { tokenStore.saveUserInfo("u1", "a@b.com", "A") }
    }

    @Test
    fun `verifyOtp failure surfaces API error`() = runTest {
        coEvery { apiService.verifyOtp(any()) } returns ApiEnvelope(
            success = false,
            data = null,
            error = "Invalid OTP"
        )

        val ex = assertThrows(Exception::class.java) {
            kotlinx.coroutines.runBlocking {
                authRepository.verifyOtp("a@b.com", "000000", null, null)
            }
        }
        assertEquals("Invalid OTP", ex.message)

        // Error path: no tokens should be stored.
        coVerify(exactly = 0) { tokenStore.saveAuthToken(any()) }
        coVerify(exactly = 0) { tokenStore.saveRefreshToken(any()) }
    }

    @Test
    fun `verifyOtp tolerates FCM unregister failure during logout`() = runTest {
        coEvery { fcmRepository.removeToken() } throws RuntimeException("network down")

        // Must not throw — best-effort cleanup on user logout.
        authRepository.logout(LogoutReason.USER_INITIATED)

        coVerify { tokenStore.clearAuthToken() }
    }

    // ----------------------------------------------------------
    // submitSupportRequest (continuing from above)
    // ----------------------------------------------------------

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
