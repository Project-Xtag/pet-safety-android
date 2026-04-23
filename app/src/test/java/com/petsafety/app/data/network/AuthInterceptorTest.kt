package com.petsafety.app.data.network

import com.petsafety.app.data.local.AuthTokenStore
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * Pass 2 audit fix — AuthInterceptor must not ship a malformed JWT.
 *
 * Prior behaviour: any non-blank string in the store was appended to the
 * Authorization header. A corrupted SharedPrefs entry (we've seen this
 * after a failed device-to-device migration) produced a permanent 401
 * loop via TokenAuthenticator. New behaviour: validate the 3-part
 * base64url shape and skip the header if it's malformed — the
 * subsequent 401 on a protected route flows through
 * TokenAuthenticator's existing clear path in a suspend context.
 */
class AuthInterceptorTest {

    private lateinit var tokenStore: AuthTokenStore
    private val authTokenFlow = MutableStateFlow<String?>(null)
    private lateinit var interceptor: AuthInterceptor

    @Before
    fun setup() {
        tokenStore = mockk(relaxed = true)
        every { tokenStore.authToken } returns authTokenFlow
        interceptor = AuthInterceptor(tokenStore)
    }

    private fun fakeChain(onProceed: (Request) -> Unit = {}): Interceptor.Chain {
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder().url("https://api.senra.pet/test").build()
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val actual = it.invocation.args[0] as Request
            onProceed(actual)
            Response.Builder()
                .request(actual)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("".toResponseBody("application/json".toMediaType()))
                .build()
        }
        return chain
    }

    @Test
    fun `ships well-formed JWT as bearer token`() {
        authTokenFlow.value = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NSJ9.abc-_D" // 3 parts
        var sentAuth: String? = null
        val chain = fakeChain { req -> sentAuth = req.header("Authorization") }
        interceptor.intercept(chain)
        assertEquals("Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NSJ9.abc-_D", sentAuth)
    }

    @Test
    fun `rejects tokens that aren't 3 dot-separated parts`() {
        authTokenFlow.value = "not-a-jwt-at-all"
        var sentAuth: String? = null
        val chain = fakeChain { req -> sentAuth = req.header("Authorization") }
        interceptor.intercept(chain)
        assertNull(sentAuth)
    }

    @Test
    fun `rejects JWT-shape with an empty part`() {
        authTokenFlow.value = "eyJhbGciOiJIUzI1NiJ9..abc" // middle part empty
        var sentAuth: String? = null
        val chain = fakeChain { req -> sentAuth = req.header("Authorization") }
        interceptor.intercept(chain)
        assertNull(sentAuth)
    }

    @Test
    fun `rejects JWT with non-base64url characters`() {
        authTokenFlow.value = "header.payload.sig with spaces"
        var sentAuth: String? = null
        val chain = fakeChain { req -> sentAuth = req.header("Authorization") }
        interceptor.intercept(chain)
        assertNull(sentAuth)
    }

    @Test
    fun `omits header entirely when store is empty`() {
        authTokenFlow.value = null
        var sentAuth: String? = null
        val chain = fakeChain { req -> sentAuth = req.header("Authorization") }
        interceptor.intercept(chain)
        assertNull(sentAuth)
    }

    @Test
    fun `always sets Accept-Language`() {
        authTokenFlow.value = null
        var sentLang: String? = null
        val chain = fakeChain { req -> sentLang = req.header("Accept-Language") }
        interceptor.intercept(chain)
        assertEquals(java.util.Locale.getDefault().language, sentLang)
    }
}
