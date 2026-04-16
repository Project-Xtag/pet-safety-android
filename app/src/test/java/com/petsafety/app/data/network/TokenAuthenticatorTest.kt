package com.petsafety.app.data.network

import app.cash.turbine.test
import com.petsafety.app.data.local.AuthTokenStore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.time.Duration.Companion.milliseconds

/**
 * Covers the auth-session-loss flow that took down production for a user on
 * Apr 3 2026: a legitimate 401 triggered the authenticator, which cleared
 * tokens and emitted authExpiredEvent, which caused a downstream FCM
 * unregister DELETE, which re-entered the authenticator and re-emitted the
 * event, etc. Tests here pin the guard behaviour that prevents that loop.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TokenAuthenticatorTest {

    private lateinit var tokenStore: AuthTokenStore
    private val authTokenFlow = MutableStateFlow<String?>(null)
    private val refreshTokenFlow = MutableStateFlow<String?>(null)
    private lateinit var mockServer: MockWebServer
    private lateinit var authenticator: TokenAuthenticator

    @Before
    fun setup() {
        mockServer = MockWebServer().also { it.start() }
        tokenStore = mockk(relaxed = true)
        every { tokenStore.authToken } returns authTokenFlow as StateFlow<String?>
        every { tokenStore.refreshToken } returns refreshTokenFlow as StateFlow<String?>
        authenticator = TokenAuthenticator(tokenStore, mockServer.url("/auth/refresh").toString())
        // Every test starts from a fresh expiry-guard state
        TokenAuthenticator.resetExpiryGuard()
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        TokenAuthenticator.resetExpiryGuard()
    }

    // ----- Guard behaviour (the production loop this fix addresses) ---------

    @Test
    fun `emits authExpiredEvent exactly once on repeated 401s with no refresh token`() = runTest {
        authTokenFlow.value = "stale-access"
        refreshTokenFlow.value = null

        TokenAuthenticator.authExpiredEvent.test {
            // Burst of 401s — simulates the cascade we saw in nginx logs:
            // auth-check 401 → logout → FCM DELETE 401 → next retry 401, etc.
            repeat(10) {
                val req = authenticator.authenticate(null, response401("stale-access"))
                assertNull("Should never retry when no refresh token is stored", req)
            }

            val first = awaitItem() // single expiry notification
            assertNotNull(first)
            expectNoEvents()
        }

        // Tokens were cleared during the first authenticate() call.
        verify(atLeast = 1) { tokenStore.clearAllSync() }
    }

    @Test
    fun `skips refresh attempt once guard is armed`() = runTest {
        authTokenFlow.value = "stale-access"
        refreshTokenFlow.value = null

        // First call trips the guard.
        authenticator.authenticate(null, response401("stale-access"))

        // Now the guard is true; if a second 401 came in it must NOT attempt
        // another refresh — in the old code every re-entry would try again
        // and re-emit expiry each time. MockWebServer would receive a request
        // if refresh was attempted; assert no such request.
        val requestCountBefore = mockServer.requestCount
        repeat(5) { authenticator.authenticate(null, response401("stale-access")) }
        assertEquals(requestCountBefore, mockServer.requestCount)
    }

    @Test
    fun `resetExpiryGuard re-arms notifications so the next real expiry fires`() = runTest {
        authTokenFlow.value = "access-1"
        refreshTokenFlow.value = null

        // First expiry fires the event.
        TokenAuthenticator.authExpiredEvent.test {
            authenticator.authenticate(null, response401("access-1"))
            awaitItem()

            // Fresh login would reset the guard.
            TokenAuthenticator.resetExpiryGuard()
            authTokenFlow.value = "access-2"

            // Next 401 after guard-reset must produce a new event.
            authTokenFlow.value = "access-2"
            refreshTokenFlow.value = null
            authenticator.authenticate(null, response401("access-2"))
            awaitItem()

            expectNoEvents()
        }
    }

    // ----- Refresh success path (critical for silent token rotation) --------

    @Test
    fun `successful refresh retries request with new token and re-arms guard`() = runTest {
        authTokenFlow.value = "old-access"
        refreshTokenFlow.value = "refresh-abc"
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(
                    """{"success":true,"data":{"token":"new-access","refreshToken":"new-refresh"}}"""
                )
        )

        val retry = authenticator.authenticate(null, response401("old-access"))

        assertNotNull(retry)
        assertEquals("Bearer new-access", retry!!.header("Authorization"))
        assertEquals("true", retry.header("Retry-Auth"))
        verify { tokenStore.saveTokensSync("new-access", "new-refresh") }

        // Recorded request body includes the refresh token.
        val recorded = mockServer.takeRequest()
        assertTrue(recorded.body.readUtf8().contains("refresh-abc"))

        // Guard should NOT be set after a successful refresh: subsequent real
        // expiries must still be reportable.
        refreshTokenFlow.value = null
        authTokenFlow.value = "new-access"
        TokenAuthenticator.authExpiredEvent.test {
            authenticator.authenticate(null, response401("new-access"))
            awaitItem() // would be missed if guard was left armed
        }
    }

    @Test
    fun `failed refresh clears tokens and emits expiry once`() = runTest {
        authTokenFlow.value = "old-access"
        refreshTokenFlow.value = "refresh-abc"
        // Server says refresh token is invalid.
        mockServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"success":false,"error":"Invalid refresh token"}"""))

        TokenAuthenticator.authExpiredEvent.test {
            val retry = authenticator.authenticate(null, response401("old-access"))
            assertNull(retry)
            awaitItem()
            expectNoEvents()
        }
        verify { tokenStore.clearAllSync() }
    }

    @Test
    fun `malformed refresh response is treated as failure`() = runTest {
        authTokenFlow.value = "old-access"
        refreshTokenFlow.value = "refresh-abc"
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("""{"not":"json-we-expect"}"""))

        val retry = authenticator.authenticate(null, response401("old-access"))
        assertNull(retry)
        verify { tokenStore.clearAllSync() }
    }

    @Test
    fun `refresh network exception clears tokens`() = runTest {
        authTokenFlow.value = "old-access"
        refreshTokenFlow.value = "refresh-abc"
        // Shut down the server — the next request will throw IOException.
        mockServer.shutdown()

        val retry = authenticator.authenticate(null, response401("old-access"))
        assertNull(retry)
        verify { tokenStore.clearAllSync() }
    }

    // ----- Concurrent refresh / retry-auth header --------------------------

    @Test
    fun `skips retry when Retry-Auth header is already present`() = runTest {
        authTokenFlow.value = "some-access"
        refreshTokenFlow.value = "some-refresh"

        val alreadyRetried = Request.Builder()
            .url("http://example.invalid/protected")
            .header("Authorization", "Bearer some-access")
            .header("Retry-Auth", "true")
            .build()
        val response = Response.Builder()
            .request(alreadyRetried)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("".toResponseBody("application/json".toMediaType()))
            .build()

        assertNull(authenticator.authenticate(null, response))
        // Must not touch the refresh endpoint if we've already retried once.
        assertEquals(0, mockServer.requestCount)
    }

    @Test
    fun `retries with refreshed token if another thread refreshed concurrently`() = runTest {
        authTokenFlow.value = "new-access-from-other-thread"
        refreshTokenFlow.value = "refresh-abc"

        // Failed request used the OLD access token.
        val retry = authenticator.authenticate(null, response401("old-access"))
        assertNotNull(retry)
        assertEquals("Bearer new-access-from-other-thread", retry!!.header("Authorization"))
        assertEquals("true", retry.header("Retry-Auth"))
        // Should NOT have called the refresh endpoint — another thread did it.
        assertEquals(0, mockServer.requestCount)
    }

    // ----- Helpers ----------------------------------------------------------

    private fun response401(withBearer: String?): Response {
        val requestBuilder = Request.Builder().url("http://example.invalid/protected")
        if (withBearer != null) requestBuilder.header("Authorization", "Bearer $withBearer")
        return Response.Builder()
            .request(requestBuilder.build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .body("""{"success":false,"error":"Access token required"}""".toResponseBody("application/json".toMediaType()))
            .build()
    }
}
