package com.petsafety.app.ui.viewmodel

import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.model.QrTag
import com.petsafety.app.data.model.UnactivatedOrderItem
import com.petsafety.app.data.repository.OrdersRepository
import com.petsafety.app.data.repository.PetsRepository
import com.petsafety.app.data.repository.QrRepository
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
class TagActivationViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var petsRepository: PetsRepository
    private lateinit var qrRepository: QrRepository
    private lateinit var ordersRepository: OrdersRepository
    private lateinit var viewModel: TagActivationViewModel

    private val testPet1 = Pet(
        id = "pet-1",
        ownerId = "user-1",
        name = "Buddy",
        species = "dog",
        breed = "Labrador",
        isMissing = false,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )

    private val testPet2 = Pet(
        id = "pet-2",
        ownerId = "user-1",
        name = "Luna",
        species = "cat",
        breed = "Siamese",
        isMissing = false,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )

    private val testTag = QrTag(
        id = "tag-1",
        qrCode = "QR-TEST-001",
        status = "active",
        petId = "pet-1",
        createdAt = "2026-01-01T00:00:00Z"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        petsRepository = mockk(relaxed = true)
        qrRepository = mockk(relaxed = true)
        ordersRepository = mockk(relaxed = true)
        viewModel = TagActivationViewModel(petsRepository, qrRepository, ordersRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // MARK: - Initial State

    @Test
    fun `initial state - pets is empty`() {
        assertTrue(viewModel.pets.value.isEmpty())
    }

    @Test
    fun `initial state - isLoadingPets is false`() {
        assertEquals(false, viewModel.isLoadingPets.value)
    }

    @Test
    fun `initial state - activationState is Idle`() {
        assertTrue(viewModel.activationState.value is ActivationState.Idle)
    }

    @Test
    fun `initial state - selectedPetId is null`() {
        assertNull(viewModel.selectedPetId.value)
    }

    // MARK: - fetchPets

    @Test
    fun `fetchPets - success - updates pets list`() = runTest {
        val pets = listOf(testPet1, testPet2)
        coEvery { petsRepository.fetchPets() } returns (pets to null)

        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(pets, viewModel.pets.value)
        assertEquals(false, viewModel.isLoadingPets.value)
    }

    @Test
    fun `fetchPets - sets loading state while fetching`() = runTest {
        coEvery { petsRepository.fetchPets() } coAnswers {
            // Verify loading is true during the call
            assertTrue(viewModel.isLoadingPets.value)
            listOf(testPet1) to null
        }

        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.isLoadingPets.value)
    }

    @Test
    fun `fetchPets - error - sets empty list`() = runTest {
        coEvery { petsRepository.fetchPets() } throws RuntimeException("Network error")

        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.pets.value.isEmpty())
        assertEquals(false, viewModel.isLoadingPets.value)
    }

    @Test
    fun `fetchPets - calls repository`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (emptyList<Pet>() to null)

        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { petsRepository.fetchPets() }
    }

    // MARK: - selectPet

    @Test
    fun `selectPet - updates selectedPetId`() {
        viewModel.selectPet("pet-1")
        assertEquals("pet-1", viewModel.selectedPetId.value)
    }

    @Test
    fun `selectPet - can change selection`() {
        viewModel.selectPet("pet-1")
        assertEquals("pet-1", viewModel.selectedPetId.value)

        viewModel.selectPet("pet-2")
        assertEquals("pet-2", viewModel.selectedPetId.value)
    }

    // MARK: - activateTag - Success

    @Test
    fun `activateTag - success - state transitions to Success`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1, testPet2) to null)
        coEvery { qrRepository.activateTag("QR-TEST-001", "pet-1") } returns testTag

        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectPet("pet-1")
        viewModel.activateTag("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.activationState.value
        assertTrue("Expected Success but got $state", state is ActivationState.Success)
        assertEquals("Buddy", (state as ActivationState.Success).petName)
        assertEquals(testTag, state.tag)
    }

    @Test
    fun `activateTag - calls qrRepository with correct params`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1) to null)
        coEvery { qrRepository.activateTag(any(), any()) } returns testTag

        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectPet("pet-1")
        viewModel.activateTag("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { qrRepository.activateTag("QR-TEST-001", "pet-1") }
    }

    @Test
    fun `activateTag - transitions through Loading state`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1) to null)
        coEvery { qrRepository.activateTag(any(), any()) } coAnswers {
            // During the call, state should be Loading
            assertTrue(viewModel.activationState.value is ActivationState.Loading)
            testTag
        }

        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectPet("pet-1")
        viewModel.activateTag("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.activationState.value is ActivationState.Success)
    }

    // MARK: - activateTag - Error Cases

    @Test
    fun `activateTag - no pet selected - sets Error`() = runTest {
        // Don't call selectPet
        viewModel.activateTag("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.activationState.value
        assertTrue("Expected Error but got $state", state is ActivationState.Error)
        assertEquals("Please select a pet first", (state as ActivationState.Error).message)
    }

    @Test
    fun `activateTag - repository throws - sets Error with message`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1) to null)
        coEvery { qrRepository.activateTag(any(), any()) } throws RuntimeException("Tag already activated")

        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectPet("pet-1")
        viewModel.activateTag("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.activationState.value
        assertTrue(state is ActivationState.Error)
        assertEquals("Tag already activated", (state as ActivationState.Error).message)
    }

    @Test
    fun `activateTag - exception without message - uses fallback`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1) to null)
        coEvery { qrRepository.activateTag(any(), any()) } throws RuntimeException()

        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.selectPet("pet-1")
        viewModel.activateTag("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.activationState.value
        assertTrue(state is ActivationState.Error)
        assertEquals("Failed to activate tag", (state as ActivationState.Error).message)
    }

    @Test
    fun `activateTag - pet not in list - uses empty petName`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1) to null)
        coEvery { qrRepository.activateTag(any(), any()) } returns testTag

        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()

        // Select a pet ID that isn't in the loaded pets list
        viewModel.selectPet("pet-unknown")
        viewModel.activateTag("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.activationState.value
        assertTrue(state is ActivationState.Success)
        assertEquals("", (state as ActivationState.Success).petName)
    }

    // MARK: - resetActivation

    @Test
    fun `resetActivation - resets state to Idle`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1) to null)
        coEvery { qrRepository.activateTag(any(), any()) } returns testTag

        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.selectPet("pet-1")
        viewModel.activateTag("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.activationState.value is ActivationState.Success)

        viewModel.resetActivation()
        assertTrue(viewModel.activationState.value is ActivationState.Idle)
    }

    @Test
    fun `resetActivation - resets from Error state`() {
        // Trigger error by not selecting pet
        viewModel.activateTag("QR-TEST-001")
        assertTrue(viewModel.activationState.value is ActivationState.Error)

        viewModel.resetActivation()
        assertTrue(viewModel.activationState.value is ActivationState.Idle)
    }

    // MARK: - Full Flow

    @Test
    fun `full flow - fetch pets, select, activate, reset`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1, testPet2) to null)
        coEvery { qrRepository.activateTag("QR-TEST-001", "pet-2") } returns testTag.copy(petId = "pet-2")

        // 1. Fetch pets
        viewModel.fetchPets()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, viewModel.pets.value.size)

        // 2. Select pet
        viewModel.selectPet("pet-2")
        assertEquals("pet-2", viewModel.selectedPetId.value)

        // 3. Activate tag
        viewModel.activateTag("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()
        val successState = viewModel.activationState.value as ActivationState.Success
        assertEquals("Luna", successState.petName)

        // 4. Reset
        viewModel.resetActivation()
        assertTrue(viewModel.activationState.value is ActivationState.Idle)
    }

    // MARK: - loadActivationData

    @Test
    fun `loadActivationData - fetches both pets and order items in parallel`() = runTest {
        val pets = listOf(testPet1, testPet2)
        val orderItems = listOf(
            UnactivatedOrderItem(orderItemId = "oi-1", petName = "Buddy"),
            UnactivatedOrderItem(orderItemId = "oi-2", petName = "Max")
        )
        coEvery { petsRepository.fetchPets() } returns (pets to null)
        coEvery { ordersRepository.getUnactivatedTagsForQRCode("QR-TEST-001") } returns orderItems

        viewModel.loadActivationData("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(pets, viewModel.pets.value)
        assertEquals(orderItems, viewModel.orderItems.value)
        assertEquals(false, viewModel.isLoadingPets.value)
    }

    @Test
    fun `loadActivationData - order items error - still loads pets`() = runTest {
        val pets = listOf(testPet1)
        coEvery { petsRepository.fetchPets() } returns (pets to null)
        coEvery { ordersRepository.getUnactivatedTagsForQRCode(any()) } throws RuntimeException("Not found")

        viewModel.loadActivationData("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(pets, viewModel.pets.value)
        assertTrue(viewModel.orderItems.value.isEmpty())
    }

    @Test
    fun `loadActivationData - sets loading state`() = runTest {
        coEvery { petsRepository.fetchPets() } coAnswers {
            assertTrue(viewModel.isLoadingPets.value)
            listOf(testPet1) to null
        }
        coEvery { ordersRepository.getUnactivatedTagsForQRCode(any()) } returns emptyList()

        viewModel.loadActivationData("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(false, viewModel.isLoadingPets.value)
    }

    // MARK: - Order matching helpers

    @Test
    fun `getMatchingPets - returns pets whose names match order items`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1, testPet2) to null)
        coEvery { ordersRepository.getUnactivatedTagsForQRCode(any()) } returns listOf(
            UnactivatedOrderItem(orderItemId = "oi-1", petName = "Buddy")
        )

        viewModel.loadActivationData("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        val matching = viewModel.getMatchingPets()
        assertEquals(1, matching.size)
        assertEquals("Buddy", matching[0].name)
    }

    @Test
    fun `getMatchingPets - case insensitive matching`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1) to null)
        coEvery { ordersRepository.getUnactivatedTagsForQRCode(any()) } returns listOf(
            UnactivatedOrderItem(orderItemId = "oi-1", petName = "buddy")
        )

        viewModel.loadActivationData("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        val matching = viewModel.getMatchingPets()
        assertEquals(1, matching.size)
        assertEquals("Buddy", matching[0].name)
    }

    @Test
    fun `getUnmatchedOrderNames - returns order names without pet profiles`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1) to null)
        coEvery { ordersRepository.getUnactivatedTagsForQRCode(any()) } returns listOf(
            UnactivatedOrderItem(orderItemId = "oi-1", petName = "Buddy"),
            UnactivatedOrderItem(orderItemId = "oi-2", petName = "Max")
        )

        viewModel.loadActivationData("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        val unmatched = viewModel.getUnmatchedOrderNames()
        assertEquals(1, unmatched.size)
        assertEquals("Max", unmatched[0])
    }

    @Test
    fun `getOtherPets - returns pets not in order`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1, testPet2) to null)
        coEvery { ordersRepository.getUnactivatedTagsForQRCode(any()) } returns listOf(
            UnactivatedOrderItem(orderItemId = "oi-1", petName = "Buddy")
        )

        viewModel.loadActivationData("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        val others = viewModel.getOtherPets()
        assertEquals(1, others.size)
        assertEquals("Luna", others[0].name)
    }

    @Test
    fun `hasOrderContext - true when order items present`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1) to null)
        coEvery { ordersRepository.getUnactivatedTagsForQRCode(any()) } returns listOf(
            UnactivatedOrderItem(orderItemId = "oi-1", petName = "Buddy")
        )

        viewModel.loadActivationData("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.hasOrderContext())
    }

    @Test
    fun `hasOrderContext - false when no order items`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (listOf(testPet1) to null)
        coEvery { ordersRepository.getUnactivatedTagsForQRCode(any()) } returns emptyList()

        viewModel.loadActivationData("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.hasOrderContext())
    }

    @Test
    fun `getUnmatchedOrderNames - ignores null pet names`() = runTest {
        coEvery { petsRepository.fetchPets() } returns (emptyList<Pet>() to null)
        coEvery { ordersRepository.getUnactivatedTagsForQRCode(any()) } returns listOf(
            UnactivatedOrderItem(orderItemId = "oi-1", petName = null),
            UnactivatedOrderItem(orderItemId = "oi-2", petName = "Max")
        )

        viewModel.loadActivationData("QR-TEST-001")
        testDispatcher.scheduler.advanceUntilIdle()

        val unmatched = viewModel.getUnmatchedOrderNames()
        assertEquals(1, unmatched.size)
        assertEquals("Max", unmatched[0])
    }
}
