package com.petsafety.app.ui.viewmodel

import app.cash.turbine.test
import com.petsafety.app.data.model.PetPhoto
import com.petsafety.app.data.network.model.PhotoOperationResponse
import com.petsafety.app.data.network.model.PhotoReorderResponse
import com.petsafety.app.data.repository.PhotosRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
class PetPhotosViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PhotosRepository
    private lateinit var viewModel: PetPhotosViewModel

    private val testPhoto1 = PetPhoto(
        id = "photo-1",
        petId = "pet-1",
        photoUrl = "https://example.com/photo1.jpg",
        isPrimary = true,
        displayOrder = 0,
        uploadedAt = "2024-01-01T00:00:00Z"
    )

    private val testPhoto2 = PetPhoto(
        id = "photo-2",
        petId = "pet-1",
        photoUrl = "https://example.com/photo2.jpg",
        isPrimary = false,
        displayOrder = 1,
        uploadedAt = "2024-01-02T00:00:00Z"
    )

    private val testPhoto3 = PetPhoto(
        id = "photo-3",
        petId = "pet-1",
        photoUrl = "https://example.com/photo3.jpg",
        isPrimary = false,
        displayOrder = 2,
        uploadedAt = "2024-01-03T00:00:00Z"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = PetPhotosViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== loadPhotos tests ====================

    @Test
    fun `loadPhotos - success - updates photos list sorted correctly`() = runTest {
        val photos = listOf(testPhoto2, testPhoto1, testPhoto3) // Unsorted
        coEvery { repository.getPetPhotos("pet-1") } returns photos

        viewModel.loadPhotos("pet-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Should be sorted: primary first, then by displayOrder, then by uploadedAt desc
        assertEquals(3, viewModel.photos.value.size)
        assertTrue(viewModel.photos.value.first().isPrimary)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `loadPhotos - empty list - sets empty photos`() = runTest {
        coEvery { repository.getPetPhotos("pet-1") } returns emptyList()

        viewModel.loadPhotos("pet-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.photos.value.isEmpty())
    }

    @Test
    fun `loadPhotos - failure - sets error message`() = runTest {
        val errorMsg = "Failed to load photos"
        coEvery { repository.getPetPhotos("pet-1") } throws RuntimeException(errorMsg)

        viewModel.loadPhotos("pet-1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorMsg, viewModel.errorMessage.value)
    }

    @Test
    fun `loadPhotos - shows loading state`() = runTest {
        coEvery { repository.getPetPhotos(any()) } returns listOf(testPhoto1)

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.loadPhotos("pet-1")
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== uploadPhoto tests ====================

    @Test
    fun `uploadPhoto - success - adds photo to list and calls callback`() = runTest {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val newPhoto = testPhoto1.copy(id = "photo-new")
        coEvery { repository.uploadPetPhoto("pet-1", bytes, true) } returns newPhoto

        var success = false
        var error: String? = null

        viewModel.uploadPhoto("pet-1", bytes, true) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
        assertTrue(viewModel.photos.value.contains(newPhoto))
        assertEquals(1f, viewModel.uploadProgress.value)
    }

    @Test
    fun `uploadPhoto - failure - returns error and sets error message`() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        val errorMsg = "Upload failed"
        coEvery { repository.uploadPetPhoto(any(), any(), any()) } throws RuntimeException(errorMsg)

        var success = false
        var error: String? = null

        viewModel.uploadPhoto("pet-1", bytes, false) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
        assertEquals(errorMsg, viewModel.errorMessage.value)
    }

    @Test
    fun `uploadPhoto - shows uploading state`() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        coEvery { repository.uploadPetPhoto(any(), any(), any()) } returns testPhoto1

        viewModel.isUploading.test {
            assertEquals(false, awaitItem())

            viewModel.uploadPhoto("pet-1", bytes, false) { _, _ -> }
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== uploadPhotos (batch) tests ====================

    @Test
    fun `uploadPhotos - success - uploads all and reloads photos`() = runTest {
        val images = listOf(
            byteArrayOf(1, 2),
            byteArrayOf(3, 4),
            byteArrayOf(5, 6)
        )
        coEvery { repository.uploadPetPhoto(any(), any(), any()) } returns testPhoto1
        coEvery { repository.getPetPhotos("pet-1") } returns listOf(testPhoto1, testPhoto2, testPhoto3)

        var succeeded = 0
        var failed = 0

        viewModel.uploadPhotos("pet-1", images) { s, f ->
            succeeded = s
            failed = f
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(3, succeeded)
        assertEquals(0, failed)
        coVerify(exactly = 3) { repository.uploadPetPhoto(any(), any(), any()) }
        coVerify { repository.getPetPhotos("pet-1") }
    }

    @Test
    fun `uploadPhotos - partial failure - counts successes and failures`() = runTest {
        val images = listOf(
            byteArrayOf(1, 2),
            byteArrayOf(3, 4),
            byteArrayOf(5, 6)
        )
        coEvery { repository.uploadPetPhoto("pet-1", images[0], false) } returns testPhoto1
        coEvery { repository.uploadPetPhoto("pet-1", images[1], false) } throws RuntimeException("Failed")
        coEvery { repository.uploadPetPhoto("pet-1", images[2], false) } returns testPhoto3
        coEvery { repository.getPetPhotos("pet-1") } returns listOf(testPhoto1, testPhoto3)

        var succeeded = 0
        var failed = 0

        viewModel.uploadPhotos("pet-1", images) { s, f ->
            succeeded = s
            failed = f
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(2, succeeded)
        assertEquals(1, failed)
    }

    @Test
    fun `uploadPhotos - updates progress during upload`() = runTest {
        val images = listOf(
            byteArrayOf(1, 2),
            byteArrayOf(3, 4)
        )
        coEvery { repository.uploadPetPhoto(any(), any(), any()) } returns testPhoto1
        coEvery { repository.getPetPhotos(any()) } returns emptyList()

        viewModel.uploadPhotos("pet-1", images) { _, _ -> }
        testDispatcher.scheduler.advanceUntilIdle()

        // After completion, progress should be reset to 0
        assertEquals(0f, viewModel.uploadProgress.value)
    }

    // ==================== setPrimaryPhoto tests ====================

    @Test
    fun `setPrimaryPhoto - success - updates photo in list`() = runTest {
        // Setup existing photos
        coEvery { repository.getPetPhotos("pet-1") } returns listOf(testPhoto1, testPhoto2)
        viewModel.loadPhotos("pet-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Set photo2 as primary
        val response = PhotoOperationResponse(message = "Primary photo updated", photo = testPhoto2.copy(isPrimary = true))
        coEvery { repository.setPrimaryPhoto("pet-1", "photo-2") } returns response

        var success = false
        var error: String? = null

        viewModel.setPrimaryPhoto("pet-1", "photo-2") { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
        assertTrue(viewModel.photos.value.find { it.id == "photo-2" }?.isPrimary == true)
        assertTrue(viewModel.photos.value.find { it.id == "photo-1" }?.isPrimary == false)
    }

    @Test
    fun `setPrimaryPhoto - failure - returns error`() = runTest {
        val errorMsg = "Failed to set primary"
        coEvery { repository.setPrimaryPhoto(any(), any()) } throws RuntimeException(errorMsg)

        var success = false
        var error: String? = null

        viewModel.setPrimaryPhoto("pet-1", "photo-2") { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
    }

    // ==================== deletePhoto tests ====================

    @Test
    fun `deletePhoto - success - removes photo from list`() = runTest {
        // Setup existing photos
        coEvery { repository.getPetPhotos("pet-1") } returns listOf(testPhoto1, testPhoto2)
        viewModel.loadPhotos("pet-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Delete photo
        val response = PhotoOperationResponse(message = "Photo deleted")
        coEvery { repository.deletePhoto("pet-1", "photo-2") } returns response

        var success = false
        var error: String? = null

        viewModel.deletePhoto("pet-1", "photo-2") { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
        assertEquals(1, viewModel.photos.value.size)
        assertNull(viewModel.photos.value.find { it.id == "photo-2" })
    }

    @Test
    fun `deletePhoto - failure - keeps photo in list`() = runTest {
        // Setup existing photos
        coEvery { repository.getPetPhotos("pet-1") } returns listOf(testPhoto1, testPhoto2)
        viewModel.loadPhotos("pet-1")
        testDispatcher.scheduler.advanceUntilIdle()

        // Delete fails
        val errorMsg = "Delete failed"
        coEvery { repository.deletePhoto("pet-1", "photo-2") } throws RuntimeException(errorMsg)

        var success = false
        var error: String? = null

        viewModel.deletePhoto("pet-1", "photo-2") { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
        assertEquals(2, viewModel.photos.value.size)
    }

    // ==================== reorderPhotos tests ====================

    @Test
    fun `reorderPhotos - success - reloads photos`() = runTest {
        val newOrder = listOf("photo-2", "photo-1", "photo-3")
        val response = PhotoReorderResponse(message = "Photos reordered", photos = listOf(testPhoto2, testPhoto1, testPhoto3))
        coEvery { repository.reorderPhotos("pet-1", newOrder) } returns response
        coEvery { repository.getPetPhotos("pet-1") } returns listOf(testPhoto2, testPhoto1, testPhoto3)

        var success = false
        var error: String? = null

        viewModel.reorderPhotos("pet-1", newOrder) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
        coVerify { repository.reorderPhotos("pet-1", newOrder) }
        coVerify { repository.getPetPhotos("pet-1") }
    }

    @Test
    fun `reorderPhotos - failure - returns error`() = runTest {
        val newOrder = listOf("photo-2", "photo-1")
        val errorMsg = "Reorder failed"
        coEvery { repository.reorderPhotos(any(), any()) } throws RuntimeException(errorMsg)

        var success = false
        var error: String? = null

        viewModel.reorderPhotos("pet-1", newOrder) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
    }

    // ==================== Initial state tests ====================

    @Test
    fun `initial state - has default values`() {
        assertTrue(viewModel.photos.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isUploading.value)
        assertEquals(0f, viewModel.uploadProgress.value)
        assertNull(viewModel.errorMessage.value)
    }
}
