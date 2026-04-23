package com.petsafety.app.data.sync

import com.petsafety.app.data.local.OfflineDataManager
import com.petsafety.app.data.local.entity.ActionQueueEntity
import com.petsafety.app.data.network.ApiService
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

/**
 * Pass 4 audit fix regression tests for SyncService.
 *
 * Two bugs covered:
 *   1. A queued action with a missing required key (e.g. petId) used to
 *      silently complete() on the queue — the user's offline edit
 *      vanished with zero signal. We now throw so failAction() runs.
 *   2. A 4xx HTTP response (pet deleted on another device, permission
 *      revoked) used to count as a retryable failure; after 5 silent
 *      retries the action was deleted. We now fast-forward to max
 *      retries on first 4xx so the user sees the real failure.
 */
class Pass4SyncServiceTest {

    @MockK lateinit var apiService: ApiService
    @MockK lateinit var offlineDataManager: OfflineDataManager
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var service: SyncService

    @Before
    fun setup() {
        MockKAnnotations.init(this, relaxed = true)
        networkMonitor = mockk(relaxed = true)
        // Always "connected" so performFullSync reaches processQueuedActions
        val connected = MutableStateFlow(true)
        coEvery { networkMonitor.isConnected } returns connected
        coEvery { networkMonitor.refreshStatus() } returns Unit

        service = SyncService(apiService, offlineDataManager, networkMonitor)
    }

    private fun action(id: String, type: String, dataJson: String) =
        ActionQueueEntity(
            id = id,
            actionType = type,
            actionDataJson = dataJson,
            createdAt = System.currentTimeMillis(),
            status = "pending",
            retryCount = 0,
            errorMessage = null
        )

    @Test
    fun `queued UPDATE_PET without petId routes through failAction instead of completing silently`() = runTest {
        val queued = action("act-1", SyncService.ActionType.UPDATE_PET.value, """{"name":"Buddy"}""")
        coEvery { offlineDataManager.getPendingActions() } returns listOf(queued)

        service.performFullSync()

        // Must NOT complete the action (which would lose the user's edit)
        coVerify(exactly = 0) { offlineDataManager.completeAction("act-1") }
        // Must route through failAction with a descriptive message
        coVerify { offlineDataManager.failAction(eq("act-1"), match { it.contains("petId") }, any()) }
    }

    @Test
    fun `queued REPORT_SIGHTING without alertId routes through failAction`() = runTest {
        val queued = action("act-2", SyncService.ActionType.REPORT_SIGHTING.value, """{"reporterName":"Anna"}""")
        coEvery { offlineDataManager.getPendingActions() } returns listOf(queued)

        service.performFullSync()

        coVerify(exactly = 0) { offlineDataManager.completeAction("act-2") }
        coVerify { offlineDataManager.failAction(eq("act-2"), match { it.contains("alertId") }, any()) }
    }

    @Test
    fun `404 from replay fast-forwards failAction retries to max instead of silent loop`() = runTest {
        val queued = action("act-3", SyncService.ActionType.MARK_PET_FOUND.value, """{"petId":"pet-xyz"}""")
        coEvery { offlineDataManager.getPendingActions() } returns listOf(queued)
        // Simulate "pet deleted on another device" — the API returns 404
        val response404 = Response.error<Unit>(404, "not found".toResponseBody())
        coEvery { apiService.updatePet("pet-xyz", any()) } throws HttpException(response404)

        service.performFullSync()

        // Previously this would call failAction ONCE with incrementRetry=true,
        // which would keep retrying for 5 sync cycles. Now we fast-forward:
        // failAction called 5 times in a row so the action is removed next pass.
        coVerify(exactly = 5) { offlineDataManager.failAction(eq("act-3"), any(), any()) }
    }

    @Test
    fun `500 from replay uses the standard retry path (not fast-forward)`() = runTest {
        val queued = action("act-4", SyncService.ActionType.MARK_PET_FOUND.value, """{"petId":"pet-abc"}""")
        coEvery { offlineDataManager.getPendingActions() } returns listOf(queued)
        val response500 = Response.error<Unit>(500, "server error".toResponseBody())
        coEvery { apiService.updatePet("pet-abc", any()) } throws HttpException(response500)

        service.performFullSync()

        // Transient 5xx — standard single retry, not fast-forward
        coVerify(exactly = 1) { offlineDataManager.failAction(eq("act-4"), any(), any()) }
    }

    @Test
    fun `429 rate-limit uses the standard retry path (not fast-forward)`() = runTest {
        val queued = action("act-5", SyncService.ActionType.MARK_PET_FOUND.value, """{"petId":"pet-rl"}""")
        coEvery { offlineDataManager.getPendingActions() } returns listOf(queued)
        val response429 = Response.error<Unit>(429, "rate-limited".toResponseBody())
        coEvery { apiService.updatePet("pet-rl", any()) } throws HttpException(response429)

        service.performFullSync()

        coVerify(exactly = 1) { offlineDataManager.failAction(eq("act-5"), any(), any()) }
    }
}
