package com.petsafety.app.data.network

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

/**
 * M3 — pin the runtime base-URL switch behaviour. Pre-fix the Retrofit
 * client baked BuildConfig.API_BASE_URL into the baseUrl at construction,
 * so a Remote Config flip of `api_base_url` was fetched but never applied
 * — operations couldn't redirect API traffic without an app release.
 *
 * The fix keeps Retrofit pinned to the bootstrap URL but adds an OkHttp
 * interceptor that rewrites scheme/host/port at intercept time. Two
 * MockWebServers (different ports) stand in for "old" and "new" hosts;
 * we flip the StateFlow-equivalent (an AtomicReference here) between
 * requests and assert the second call lands at the new host.
 */
class ApiBaseUrlInterceptorTest {

    private lateinit var serverA: MockWebServer
    private lateinit var serverB: MockWebServer

    @Before
    fun setUp() {
        serverA = MockWebServer().also { it.start() }
        serverB = MockWebServer().also { it.start() }
    }

    @After
    fun tearDown() {
        serverA.shutdown()
        serverB.shutdown()
    }

    @Test
    fun `routes request to the host returned by the provider at intercept time`() {
        val target = AtomicReference(serverA.url("/").toString())
        val client = OkHttpClient.Builder()
            .addInterceptor(ApiBaseUrlInterceptor { target.get() })
            .build()

        serverA.enqueue(MockResponse().setBody("a"))
        // Request URL points at serverB but the interceptor must rewrite
        // it to serverA before the request leaves the OkHttp stack.
        val responseA = client.newCall(
            Request.Builder().url(serverB.url("/api/pets")).build()
        ).execute()
        responseA.close()

        assertEquals(1, serverA.requestCount)
        assertEquals(0, serverB.requestCount)
        assertEquals("/api/pets", serverA.takeRequest().path)
    }

    @Test
    fun `picks up base-URL changes between requests without rebuilding the client`() {
        val target = AtomicReference(serverA.url("/").toString())
        val client = OkHttpClient.Builder()
            .addInterceptor(ApiBaseUrlInterceptor { target.get() })
            .build()

        serverA.enqueue(MockResponse().setBody("a"))
        client.newCall(
            Request.Builder().url(serverA.url("/health")).build()
        ).execute().close()

        // Flip the live "Remote Config value" — same OkHttp client, same
        // Retrofit instance would still use the bootstrap base.
        target.set(serverB.url("/").toString())
        serverB.enqueue(MockResponse().setBody("b"))

        client.newCall(
            Request.Builder().url(serverA.url("/health")).build()
        ).execute().close()

        // First request hit serverA, second one hit serverB. Crucially
        // we never rebuilt the client between them.
        assertEquals(1, serverA.requestCount)
        assertEquals(1, serverB.requestCount)
        val rewrittenRequest = serverB.takeRequest()
        assertEquals("/health", rewrittenRequest.path)
        // Host header should be the new server's authority, not serverA's,
        // so backends that route by Host (CloudFront, ALBs, etc.) treat
        // this as legitimately addressed to serverB.
        assertEquals("${serverB.hostName}:${serverB.port}", rewrittenRequest.getHeader("Host"))
    }

    @Test
    fun `preserves request path query and method through the rewrite`() {
        val target = AtomicReference(serverA.url("/").toString())
        val client = OkHttpClient.Builder()
            .addInterceptor(ApiBaseUrlInterceptor { target.get() })
            .build()

        serverA.enqueue(MockResponse().setResponseCode(204))
        val response = client.newCall(
            Request.Builder()
                .url(serverB.url("/api/pets/123?expand=tags&locale=hu"))
                .delete()
                .build()
        ).execute()
        response.close()

        val recorded = serverA.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/api/pets/123?expand=tags&locale=hu", recorded.path)
    }

    @Test
    fun `falls through unchanged when provider returns blank`() {
        val target = AtomicReference("")
        val client = OkHttpClient.Builder()
            .addInterceptor(ApiBaseUrlInterceptor { target.get() })
            .build()

        serverA.enqueue(MockResponse().setBody("a"))
        client.newCall(
            Request.Builder().url(serverA.url("/api/pets")).build()
        ).execute().close()

        assertEquals(1, serverA.requestCount)
    }

    @Test
    fun `falls through unchanged when provider returns malformed URL`() {
        val target = AtomicReference("not://a real:url")
        val client = OkHttpClient.Builder()
            .addInterceptor(ApiBaseUrlInterceptor { target.get() })
            .build()

        // Should NOT throw — just log and proceed with the original URL.
        serverA.enqueue(MockResponse().setBody("a"))
        client.newCall(
            Request.Builder().url(serverA.url("/api/pets")).build()
        ).execute().close()

        assertEquals(1, serverA.requestCount)
        assertEquals(0, serverB.requestCount)
    }

    @Test
    fun `is a no-op when the provider URL matches the request URL`() {
        // Sanity check: when the rewrite would produce an identical URL,
        // the interceptor must not crash or duplicate the request.
        val target = AtomicReference(serverA.url("/").toString())
        val client = OkHttpClient.Builder()
            .addInterceptor(ApiBaseUrlInterceptor { target.get() })
            .build()

        serverA.enqueue(MockResponse().setBody("ok"))
        val response = client.newCall(
            Request.Builder().url(serverA.url("/api/health")).build()
        ).execute()
        response.close()

        assertEquals(1, serverA.requestCount)
        assertEquals("/api/health", serverA.takeRequest().path)
    }

    @Test
    fun `survives many requests with provider flipping every time`() {
        // Light stress: 6 round-trips, alternating servers each time.
        val target = AtomicReference(serverA.url("/").toString())
        val client = OkHttpClient.Builder()
            .addInterceptor(ApiBaseUrlInterceptor { target.get() })
            .build()

        repeat(6) { i ->
            val server = if (i % 2 == 0) serverA else serverB
            target.set(server.url("/").toString())
            server.enqueue(MockResponse().setResponseCode(200))
            client.newCall(
                Request.Builder().url(serverA.url("/api/ping")).build()
            ).execute().close()
        }

        assertEquals(3, serverA.requestCount)
        assertEquals(3, serverB.requestCount)
        // Each server saw the path verbatim — sanity-check the rewrite
        // didn't mangle it on either side.
        assertNotEquals(0, serverA.takeRequest().path?.length ?: 0)
    }
}
