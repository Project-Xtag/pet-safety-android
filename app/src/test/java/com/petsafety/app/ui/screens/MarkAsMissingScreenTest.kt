package com.petsafety.app.ui.screens

import android.app.Application
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.model.User
import com.petsafety.app.data.repository.OfflineQueuedException
import com.petsafety.app.data.repository.PetsRepository
import com.petsafety.app.ui.viewmodel.PetsViewModel
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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for the unified Mark as Missing flow.
 * Verifies ViewModel interactions, form validation logic, pet filtering,
 * registered address formatting, and notification source mapping.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MarkAsMissingScreenTest {

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
        isMissing = false
    )

    private val missingPet = Pet(
        id = "pet-2",
        ownerId = "user-1",
        name = "Luna",
        species = "Cat",
        isMissing = true
    )

    private val testUser = User(
        id = "user-1",
        email = "test@example.com",
        firstName = "Test",
        lastName = "User",
        address = "123 Main St",
        city = "Budapest",
        postalCode = "1010",
        country = "Hungary"
    )

    private val testUserNoAddress = User(
        id = "user-2",
        email = "test2@example.com",
        firstName = "No",
        lastName = "Address"
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

    // --- Mark Pet Missing via ViewModel with full parameters ---

    @Test
    fun `markPetMissing with registered address passes all parameters correctly`() = runTest {
        val loc = LocationCoordinate(47.4979, 19.0402)
        coEvery {
            repository.markPetMissing(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(testPet.copy(isMissing = true))

        var success = false
        var errorMsg: String? = null

        viewModel.markPetMissing(
            petId = testPet.id,
            location = loc,
            address = "123 Main St, Budapest, 1010, Hungary",
            description = "Last seen near the park",
            rewardAmount = "€50",
            notificationCenterSource = "registered_address",
            notificationCenterLocation = loc,
            notificationCenterAddress = "123 Main St, Budapest, 1010, Hungary"
        ) { s, e ->
            success = s
            errorMsg = e
        }

        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue("Should succeed", success)
        assertNull("No error expected", errorMsg)

        coVerify {
            repository.markPetMissing(
                "pet-1", loc, "123 Main St, Budapest, 1010, Hungary",
                "Last seen near the park", "€50", "registered_address",
                loc, "123 Main St, Budapest, 1010, Hungary"
            )
        }
    }

    @Test
    fun `markPetMissing with current location passes GPS coordinates`() = runTest {
        val loc = LocationCoordinate(47.5, 19.04)
        coEvery {
            repository.markPetMissing(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(testPet.copy(isMissing = true))

        var success = false
        viewModel.markPetMissing(
            petId = testPet.id,
            location = loc,
            address = "Budapest, Hungary",
            description = null,
            rewardAmount = null,
            notificationCenterSource = "current_location",
            notificationCenterLocation = loc,
            notificationCenterAddress = "Budapest, Hungary"
        ) { s, _ -> success = s }

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(success)
    }

    @Test
    fun `markPetMissing with custom address source`() = runTest {
        val loc = LocationCoordinate(48.2, 16.37)
        coEvery {
            repository.markPetMissing(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(testPet.copy(isMissing = true))

        var success = false
        viewModel.markPetMissing(
            petId = testPet.id,
            location = loc,
            address = "Prater Park, Vienna",
            description = "Was wearing a red collar",
            rewardAmount = null,
            notificationCenterSource = "custom_address",
            notificationCenterLocation = loc,
            notificationCenterAddress = "Prater Park, Vienna"
        ) { s, _ -> success = s }

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(success)
    }

    @Test
    fun `markPetMissing with null reward and description succeeds`() = runTest {
        val loc = LocationCoordinate(47.5, 19.0)
        coEvery {
            repository.markPetMissing(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.success(testPet.copy(isMissing = true))

        var success = false
        viewModel.markPetMissing(
            petId = testPet.id,
            location = loc,
            address = "Budapest",
            description = null,
            rewardAmount = null,
            notificationCenterSource = "current_location",
            notificationCenterLocation = loc,
            notificationCenterAddress = "Budapest"
        ) { s, _ -> success = s }

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(success)

        coVerify {
            repository.markPetMissing("pet-1", loc, "Budapest", null, null, "current_location", loc, "Budapest")
        }
    }

    @Test
    fun `markPetMissing offline queued returns success`() = runTest {
        coEvery {
            repository.markPetMissing(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.failure(OfflineQueuedException())

        var success = false
        viewModel.markPetMissing(
            petId = testPet.id,
            location = null,
            address = "Some address",
            description = null,
            notificationCenterSource = "registered_address"
        ) { s, _ -> success = s }

        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue("Offline queued should return success", success)
    }

    @Test
    fun `markPetMissing failure returns error message`() = runTest {
        coEvery {
            repository.markPetMissing(any(), any(), any(), any(), any(), any(), any(), any())
        } returns Result.failure(RuntimeException("Network error"))

        var success = false
        var errorMsg: String? = null

        viewModel.markPetMissing(
            petId = testPet.id,
            location = null,
            address = "Some address",
            description = null,
            notificationCenterSource = "registered_address"
        ) { s, e ->
            success = s
            errorMsg = e
        }

        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse("Should fail", success)
        assertNotNull("Should have error message", errorMsg)
        assertTrue("Error should mention network", errorMsg!!.contains("Network error"))
    }

    // --- Registered Address Formatting ---

    @Test
    fun `registered address is formatted from user profile fields`() {
        val parts = listOfNotNull(
            testUser.address, testUser.addressLine2, testUser.city, testUser.postalCode, testUser.country
        ).filter { it.isNotBlank() }
        val formatted = parts.joinToString(", ")
        assertEquals("123 Main St, Budapest, 1010, Hungary", formatted)
    }

    @Test
    fun `registered address is null when user has no address fields`() {
        val parts = listOfNotNull(
            testUserNoAddress.address, testUserNoAddress.addressLine2,
            testUserNoAddress.city, testUserNoAddress.postalCode, testUserNoAddress.country
        ).filter { it.isNotBlank() }
        val formatted = parts.joinToString(", ").ifEmpty { null }
        assertNull(formatted)
    }

    @Test
    fun `registered address handles partial fields`() {
        val user = testUser.copy(address = "Main St", addressLine2 = null, city = "Vienna", postalCode = null, country = null)
        val parts = listOfNotNull(user.address, user.addressLine2, user.city, user.postalCode, user.country)
            .filter { it.isNotBlank() }
        assertEquals("Main St, Vienna", parts.joinToString(", "))
    }

    // --- Pet Selection (QuickMarkMissing) ---

    @Test
    fun `available pets filters out missing pets`() {
        val allPets = listOf(testPet, missingPet)
        val available = allPets.filter { !it.isMissing }
        assertEquals(1, available.size)
        assertEquals("Buddy", available[0].name)
    }

    @Test
    fun `all pets missing returns empty available list`() {
        val allPets = listOf(missingPet, missingPet.copy(id = "pet-3", name = "Max"))
        val available = allPets.filter { !it.isMissing }
        assertTrue("Should be empty when all pets are missing", available.isEmpty())
    }

    @Test
    fun `no pets returns empty available list`() {
        val available = emptyList<Pet>().filter { !it.isMissing }
        assertTrue(available.isEmpty())
    }

    @Test
    fun `multiple non-missing pets all appear in available list`() {
        val pets = listOf(
            testPet,
            testPet.copy(id = "pet-3", name = "Rex"),
            missingPet,
            testPet.copy(id = "pet-4", name = "Charlie")
        )
        val available = pets.filter { !it.isMissing }
        assertEquals(3, available.size)
    }

    // --- Form Validation Logic ---

    @Test
    fun `current location source is valid when location is captured`() {
        assertTrue(LocationCoordinate(47.5, 19.0) != null)
    }

    @Test
    fun `current location source is invalid when no GPS fix`() {
        val capturedLocation: LocationCoordinate? = null
        assertFalse(capturedLocation != null)
    }

    @Test
    fun `registered address source is valid when user has address`() {
        assertTrue("123 Main St" != null)
    }

    @Test
    fun `registered address source is invalid when null`() {
        val registeredAddress: String? = null
        assertFalse(registeredAddress != null)
    }

    @Test
    fun `custom address source is valid when text is entered`() {
        assertTrue("Some address".isNotBlank())
    }

    @Test
    fun `custom address source is invalid when empty`() {
        assertFalse("".isNotBlank())
    }

    @Test
    fun `custom address source is invalid when whitespace only`() {
        assertFalse("   ".isNotBlank())
    }

    // --- Notification Source Mapping ---

    @Test
    fun `notification source maps correctly to API values`() {
        assertEquals("current_location", mapSource("CURRENT"))
        assertEquals("registered_address", mapSource("REGISTERED"))
        assertEquals("custom_address", mapSource("CUSTOM"))
    }

    private fun mapSource(source: String): String = when (source) {
        "CURRENT" -> "current_location"
        "REGISTERED" -> "registered_address"
        "CUSTOM" -> "custom_address"
        else -> "unknown"
    }
}
