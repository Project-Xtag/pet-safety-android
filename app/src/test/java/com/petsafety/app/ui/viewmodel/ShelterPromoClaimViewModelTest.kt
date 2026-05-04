package com.petsafety.app.ui.viewmodel

import com.petsafety.app.data.model.ClaimPromoTagResponse
import com.petsafety.app.data.network.model.CreatePetRequest
import com.petsafety.app.data.repository.PetsRepository
import com.petsafety.app.data.repository.QrRepository
import com.petsafety.app.util.StringProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * M1 — pin that the shelter-promo claim flow forwards the FULL pet model
 * the user fills out on the screen, not the lean 5-field subset the
 * pre-fix code sent. Backend's claimPromoSchema accepts every CreatePet
 * field as optional, so this is purely a client-coverage gate.
 */
class ShelterPromoClaimViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var qrRepository: QrRepository
    private lateinit var petsRepository: PetsRepository
    private lateinit var stringProvider: StringProvider
    private lateinit var viewModel: ShelterPromoClaimViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        qrRepository = mockk(relaxed = true)
        petsRepository = mockk(relaxed = true)
        stringProvider = mockk(relaxed = true)
        viewModel = ShelterPromoClaimViewModel(qrRepository, petsRepository, stringProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `claimWithNewPet forwards every field of CreatePetRequest to the repository`() = runTest {
        coEvery { qrRepository.claimPromoTag(any(), any(), any()) } returns ClaimPromoTagResponse(
            success = true,
            message = "ok",
        )

        val petSlot = slot<CreatePetRequest>()
        coEvery { qrRepository.claimPromoTag(qrCode = any(), pet = capture(petSlot), petId = null) } returns
            ClaimPromoTagResponse(success = true, message = "ok")

        val request = CreatePetRequest(
            name = "Buddy",
            species = "dog",
            breed = "Golden Retriever",
            color = "golden",
            weight = 28.4,
            microchipNumber = "123456789012345",
            sex = "male",
            isNeutered = true,
            medicalNotes = "Healthy, vaccinated 2026-01-15",
            allergies = "chicken",
            medications = "none",
            uniqueFeatures = "white spot on chest",
            notes = "Friendly with kids",
            dateOfBirth = "2023-04-12",
            dobIsApproximate = false,
        )

        viewModel.claimWithNewPet("senra/ABCD1234", request)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { qrRepository.claimPromoTag(qrCode = "senra/ABCD1234", pet = any(), petId = null) }
        val captured = petSlot.captured
        assertEquals("Buddy", captured.name)
        assertEquals("dog", captured.species)
        assertEquals("Golden Retriever", captured.breed)
        assertEquals("golden", captured.color)
        assertEquals(28.4, captured.weight!!, 0.001)
        assertEquals("123456789012345", captured.microchipNumber)
        assertEquals("male", captured.sex)
        assertEquals(true, captured.isNeutered)
        assertEquals("Healthy, vaccinated 2026-01-15", captured.medicalNotes)
        assertEquals("chicken", captured.allergies)
        assertEquals("none", captured.medications)
        assertEquals("white spot on chest", captured.uniqueFeatures)
        assertEquals("Friendly with kids", captured.notes)
        assertEquals("2023-04-12", captured.dateOfBirth)
        assertEquals(false, captured.dobIsApproximate)
    }

    @Test
    fun `claimWithNewPet allows minimal payload (name and species only)`() = runTest {
        val petSlot = slot<CreatePetRequest>()
        coEvery { qrRepository.claimPromoTag(qrCode = any(), pet = capture(petSlot), petId = null) } returns
            ClaimPromoTagResponse(success = true, message = "ok")

        viewModel.claimWithNewPet(
            "senra/ABCD1234",
            CreatePetRequest(name = "Buddy", species = "cat")
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val captured = petSlot.captured
        assertEquals("Buddy", captured.name)
        assertEquals("cat", captured.species)
        // All optional fields stay null — the screen passes ifBlank { null }.
        assertNull(captured.breed)
        assertNull(captured.microchipNumber)
        assertNull(captured.medicalNotes)
        assertNull(captured.weight)
    }

    @Test
    fun `claim error surfaces in claimState`() = runTest {
        coEvery { qrRepository.claimPromoTag(any(), any(), any()) } throws RuntimeException("network down")

        viewModel.claimWithNewPet(
            "senra/ABCD1234",
            CreatePetRequest(name = "Buddy", species = "dog")
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.claimState.value
        assertTrue("expected Error state, got $state", state is ShelterPromoClaimViewModel.ClaimState.Error)
        assertEquals("network down", (state as ShelterPromoClaimViewModel.ClaimState.Error).message)
    }
}
