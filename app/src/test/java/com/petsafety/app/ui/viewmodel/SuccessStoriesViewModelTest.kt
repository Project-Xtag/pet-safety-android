package com.petsafety.app.ui.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.petsafety.app.R
import com.petsafety.app.data.model.SuccessStory
import com.petsafety.app.data.network.model.CreateSuccessStoryRequest
import com.petsafety.app.data.network.model.SuccessStoriesResponse
import com.petsafety.app.data.network.model.UpdateSuccessStoryRequest
import com.petsafety.app.data.repository.SuccessStoriesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class SuccessStoriesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var repository: SuccessStoriesRepository
    private lateinit var viewModel: SuccessStoriesViewModel

    private val testStory = SuccessStory(
        id = "story-1",
        alertId = "alert-1",
        petId = "pet-1",
        ownerId = "user-1",
        reunionCity = "London",
        reunionLatitude = 51.5074,
        reunionLongitude = -0.1278,
        storyText = "We found Buddy in the park!",
        isPublic = true,
        isConfirmed = true,
        missingSince = "2024-01-01T00:00:00Z",
        foundAt = "2024-01-05T00:00:00Z",
        createdAt = "2024-01-05T00:00:00Z",
        updatedAt = "2024-01-05T00:00:00Z",
        petName = "Buddy",
        petSpecies = "Dog",
        petPhotoUrl = "https://example.com/buddy.jpg",
        distanceKm = 2.5
    )

    private val testStory2 = SuccessStory(
        id = "story-2",
        alertId = "alert-2",
        petId = "pet-2",
        ownerId = "user-2",
        reunionCity = "Manchester",
        storyText = "Whiskers came back home!",
        isPublic = true,
        isConfirmed = true,
        foundAt = "2024-01-10T00:00:00Z",
        createdAt = "2024-01-10T00:00:00Z",
        updatedAt = "2024-01-10T00:00:00Z",
        petName = "Whiskers",
        petSpecies = "Cat"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        every { application.getString(R.string.error_load_stories) } returns "Failed to load stories"
        repository = mockk(relaxed = true)
        viewModel = SuccessStoriesViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== fetchStories tests ====================

    @Test
    fun `fetchStories - success - updates stories list`() = runTest {
        val response = SuccessStoriesResponse(
            stories = listOf(testStory, testStory2),
            total = 2,
            hasMore = false,
            page = 1,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(51.5, -0.12, 100.0, 1, 10) } returns response

        viewModel.fetchStories(51.5, -0.12, 100.0, 1, loadMore = false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.stories.value.size)
        assertEquals(testStory, viewModel.stories.value[0])
        assertEquals(1, viewModel.currentPage.value)
        assertFalse(viewModel.hasMore.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `fetchStories - loadMore true - appends to existing list`() = runTest {
        // First fetch
        val response1 = SuccessStoriesResponse(
            stories = listOf(testStory),
            total = 2,
            hasMore = true,
            page = 1,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(51.5, -0.12, 100.0, 1, 10) } returns response1
        viewModel.fetchStories(51.5, -0.12, 100.0, 1, loadMore = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Load more
        val response2 = SuccessStoriesResponse(
            stories = listOf(testStory2),
            total = 2,
            hasMore = false,
            page = 2,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(51.5, -0.12, 100.0, 2, 10) } returns response2
        viewModel.fetchStories(51.5, -0.12, 100.0, 2, loadMore = true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.stories.value.size)
        assertEquals(testStory, viewModel.stories.value[0])
        assertEquals(testStory2, viewModel.stories.value[1])
        assertEquals(2, viewModel.currentPage.value)
        assertFalse(viewModel.hasMore.value)
    }

    @Test
    fun `fetchStories - failure - sets error message`() = runTest {
        val errorMsg = "Network error"
        coEvery { repository.fetchPublicStories(any(), any(), any(), any(), any()) } throws RuntimeException(errorMsg)

        viewModel.fetchStories(51.5, -0.12, 100.0, 1, loadMore = false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorMsg, viewModel.errorMessage.value)
    }

    @Test
    fun `fetchStories - failure with null message - uses default error`() = runTest {
        coEvery { repository.fetchPublicStories(any(), any(), any(), any(), any()) } throws RuntimeException()

        viewModel.fetchStories(51.5, -0.12, 100.0, 1, loadMore = false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Failed to load stories", viewModel.errorMessage.value)
    }

    @Test
    fun `fetchStories - shows loading state`() = runTest {
        val response = SuccessStoriesResponse(
            stories = listOf(testStory),
            total = 1,
            hasMore = false,
            page = 1,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(any(), any(), any(), any(), any()) } returns response

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.fetchStories(51.5, -0.12, 100.0, 1, loadMore = false)
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== refresh tests ====================

    @Test
    fun `refresh - success - replaces stories list`() = runTest {
        // Initial fetch
        val response1 = SuccessStoriesResponse(
            stories = listOf(testStory),
            total = 1,
            hasMore = false,
            page = 1,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(51.5, -0.12, 100.0, 1, 10) } returns response1
        viewModel.fetchStories(51.5, -0.12, 100.0, 1, loadMore = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Refresh returns different data
        val response2 = SuccessStoriesResponse(
            stories = listOf(testStory2),
            total = 1,
            hasMore = false,
            page = 1,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(51.5, -0.12, 100.0, 1, 10) } returns response2

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.stories.value.size)
        assertEquals(testStory2, viewModel.stories.value[0])
    }

    @Test
    fun `refresh - uses last location parameters`() = runTest {
        val response = SuccessStoriesResponse(
            stories = listOf(testStory),
            total = 1,
            hasMore = false,
            page = 1,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(40.7, -74.0, 50.0, 1, 10) } returns response

        viewModel.fetchStories(40.7, -74.0, 50.0, 1, loadMore = false)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 2) { repository.fetchPublicStories(40.7, -74.0, 50.0, 1, 10) }
    }

    @Test
    fun `refresh - shows refreshing state`() = runTest {
        val response = SuccessStoriesResponse(
            stories = listOf(testStory),
            total = 1,
            hasMore = false,
            page = 1,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(any(), any(), any(), any(), any()) } returns response

        viewModel.isRefreshing.test {
            assertEquals(false, awaitItem())

            viewModel.refresh()
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `refresh - failure - sets error message`() = runTest {
        coEvery { repository.fetchPublicStories(any(), any(), any(), any(), any()) } throws RuntimeException("Refresh failed")

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Refresh failed", viewModel.errorMessage.value)
        assertFalse(viewModel.isRefreshing.value)
    }

    // ==================== createStory tests ====================

    @Test
    fun `createStory - success - adds story to list and calls callback`() = runTest {
        val request = CreateSuccessStoryRequest(
            petId = "pet-1",
            alertId = "alert-1",
            storyText = "Found my pet!"
        )
        coEvery { repository.createStory(request) } returns testStory

        var success = false
        var error: String? = null

        viewModel.createStory(request) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
        assertTrue(viewModel.stories.value.contains(testStory))
    }

    @Test
    fun `createStory - success - prepends story to existing list`() = runTest {
        // Setup existing stories
        val response = SuccessStoriesResponse(
            stories = listOf(testStory2),
            total = 1,
            hasMore = false,
            page = 1,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(any(), any(), any(), any(), any()) } returns response
        viewModel.fetchStories(51.5, -0.12, 100.0, 1, loadMore = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Create new story
        val request = CreateSuccessStoryRequest(petId = "pet-1")
        coEvery { repository.createStory(request) } returns testStory

        viewModel.createStory(request) { _, _ -> }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, viewModel.stories.value.size)
        assertEquals(testStory, viewModel.stories.value[0]) // New story should be first
    }

    @Test
    fun `createStory - failure - returns error`() = runTest {
        val request = CreateSuccessStoryRequest(petId = "pet-1")
        val errorMsg = "Failed to create story"
        coEvery { repository.createStory(request) } throws RuntimeException(errorMsg)

        var success = false
        var error: String? = null

        viewModel.createStory(request) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
    }

    @Test
    fun `createStory - shows loading state`() = runTest {
        val request = CreateSuccessStoryRequest(petId = "pet-1")
        coEvery { repository.createStory(request) } returns testStory

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.createStory(request) { _, _ -> }
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== updateStory tests ====================

    @Test
    fun `updateStory - success - updates story in list`() = runTest {
        // Setup existing stories
        val response = SuccessStoriesResponse(
            stories = listOf(testStory),
            total = 1,
            hasMore = false,
            page = 1,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(any(), any(), any(), any(), any()) } returns response
        viewModel.fetchStories(51.5, -0.12, 100.0, 1, loadMore = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Update story
        val request = UpdateSuccessStoryRequest(storyText = "Updated story text")
        val updatedStory = testStory.copy(storyText = "Updated story text")
        coEvery { repository.updateStory("story-1", request) } returns updatedStory

        var success = false
        var error: String? = null

        viewModel.updateStory("story-1", request) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
        assertEquals("Updated story text", viewModel.stories.value.first().storyText)
    }

    @Test
    fun `updateStory - failure - returns error`() = runTest {
        val request = UpdateSuccessStoryRequest(storyText = "Updated")
        val errorMsg = "Update failed"
        coEvery { repository.updateStory(any(), any()) } throws RuntimeException(errorMsg)

        var success = false
        var error: String? = null

        viewModel.updateStory("story-1", request) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
    }

    // ==================== deleteStory tests ====================

    @Test
    fun `deleteStory - success - removes story from list`() = runTest {
        // Setup existing stories
        val response = SuccessStoriesResponse(
            stories = listOf(testStory, testStory2),
            total = 2,
            hasMore = false,
            page = 1,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(any(), any(), any(), any(), any()) } returns response
        viewModel.fetchStories(51.5, -0.12, 100.0, 1, loadMore = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Delete story
        coEvery { repository.deleteStory("story-1") } returns Unit

        var success = false
        var error: String? = null

        viewModel.deleteStory("story-1") { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
        assertEquals(1, viewModel.stories.value.size)
        assertNull(viewModel.stories.value.find { it.id == "story-1" })
    }

    @Test
    fun `deleteStory - failure - keeps story in list`() = runTest {
        // Setup existing stories
        val response = SuccessStoriesResponse(
            stories = listOf(testStory),
            total = 1,
            hasMore = false,
            page = 1,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(any(), any(), any(), any(), any()) } returns response
        viewModel.fetchStories(51.5, -0.12, 100.0, 1, loadMore = false)
        testDispatcher.scheduler.advanceUntilIdle()

        // Delete fails
        val errorMsg = "Delete failed"
        coEvery { repository.deleteStory("story-1") } throws RuntimeException(errorMsg)

        var success = false
        var error: String? = null

        viewModel.deleteStory("story-1") { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
        assertEquals(1, viewModel.stories.value.size)
    }

    // ==================== Edge case tests ====================

    @Test
    fun `fetchStories - empty response - sets empty list`() = runTest {
        val response = SuccessStoriesResponse(
            stories = emptyList(),
            total = 0,
            hasMore = false,
            page = 1,
            limit = 10
        )
        coEvery { repository.fetchPublicStories(any(), any(), any(), any(), any()) } returns response

        viewModel.fetchStories(51.5, -0.12, 100.0, 1, loadMore = false)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.stories.value.isEmpty())
        assertFalse(viewModel.hasMore.value)
    }

    @Test
    fun `initial state - has default values`() {
        assertTrue(viewModel.stories.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isRefreshing.value)
        assertNull(viewModel.errorMessage.value)
        assertFalse(viewModel.hasMore.value)
        assertEquals(1, viewModel.currentPage.value)
    }
}
