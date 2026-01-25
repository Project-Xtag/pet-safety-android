package com.petsafety.app.data.repository

import com.petsafety.app.data.local.OfflineDataManager
import com.petsafety.app.data.model.Breed
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.ApiEnvelope
import com.petsafety.app.data.network.model.BreedsResponse
import com.petsafety.app.data.network.model.CreatePetRequest
import com.petsafety.app.data.network.model.MarkMissingResponse
import com.petsafety.app.data.network.model.PetResponse
import com.petsafety.app.data.network.model.PetsResponse
import com.petsafety.app.data.network.model.UpdatePetRequest
import com.petsafety.app.data.sync.NetworkMonitor
import com.petsafety.app.data.sync.SyncService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PetsRepositoryTest {

    private lateinit var apiService: ApiService
    private lateinit var offlineManager: OfflineDataManager
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var syncService: SyncService
    private lateinit var repository: PetsRepository

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
        isMissing = false,
        createdAt = "2024-01-02T00:00:00Z",
        updatedAt = "2024-01-02T00:00:00Z"
    )

    @Before
    fun setup() {
        apiService = mockk(relaxed = true)
        offlineManager = mockk(relaxed = true)
        networkMonitor = mockk(relaxed = true)
        syncService = mockk(relaxed = true)

        // Default: online
        every { networkMonitor.isConnected } returns MutableStateFlow(true)

        repository = PetsRepository(apiService, offlineManager, networkMonitor, syncService)
    }

    // ==================== fetchPets tests ====================

    @Test
    fun `fetchPets - online - fetches from API and caches`() = runTest {
        val pets = listOf(testPet, testPet2)
        coEvery { apiService.getPets() } returns ApiEnvelope(
            success = true,
            data = PetsResponse(pets = pets)
        )

        val result = repository.fetchPets()

        assertEquals(pets, result.first)
        assertNull(result.second)
        coVerify { offlineManager.savePets(pets) }
    }

    @Test
    fun `fetchPets - offline - returns cached data with message`() = runTest {
        val cachedPets = listOf(testPet)
        every { networkMonitor.isConnected } returns MutableStateFlow(false)
        coEvery { offlineManager.fetchPets() } returns cachedPets

        val result = repository.fetchPets()

        assertEquals(cachedPets, result.first)
        assertEquals("Showing cached data (offline)", result.second)
        coVerify(exactly = 0) { apiService.getPets() }
    }

    // ==================== createPet tests ====================

    @Test
    fun `createPet - success - creates and caches pet`() = runTest {
        val request = CreatePetRequest(name = "New Pet", species = "Dog")
        coEvery { apiService.createPet(request) } returns ApiEnvelope(
            success = true,
            data = PetResponse(pet = testPet.copy(name = "New Pet"))
        )

        val result = repository.createPet(request)

        assertEquals("New Pet", result.name)
        coVerify { offlineManager.savePet(any()) }
    }

    @Test
    fun `createPet - null data response - throws error`() = runTest {
        val request = CreatePetRequest(name = "New Pet", species = "Dog")
        coEvery { apiService.createPet(request) } returns ApiEnvelope(
            success = true,
            data = null
        )

        var exception: Exception? = null
        try {
            repository.createPet(request)
        } catch (e: Exception) {
            exception = e
        }

        assertNotNull(exception)
        assertTrue(exception?.message?.contains("Missing pet") == true)
    }

    // ==================== updatePet tests ====================

    @Test
    fun `updatePet - success - updates and caches pet`() = runTest {
        val request = UpdatePetRequest(name = "Updated Buddy")
        val updatedPet = testPet.copy(name = "Updated Buddy")
        coEvery { apiService.updatePet("pet-1", request) } returns ApiEnvelope(
            success = true,
            data = PetResponse(pet = updatedPet)
        )

        val result = repository.updatePet("pet-1", request)

        assertEquals("Updated Buddy", result.name)
        coVerify { offlineManager.savePet(updatedPet) }
    }

    // ==================== deletePet tests ====================

    @Test
    fun `deletePet - success - calls API`() = runTest {
        coEvery { apiService.deletePet("pet-1") } returns mockk()

        repository.deletePet("pet-1")

        coVerify { apiService.deletePet("pet-1") }
    }

    // ==================== markPetMissing tests ====================

    @Test
    fun `markPetMissing - online - calls API`() = runTest {
        val location = LocationCoordinate(51.5074, -0.1278)
        coEvery { apiService.markPetMissing("pet-1", any()) } returns ApiEnvelope(
            success = true,
            data = MarkMissingResponse(
                pet = testPet.copy(isMissing = true),
                message = "Pet marked as missing"
            )
        )

        val result = repository.markPetMissing("pet-1", location, "London", "Lost in park")

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()?.isMissing == true)
        coVerify { offlineManager.savePet(any()) }
    }

    @Test
    fun `markPetMissing - offline - queues action`() = runTest {
        every { networkMonitor.isConnected } returns MutableStateFlow(false)
        coEvery { syncService.queueAction(any(), any()) } returns "action-1"

        val result = repository.markPetMissing("pet-1", null, "London", "Lost in park")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OfflineQueuedException)
        coVerify { syncService.queueAction(SyncService.ActionType.MARK_PET_LOST, any()) }
    }

    @Test
    fun `markPetMissing - offline - includes location in queued data`() = runTest {
        every { networkMonitor.isConnected } returns MutableStateFlow(false)
        val location = LocationCoordinate(51.5074, -0.1278)

        repository.markPetMissing("pet-1", location, "London", "Lost")

        coVerify {
            syncService.queueAction(
                SyncService.ActionType.MARK_PET_LOST,
                match { map ->
                    map["petId"] == "pet-1" &&
                    map["latitude"] == 51.5074 &&
                    map["longitude"] == -0.1278 &&
                    map["lastSeenAddress"] == "London" &&
                    map["description"] == "Lost"
                }
            )
        }
    }

    // ==================== markPetFound tests ====================

    @Test
    fun `markPetFound - online - calls API`() = runTest {
        coEvery { apiService.updatePet("pet-1", any()) } returns ApiEnvelope(
            success = true,
            data = PetResponse(pet = testPet.copy(isMissing = false))
        )

        val result = repository.markPetFound("pet-1")

        assertTrue(result.isSuccess)
        coVerify { offlineManager.savePet(any()) }
    }

    @Test
    fun `markPetFound - offline - queues action`() = runTest {
        every { networkMonitor.isConnected } returns MutableStateFlow(false)

        val result = repository.markPetFound("pet-1")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is OfflineQueuedException)
        coVerify {
            syncService.queueAction(
                SyncService.ActionType.MARK_PET_FOUND,
                match { it["petId"] == "pet-1" }
            )
        }
    }

    // ==================== Breed tests ====================

    @Test
    fun `getDogBreeds - returns dog breeds`() = runTest {
        val breeds = listOf(
            Breed(id = "1", name = "Golden Retriever", species = "Dog"),
            Breed(id = "2", name = "Labrador", species = "Dog")
        )
        coEvery { apiService.getDogBreeds() } returns ApiEnvelope(
            success = true,
            data = BreedsResponse(breeds = breeds)
        )

        val result = repository.getDogBreeds()

        assertEquals(breeds, result)
    }

    @Test
    fun `getCatBreeds - returns cat breeds`() = runTest {
        val breeds = listOf(
            Breed(id = "1", name = "Persian", species = "Cat"),
            Breed(id = "2", name = "Siamese", species = "Cat")
        )
        coEvery { apiService.getCatBreeds() } returns ApiEnvelope(
            success = true,
            data = BreedsResponse(breeds = breeds)
        )

        val result = repository.getCatBreeds()

        assertEquals(breeds, result)
    }

    @Test
    fun `getBreedsBySpecies - dog - returns dog breeds`() = runTest {
        val breeds = listOf(Breed(id = "1", name = "Beagle", species = "Dog"))
        coEvery { apiService.getDogBreeds() } returns ApiEnvelope(
            success = true,
            data = BreedsResponse(breeds = breeds)
        )

        val result = repository.getBreedsBySpecies("dog")

        assertEquals(breeds, result)
    }

    @Test
    fun `getBreedsBySpecies - cat - returns cat breeds`() = runTest {
        val breeds = listOf(Breed(id = "1", name = "Maine Coon", species = "Cat"))
        coEvery { apiService.getCatBreeds() } returns ApiEnvelope(
            success = true,
            data = BreedsResponse(breeds = breeds)
        )

        val result = repository.getBreedsBySpecies("cat")

        assertEquals(breeds, result)
    }

    @Test
    fun `getBreedsBySpecies - unknown species - returns empty`() = runTest {
        val result = repository.getBreedsBySpecies("bird")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `getBreedsBySpecies - case insensitive`() = runTest {
        val breeds = listOf(Breed(id = "1", name = "Poodle", species = "Dog"))
        coEvery { apiService.getDogBreeds() } returns ApiEnvelope(
            success = true,
            data = BreedsResponse(breeds = breeds)
        )

        val result = repository.getBreedsBySpecies("DOG")

        assertEquals(breeds, result)
    }
}
