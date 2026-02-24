package com.petsafety.app.ui.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.petsafety.app.data.model.Breed
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.network.model.CreatePetRequest
import com.petsafety.app.data.network.model.SubscriptionLimitInfo
import com.petsafety.app.data.network.model.UpdatePetRequest
import com.petsafety.app.data.repository.OfflineQueuedException
import com.petsafety.app.data.repository.PetLimitExceededException
import com.petsafety.app.data.repository.PetsRepository
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
class PetsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var repository: PetsRepository
    private lateinit var viewModel: PetsViewModel

    private val testPet = Pet(
        id = "pet-1",
        ownerId = "user-1",
        name = "Buddy",
        species = "Dog",
        breed = "Golden Retriever",
        color = "Golden",
        isMissing = false,
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z"
    )

    private val testPet2 = Pet(
        id = "pet-2",
        ownerId = "user-1",
        name = "Whiskers",
        species = "Cat",
        breed = "Persian",
        color = "White",
        isMissing = false,
        createdAt = "2024-01-02T00:00:00Z",
        updatedAt = "2024-01-02T00:00:00Z"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        viewModel = PetsViewModel(application, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== fetchPets tests ====================

    @Test
    fun `fetchPets - success - updates pets list`() = runTest {
        // Given
        val pets = listOf(testPet, testPet2)
        coEvery { repository.fetchPets() } returns (pets to null)

        // When
        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(pets, viewModel.pets.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `fetchPets - with error message - sets error`() = runTest {
        // Given
        val pets = listOf(testPet)
        val errorMsg = "Showing cached data (offline)"
        coEvery { repository.fetchPets() } returns (pets to errorMsg)

        // When
        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        assertEquals(pets, viewModel.pets.value)
        assertEquals(errorMsg, viewModel.errorMessage.value)
    }

    @Test
    fun `fetchPets - shows loading state during fetch`() = runTest {
        coEvery { repository.fetchPets() } returns (listOf(testPet) to null)

        viewModel.isLoading.test {
            assertEquals(false, awaitItem()) // Initial state

            viewModel.fetchPets()
            assertEquals(true, awaitItem()) // Loading started

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem()) // Loading finished
        }
    }

    // ==================== refresh tests ====================

    @Test
    fun `refresh - updates pets and shows refreshing state`() = runTest {
        val pets = listOf(testPet)
        coEvery { repository.fetchPets() } returns (pets to null)

        viewModel.isRefreshing.test {
            assertEquals(false, awaitItem()) // Initial state

            viewModel.refresh()
            assertEquals(true, awaitItem()) // Refreshing started

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem()) // Refreshing finished
        }

        assertEquals(pets, viewModel.pets.value)
    }

    // ==================== fetchBreeds tests ====================

    @Test
    fun `fetchBreeds - dog - fetches dog breeds`() = runTest {
        val breeds = listOf(
            Breed(id = "1", name = "Golden Retriever", species = "Dog"),
            Breed(id = "2", name = "Labrador", species = "Dog")
        )
        coEvery { repository.getBreedsBySpecies("Dog") } returns breeds

        viewModel.fetchBreeds("Dog")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(breeds, viewModel.breeds.value)
        coVerify { repository.getBreedsBySpecies("Dog") }
    }

    @Test
    fun `fetchBreeds - cat - fetches cat breeds`() = runTest {
        val breeds = listOf(
            Breed(id = "1", name = "Persian", species = "Cat"),
            Breed(id = "2", name = "Siamese", species = "Cat")
        )
        coEvery { repository.getBreedsBySpecies("Cat") } returns breeds

        viewModel.fetchBreeds("Cat")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(breeds, viewModel.breeds.value)
    }

    @Test
    fun `fetchBreeds - error - sets empty list`() = runTest {
        coEvery { repository.getBreedsBySpecies(any()) } throws RuntimeException("Network error")

        viewModel.fetchBreeds("Dog")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(emptyList<Breed>(), viewModel.breeds.value)
    }

    // ==================== createPet tests ====================

    @Test
    fun `createPet - success - adds pet to list and calls callback`() = runTest {
        val request = CreatePetRequest(name = "New Pet", species = "Dog")
        val createdPet = testPet.copy(id = "pet-new", name = "New Pet")
        coEvery { repository.createPet(request) } returns createdPet

        var resultPet: Pet? = null
        var resultError: String? = null

        viewModel.createPet(request) { pet, error ->
            resultPet = pet
            resultError = error
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(createdPet, resultPet)
        assertNull(resultError)
        assertTrue(viewModel.pets.value.contains(createdPet))
    }

    @Test
    fun `createPet - failure - calls callback with error`() = runTest {
        val request = CreatePetRequest(name = "New Pet", species = "Dog")
        val errorMsg = "Failed to create pet"
        coEvery { repository.createPet(request) } throws RuntimeException(errorMsg)

        var resultPet: Pet? = null
        var resultError: String? = null

        viewModel.createPet(request) { pet, error ->
            resultPet = pet
            resultError = error
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(resultPet)
        assertEquals(errorMsg, resultError)
    }

    // ==================== updatePet tests ====================

    @Test
    fun `updatePet - success - updates pet in list`() = runTest {
        // Setup initial pets
        coEvery { repository.fetchPets() } returns (listOf(testPet) to null)
        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        val request = UpdatePetRequest(name = "Updated Buddy")
        val updatedPet = testPet.copy(name = "Updated Buddy")
        coEvery { repository.updatePet("pet-1", request) } returns updatedPet

        var success = false
        var error: String? = null

        viewModel.updatePet("pet-1", request) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
        assertEquals("Updated Buddy", viewModel.pets.value.find { it.id == "pet-1" }?.name)
    }

    @Test
    fun `updatePet - failure - calls callback with error`() = runTest {
        val request = UpdatePetRequest(name = "Updated")
        val errorMsg = "Update failed"
        coEvery { repository.updatePet(any(), any()) } throws RuntimeException(errorMsg)

        var success = false
        var error: String? = null

        viewModel.updatePet("pet-1", request) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
    }

    // ==================== deletePet tests ====================

    @Test
    fun `deletePet - success - removes pet from list`() = runTest {
        // Setup initial pets
        coEvery { repository.fetchPets() } returns (listOf(testPet, testPet2) to null)
        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.deletePet("pet-1") } returns Unit

        var success = false

        viewModel.deletePet("pet-1") { s, _ -> success = s }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertEquals(1, viewModel.pets.value.size)
        assertNull(viewModel.pets.value.find { it.id == "pet-1" })
    }

    @Test
    fun `deletePet - failure - keeps pet in list`() = runTest {
        // Setup initial pets
        coEvery { repository.fetchPets() } returns (listOf(testPet) to null)
        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.deletePet("pet-1") } throws RuntimeException("Delete failed")

        var success = false
        var error: String? = null

        viewModel.deletePet("pet-1") { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals("Delete failed", error)
        assertEquals(1, viewModel.pets.value.size)
    }

    // ==================== uploadPhoto tests ====================

    @Test
    fun `uploadPhoto - success - updates pet with new image`() = runTest {
        // Setup initial pets
        coEvery { repository.fetchPets() } returns (listOf(testPet) to null)
        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedPet = Pet(
            id = "pet-1",
            ownerId = "user-1",
            name = "Buddy",
            species = "Dog",
            breed = "Golden Retriever",
            color = "Golden",
            isMissing = false,
            createdAt = "2024-01-01T00:00:00Z",
            updatedAt = "2024-01-01T00:00:00Z"
        )
        coEvery { repository.uploadProfilePhoto("pet-1", any()) } returns updatedPet

        var success = false

        viewModel.uploadPhoto("pet-1", byteArrayOf(1, 2, 3)) { s, _ -> success = s }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        coVerify { repository.uploadProfilePhoto("pet-1", any()) }
    }

    // ==================== markPetMissing tests ====================

    @Test
    fun `markPetMissing - success - calls callback with success`() = runTest {
        val location = LocationCoordinate(51.5074, -0.1278)
        coEvery { repository.markPetMissing("pet-1", location, "London", "Last seen in park") } returns Result.success(testPet.copy(isMissing = true))

        var success = false
        var error: String? = null

        viewModel.markPetMissing("pet-1", location, "London", "Last seen in park") { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
    }

    @Test
    fun `markPetMissing - offline queued - still returns success`() = runTest {
        coEvery { repository.markPetMissing(any(), any(), any(), any()) } returns Result.failure(OfflineQueuedException())

        var success = false

        viewModel.markPetMissing("pet-1", null, "London", null) { s, _ -> success = s }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success) // Offline queued actions are treated as success
    }

    @Test
    fun `markPetMissing - failure - returns error`() = runTest {
        coEvery { repository.markPetMissing(any(), any(), any(), any()) } returns Result.failure(RuntimeException("Network error"))

        var success = false
        var error: String? = null

        viewModel.markPetMissing("pet-1", null, "London", null) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals("Network error", error)
    }

    // ==================== markPetFound tests ====================

    @Test
    fun `markPetFound - success - calls callback with success`() = runTest {
        coEvery { repository.markPetFound("pet-1") } returns Result.success(testPet.copy(isMissing = false))

        var success = false

        viewModel.markPetFound("pet-1") { s, _ -> success = s }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
    }

    @Test
    fun `markPetFound - offline queued - still returns success`() = runTest {
        coEvery { repository.markPetFound(any()) } returns Result.failure(OfflineQueuedException())

        var success = false

        viewModel.markPetFound("pet-1") { s, _ -> success = s }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
    }

    @Test
    fun `markPetFound - failure - returns error`() = runTest {
        coEvery { repository.markPetFound(any()) } returns Result.failure(RuntimeException("Failed"))

        var success = false
        var error: String? = null

        viewModel.markPetFound("pet-1") { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals("Failed", error)
    }

    // ==================== Pet limit exceeded tests ====================

    @Test
    fun `createPet - pet limit exceeded - emits upgrade prompt`() = runTest {
        val request = CreatePetRequest(name = "Second Pet", species = "Cat")
        val limitInfo = SubscriptionLimitInfo(
            currentPlan = "standard",
            currentPetCount = 1,
            maxPets = 1,
            upgradeTo = "ultimate",
            upgradePrice = "€6.95/month"
        )
        coEvery { repository.createPet(request) } throws PetLimitExceededException(limitInfo)

        var resultPet: Pet? = null
        var resultError: String? = null

        viewModel.createPet(request) { pet, error ->
            resultPet = pet
            resultError = error
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Should not return generic error — dialog handles it
        assertNull(resultPet)
        assertNull(resultError)

        // Should show upgrade prompt with correct info
        val promptInfo = viewModel.showUpgradePrompt.value
        assertEquals("standard", promptInfo?.currentPlan)
        assertEquals("ultimate", promptInfo?.upgradeTo)
        assertEquals("€6.95/month", promptInfo?.upgradePrice)
        assertEquals(1, promptInfo?.maxPets)
    }

    @Test
    fun `createPet - pet limit exceeded - contains upgrade info`() = runTest {
        val request = CreatePetRequest(name = "Third Pet", species = "Dog")
        val limitInfo = SubscriptionLimitInfo(
            currentPlan = "standard",
            currentPetCount = 1,
            maxPets = 1,
            upgradeTo = "ultimate",
            upgradePrice = "€6.95/month"
        )
        coEvery { repository.createPet(request) } throws PetLimitExceededException(limitInfo)

        viewModel.createPet(request) { _, _ -> }
        testDispatcher.scheduler.advanceUntilIdle()

        val info = viewModel.showUpgradePrompt.value
        assertEquals(limitInfo, info)

        // Dismiss should clear the prompt
        viewModel.dismissUpgradePrompt()
        assertNull(viewModel.showUpgradePrompt.value)
    }
}
