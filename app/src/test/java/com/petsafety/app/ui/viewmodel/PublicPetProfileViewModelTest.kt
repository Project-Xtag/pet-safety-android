package com.petsafety.app.ui.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.petsafety.app.R
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.model.TagLookupResponse
import com.petsafety.app.data.repository.QrRepository
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
class PublicPetProfileViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var qrRepository: QrRepository
    private lateinit var viewModel: PublicPetProfileViewModel

    private val testPet = Pet(
        id = "pet-1",
        ownerId = "user-1",
        name = "Buddy",
        species = "Dog",
        breed = "Golden Retriever",
        color = "Golden",
        isMissing = false,
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z",
        ownerName = "John Doe",
        ownerPhone = "+1234567890",
        ownerEmail = "john@example.com"
    )

    private val missingPet = testPet.copy(
        id = "pet-2",
        name = "Whiskers",
        species = "Cat",
        isMissing = true
    )

    private fun lookupWithPet(pet: Pet) = TagLookupResponse(
        exists = true,
        status = "active",
        hasPet = true,
        isOwner = false,
        canActivate = false,
        pet = pet
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        every { application.getString(R.string.error_load_pet_profile) } returns "Failed to load pet profile"
        qrRepository = mockk(relaxed = true)
        viewModel = PublicPetProfileViewModel(application, qrRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== loadPublicProfile tests ====================

    @Test
    fun `loadPublicProfile - success - updates pet state`() = runTest {
        coEvery { qrRepository.lookupTag("ABC123") } returns lookupWithPet(testPet)

        viewModel.loadPublicProfile("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(testPet, viewModel.pet.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `loadPublicProfile - success with missing pet - updates pet state`() = runTest {
        coEvery { qrRepository.lookupTag("LOST123") } returns lookupWithPet(missingPet)

        viewModel.loadPublicProfile("LOST123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(missingPet, viewModel.pet.value)
        assertTrue(viewModel.pet.value?.isMissing == true)
    }

    @Test
    fun `loadPublicProfile - failure - sets error message`() = runTest {
        val errorMsg = "QR code not found"
        coEvery { qrRepository.lookupTag("INVALID") } throws RuntimeException(errorMsg)

        viewModel.loadPublicProfile("INVALID")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.pet.value)
        assertEquals(errorMsg, viewModel.errorMessage.value)
    }

    @Test
    fun `loadPublicProfile - failure with null message - uses default error`() = runTest {
        coEvery { qrRepository.lookupTag("INVALID") } throws RuntimeException()

        viewModel.loadPublicProfile("INVALID")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Failed to load pet profile", viewModel.errorMessage.value)
    }

    @Test
    fun `loadPublicProfile - shows loading state`() = runTest {
        coEvery { qrRepository.lookupTag(any()) } returns lookupWithPet(testPet)

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.loadPublicProfile("ABC123")
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    @Test
    fun `loadPublicProfile - clears previous error before loading`() = runTest {
        // First, trigger an error
        coEvery { qrRepository.lookupTag("INVALID") } throws RuntimeException("Error")
        viewModel.loadPublicProfile("INVALID")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("Error", viewModel.errorMessage.value)

        // Now load successfully
        coEvery { qrRepository.lookupTag("ABC123") } returns lookupWithPet(testPet)
        viewModel.loadPublicProfile("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.errorMessage.value)
        assertEquals(testPet, viewModel.pet.value)
    }

    @Test
    fun `loadPublicProfile - calls lookupTag then fire-and-forget scanQr`() = runTest {
        coEvery { qrRepository.lookupTag("MYCODE123") } returns lookupWithPet(testPet)

        viewModel.loadPublicProfile("MYCODE123")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { qrRepository.lookupTag("MYCODE123") }
        coVerify { qrRepository.scanQr("MYCODE123") }
    }

    @Test
    fun `loadPublicProfile - scanQr failure does not affect pet state`() = runTest {
        coEvery { qrRepository.lookupTag("ABC123") } returns lookupWithPet(testPet)
        coEvery { qrRepository.scanQr("ABC123") } throws RuntimeException("Scan failed")

        viewModel.loadPublicProfile("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        // Pet should still be loaded from lookup
        assertEquals(testPet, viewModel.pet.value)
        assertNull(viewModel.errorMessage.value)
    }

    // ==================== Pet details tests ====================

    @Test
    fun `loadPublicProfile - pet with all details - available in state`() = runTest {
        val detailedPet = testPet.copy(
            breed = "Labrador",
            color = "Black",
            medicalNotes = "Allergic to chicken",
            uniqueFeatures = "Has a white spot on chest",
            microchipNumber = "123456789",
            sex = "Male",
            isNeutered = true,
            ageYears = 3,
            ageMonths = 6
        )
        coEvery { qrRepository.lookupTag("ABC123") } returns lookupWithPet(detailedPet)

        viewModel.loadPublicProfile("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        val pet = viewModel.pet.value
        assertEquals("Labrador", pet?.breed)
        assertEquals("Black", pet?.color)
        assertEquals("Allergic to chicken", pet?.medicalNotes)
        assertEquals("Has a white spot on chest", pet?.uniqueFeatures)
        assertEquals("123456789", pet?.microchipNumber)
        assertEquals("Male", pet?.sex)
        assertEquals(true, pet?.isNeutered)
    }

    @Test
    fun `loadPublicProfile - pet with owner contact info - available in state`() = runTest {
        coEvery { qrRepository.lookupTag("ABC123") } returns lookupWithPet(testPet)

        viewModel.loadPublicProfile("ABC123")
        testDispatcher.scheduler.advanceUntilIdle()

        val pet = viewModel.pet.value
        assertEquals("John Doe", pet?.ownerName)
        assertEquals("+1234567890", pet?.ownerPhone)
        assertEquals("john@example.com", pet?.ownerEmail)
    }

    // ==================== Initial state tests ====================

    @Test
    fun `initial state - has default values`() {
        assertNull(viewModel.pet.value)
        assertFalse(viewModel.isLoading.value)
        assertNull(viewModel.errorMessage.value)
    }

    // ==================== Edge cases ====================

    @Test
    fun `loadPublicProfile - with empty qr code - calls repository`() = runTest {
        coEvery { qrRepository.lookupTag("") } throws RuntimeException("Invalid code")

        viewModel.loadPublicProfile("")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { qrRepository.lookupTag("") }
        assertEquals("Invalid code", viewModel.errorMessage.value)
    }

    @Test
    fun `loadPublicProfile - concurrent calls - both complete`() = runTest {
        coEvery { qrRepository.lookupTag("CODE1") } returns lookupWithPet(testPet)
        coEvery { qrRepository.lookupTag("CODE2") } returns lookupWithPet(missingPet)

        viewModel.loadPublicProfile("CODE1")
        viewModel.loadPublicProfile("CODE2")

        testDispatcher.scheduler.advanceUntilIdle()

        // The second call should override the first
        assertEquals(missingPet, viewModel.pet.value)
    }
}
