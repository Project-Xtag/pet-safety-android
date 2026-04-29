package com.petsafety.app.data.network

import com.petsafety.app.data.config.ConfigurationManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [AppCheckInterceptor] focused on fail-closed gating (audit H47).
 *
 * Pre-fix the interceptor proceeded with no header on token failure, which
 * leaves the backend defenceless once WS8.7 ships server-side enforcement.
 * The fix lets ops flip a Remote Config flag to swap from fail-open to
 * fail-closed without an app release. The DEBUG-build short-circuit is
 * preserved so developers don't get locked out by Firebase debug-token
 * rate limits.
 */
class AppCheckInterceptorTest {

    private val configManager: ConfigurationManager = mockk(relaxed = true)

    private fun makeInterceptor(
        isDebug: Boolean,
        enforce: Boolean,
    ): AppCheckInterceptor {
        return AppCheckInterceptor(configManager).also {
            it.setBehaviorForTest(
                isDebugBuild = { isDebug },
                enforceFailClosed = { enforce },
            )
        }
    }

    private fun fakeChain(request: Request, onDial: () -> Unit = {}): Interceptor.Chain {
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            onDial()
            Response.Builder()
                .request(firstArg())
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("ok".toResponseBody(null))
                .build()
        }
        return chain
    }

    private fun req() = Request.Builder()
        .url("https://api.example.com/v1/x".toHttpUrl())
        .build()

    @Test
    fun `attaches header when token is available`() {
        coEvery { configManager.getAppCheckToken(any()) } returns "abc.def.ghi"
        val interceptor = makeInterceptor(isDebug = false, enforce = true)

        var seen: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req()
        every { chain.proceed(any()) } answers {
            seen = firstArg()
            Response.Builder()
                .request(seen!!)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("ok".toResponseBody(null))
                .build()
        }

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals("abc.def.ghi", seen?.header("X-Firebase-AppCheck"))
    }

    @Test
    fun `proceeds without header in DEBUG even when token is null`() {
        coEvery { configManager.getAppCheckToken(any()) } returns null
        // Enforcement on, but DEBUG short-circuit must win regardless.
        val interceptor = makeInterceptor(isDebug = true, enforce = true)

        var seen: Request? = null
        val chain = mockk<Interceptor.Chain>()
        every { chain.request() } returns req()
        every { chain.proceed(any()) } answers {
            seen = firstArg()
            Response.Builder()
                .request(seen!!)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("ok".toResponseBody(null))
                .build()
        }

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertNull("DEBUG must never block on App Check failure", seen?.header("X-Firebase-AppCheck"))
    }

    @Test
    fun `release build proceeds without header when enforcement is off (legacy fail-open)`() {
        coEvery { configManager.getAppCheckToken(any()) } returns null
        val interceptor = makeInterceptor(isDebug = false, enforce = false)

        var dialed = false
        val chain = fakeChain(req()) { dialed = true }

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertTrue("network must still be dialed when enforcement is off", dialed)
    }

    @Test
    fun `release build returns synthetic 503 without dialing when enforcement is on`() {
        coEvery { configManager.getAppCheckToken(any()) } returns null
        val interceptor = makeInterceptor(isDebug = false, enforce = true)

        var dialed = false
        val chain = fakeChain(req()) { dialed = true }

        val response = interceptor.intercept(chain)

        assertEquals(503, response.code)
        assertNotNull(response.body)
        val body = response.body?.string() ?: ""
        assertTrue(
            "fail-closed body should be the structured payload — got: $body",
            body.contains("App attestation unavailable"),
        )
        assertTrue("network must NOT be dialed under fail-closed", !dialed)
        verify(exactly = 0) { chain.proceed(any()) }
    }
}
