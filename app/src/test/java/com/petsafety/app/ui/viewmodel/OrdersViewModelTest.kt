package com.petsafety.app.ui.viewmodel

import app.cash.turbine.test
import com.petsafety.app.data.model.Order
import com.petsafety.app.data.model.PaymentIntent
import com.petsafety.app.data.network.model.CreateReplacementOrderRequest
import com.petsafety.app.data.network.model.CreateTagOrderRequest
import com.petsafety.app.data.network.model.CreateTagOrderResponse
import com.petsafety.app.data.network.model.ReplacementOrderResponse
import com.petsafety.app.data.repository.OrdersRepository
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.petsafety.app.data.network.model.AddressDetails as NetworkAddressDetails
import com.petsafety.app.data.model.AddressDetails as ModelAddressDetails

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: OrdersRepository
    private lateinit var viewModel: OrdersViewModel

    private val testNetworkAddress = NetworkAddressDetails(
        street1 = "123 Main St",
        city = "London",
        postCode = "SW1A 1AA",
        country = "UK"
    )

    private val testModelAddress = ModelAddressDetails(
        street1 = "123 Main St",
        city = "London",
        postCode = "SW1A 1AA",
        country = "UK"
    )

    private val testOrder = Order(
        id = "order-1",
        userId = "user-1",
        petName = "Buddy",
        totalAmount = 19.99,
        shippingCost = 4.99,
        shippingAddress = testModelAddress,
        paymentMethod = "card",
        paymentStatus = "paid",
        orderStatus = "processing",
        createdAt = "2024-01-01T00:00:00Z",
        updatedAt = "2024-01-01T00:00:00Z"
    )

    private val testOrder2 = Order(
        id = "order-2",
        userId = "user-1",
        petName = "Whiskers",
        totalAmount = 14.99,
        shippingCost = 4.99,
        paymentMethod = "card",
        paymentStatus = "paid",
        orderStatus = "shipped",
        createdAt = "2024-01-05T00:00:00Z",
        updatedAt = "2024-01-06T00:00:00Z"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        viewModel = OrdersViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==================== fetchOrders tests ====================

    @Test
    fun `fetchOrders - success - updates orders list`() = runTest {
        val orders = listOf(testOrder, testOrder2)
        coEvery { repository.getOrders() } returns orders

        viewModel.fetchOrders()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(orders, viewModel.orders.value)
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `fetchOrders - empty list - sets empty list`() = runTest {
        coEvery { repository.getOrders() } returns emptyList()

        viewModel.fetchOrders()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.orders.value.isEmpty())
        assertNull(viewModel.errorMessage.value)
    }

    @Test
    fun `fetchOrders - failure - sets error message`() = runTest {
        val errorMsg = "Network error"
        coEvery { repository.getOrders() } throws RuntimeException(errorMsg)

        viewModel.fetchOrders()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorMsg, viewModel.errorMessage.value)
    }

    @Test
    fun `fetchOrders - shows loading state`() = runTest {
        coEvery { repository.getOrders() } returns listOf(testOrder)

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.fetchOrders()
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== refresh tests ====================

    @Test
    fun `refresh - success - updates orders list`() = runTest {
        val orders = listOf(testOrder)
        coEvery { repository.getOrders() } returns orders

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(orders, viewModel.orders.value)
    }

    @Test
    fun `refresh - shows refreshing state`() = runTest {
        coEvery { repository.getOrders() } returns listOf(testOrder)

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
        val errorMsg = "Refresh failed"
        coEvery { repository.getOrders() } throws RuntimeException(errorMsg)

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorMsg, viewModel.errorMessage.value)
        assertFalse(viewModel.isRefreshing.value)
    }

    // ==================== createOrder tests ====================

    @Test
    fun `createOrder - success - returns response via callback`() = runTest {
        val request = CreateTagOrderRequest(
            petNames = listOf("Buddy"),
            ownerName = "John Doe",
            email = "john@example.com",
            shippingAddress = testNetworkAddress
        )
        val orderResponse = CreateTagOrderResponse(
            order = testOrder,
            userCreated = false,
            userId = "user-1",
            message = "Order created successfully"
        )
        coEvery { repository.createTagOrder(request) } returns orderResponse

        var resultResponse: CreateTagOrderResponse? = null
        var resultError: String? = null

        viewModel.createOrder(request) { response, error ->
            resultResponse = response
            resultError = error
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(orderResponse, resultResponse)
        assertNull(resultError)
    }

    @Test
    fun `createOrder - failure - returns error via callback`() = runTest {
        val request = CreateTagOrderRequest(
            petNames = listOf("Buddy"),
            ownerName = "John Doe",
            email = "john@example.com",
            shippingAddress = testNetworkAddress
        )
        val errorMsg = "Payment failed"
        coEvery { repository.createTagOrder(request) } throws RuntimeException(errorMsg)

        var resultResponse: CreateTagOrderResponse? = null
        var resultError: String? = null

        viewModel.createOrder(request) { response, error ->
            resultResponse = response
            resultError = error
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(resultResponse)
        assertEquals(errorMsg, resultError)
    }

    @Test
    fun `createOrder - shows loading state`() = runTest {
        val request = CreateTagOrderRequest(
            petNames = listOf("Buddy"),
            ownerName = "John Doe",
            email = "john@example.com",
            shippingAddress = testNetworkAddress
        )
        val orderResponse = CreateTagOrderResponse(
            order = testOrder,
            message = "Order created"
        )
        coEvery { repository.createTagOrder(request) } returns orderResponse

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.createOrder(request) { _, _ -> }
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== createReplacementOrder tests ====================

    @Test
    fun `createReplacementOrder - success - returns response via callback`() = runTest {
        val request = CreateReplacementOrderRequest(shippingAddress = testNetworkAddress)
        val response = ReplacementOrderResponse(order = testOrder, message = "Replacement order created")
        coEvery { repository.createReplacementOrder("pet-1", request) } returns response

        var result: ReplacementOrderResponse? = null
        var error: String? = null

        viewModel.createReplacementOrder("pet-1", request) { r, e ->
            result = r
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNotNull(result)
        assertNull(error)
        coVerify { repository.createReplacementOrder("pet-1", request) }
    }

    @Test
    fun `createReplacementOrder - failure - returns error via callback`() = runTest {
        val request = CreateReplacementOrderRequest(shippingAddress = testNetworkAddress)
        val errorMsg = "Not eligible for replacement"
        coEvery { repository.createReplacementOrder("pet-1", request) } throws RuntimeException(errorMsg)

        var result: ReplacementOrderResponse? = null
        var error: String? = null

        viewModel.createReplacementOrder("pet-1", request) { r, e ->
            result = r
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(result)
        assertEquals(errorMsg, error)
    }

    @Test
    fun `createReplacementOrder - shows loading state`() = runTest {
        val request = CreateReplacementOrderRequest(shippingAddress = testNetworkAddress)
        val response = ReplacementOrderResponse(order = testOrder, message = "Created")
        coEvery { repository.createReplacementOrder(any(), any()) } returns response

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.createReplacementOrder("pet-1", request) { _, _ -> }
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== createPaymentIntent tests ====================

    @Test
    fun `createPaymentIntent - success - returns true via callback`() = runTest {
        val paymentIntent = PaymentIntent(
            id = "pi_123",
            clientSecret = "pi_123_secret",
            amount = 19.99,
            currency = "gbp",
            status = "requires_payment_method"
        )
        coEvery {
            repository.createPaymentIntent("order-1", 19.99, "john@example.com", "card", "gbp")
        } returns paymentIntent

        var success = false
        var error: String? = null

        viewModel.createPaymentIntent(
            orderId = "order-1",
            amount = 19.99,
            email = "john@example.com",
            paymentMethod = "card",
            currency = "gbp"
        ) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        assertNull(error)
    }

    @Test
    fun `createPaymentIntent - with null optional params - succeeds`() = runTest {
        val paymentIntent = PaymentIntent(
            id = "pi_123",
            amount = 19.99,
            currency = "gbp"
        )
        coEvery {
            repository.createPaymentIntent("order-1", 19.99, null, null, null)
        } returns paymentIntent

        var success = false

        viewModel.createPaymentIntent(
            orderId = "order-1",
            amount = 19.99,
            email = null,
            paymentMethod = null,
            currency = null
        ) { s, _ ->
            success = s
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(success)
        coVerify { repository.createPaymentIntent("order-1", 19.99, null, null, null) }
    }

    @Test
    fun `createPaymentIntent - failure - returns error via callback`() = runTest {
        val errorMsg = "Payment service unavailable"
        coEvery {
            repository.createPaymentIntent(any(), any(), any(), any(), any())
        } throws RuntimeException(errorMsg)

        var success = false
        var error: String? = null

        viewModel.createPaymentIntent(
            orderId = "order-1",
            amount = 19.99,
            email = null,
            paymentMethod = null,
            currency = null
        ) { s, e ->
            success = s
            error = e
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(success)
        assertEquals(errorMsg, error)
    }

    // ==================== createTagCheckout tests ====================

    @Test
    fun `createTagCheckout - success - sets checkoutUrl`() = runTest {
        val expectedUrl = "https://checkout.stripe.com/c/pay/cs_test_123"
        coEvery { repository.createTagCheckout(2, "HU", null, null) } returns expectedUrl

        viewModel.createTagCheckout(quantity = 2, countryCode = "HU")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(expectedUrl, viewModel.checkoutUrl.value)
    }

    @Test
    fun `createTagCheckout - failure - calls onError`() = runTest {
        val errorMsg = "Checkout failed"
        coEvery { repository.createTagCheckout(any(), any(), any(), any()) } throws RuntimeException(errorMsg)

        var receivedError: String? = null
        viewModel.createTagCheckout(quantity = 1, countryCode = "HU") { error ->
            receivedError = error
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(errorMsg, receivedError)
        assertNull(viewModel.checkoutUrl.value)
    }

    @Test
    fun `createTagCheckout - with deliveryMethod - passes to repository`() = runTest {
        coEvery { repository.createTagCheckout(any(), any(), any(), any()) } returns "https://checkout.stripe.com/test"

        viewModel.createTagCheckout(
            quantity = 1,
            countryCode = "HU",
            deliveryMethod = "home_delivery"
        )
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.createTagCheckout(1, "HU", "home_delivery", null) }
    }

    @Test
    fun `createTagCheckout - with postapoint details - passes to repository`() = runTest {
        val postapointDetails = com.petsafety.app.data.network.model.PostaPointDetails(
            id = "pp-1",
            name = "Budapest Posta 1",
            address = "Kossuth u. 10"
        )
        coEvery { repository.createTagCheckout(any(), any(), any(), any()) } returns "https://checkout.stripe.com/test"

        viewModel.createTagCheckout(
            quantity = 1,
            countryCode = "HU",
            deliveryMethod = "postapoint",
            postapointDetails = postapointDetails
        )
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { repository.createTagCheckout(1, "HU", "postapoint", postapointDetails) }
    }

    @Test
    fun `createTagCheckout - shows loading state`() = runTest {
        coEvery { repository.createTagCheckout(any(), any(), any(), any()) } returns "https://checkout.stripe.com/test"

        viewModel.isLoading.test {
            assertEquals(false, awaitItem())

            viewModel.createTagCheckout(quantity = 1)
            assertEquals(true, awaitItem())

            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(false, awaitItem())
        }
    }

    // ==================== handleCheckout tests ====================

    @Test
    fun `handleCheckoutComplete - clears checkoutUrl and fetches orders`() = runTest {
        coEvery { repository.createTagCheckout(any(), any(), any(), any()) } returns "https://test.com"
        coEvery { repository.getOrders() } returns listOf(testOrder)

        viewModel.createTagCheckout(quantity = 1)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("https://test.com", viewModel.checkoutUrl.value)

        viewModel.handleCheckoutComplete()
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.checkoutUrl.value)
        assertEquals(listOf(testOrder), viewModel.orders.value)
    }

    @Test
    fun `handleCheckoutCancelled - clears checkoutUrl without fetching`() = runTest {
        coEvery { repository.createTagCheckout(any(), any(), any(), any()) } returns "https://test.com"

        viewModel.createTagCheckout(quantity = 1)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.handleCheckoutCancelled()

        assertNull(viewModel.checkoutUrl.value)
    }

    // ==================== createReplacementOrder with deliveryMethod tests ====================

    @Test
    fun `createReplacementOrder - with deliveryMethod - passes to repository`() = runTest {
        val request = CreateReplacementOrderRequest(
            shippingAddress = testNetworkAddress,
            deliveryMethod = "postapoint",
            postapointDetails = com.petsafety.app.data.network.model.PostaPointDetails(
                id = "pp-1",
                name = "Test Point",
                address = "Test Address"
            )
        )
        val response = ReplacementOrderResponse(order = testOrder, message = "Created")
        coEvery { repository.createReplacementOrder("pet-1", request) } returns response

        var resultResponse: ReplacementOrderResponse? = null
        viewModel.createReplacementOrder("pet-1", request) { r, _ ->
            resultResponse = r
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(response, resultResponse)
        coVerify { repository.createReplacementOrder("pet-1", request) }
    }

    @Test
    fun `createReplacementOrder - without deliveryMethod backward compat - succeeds`() = runTest {
        val request = CreateReplacementOrderRequest(shippingAddress = testNetworkAddress)
        val response = ReplacementOrderResponse(order = testOrder, message = "Created")
        coEvery { repository.createReplacementOrder("pet-1", request) } returns response

        var resultResponse: ReplacementOrderResponse? = null
        viewModel.createReplacementOrder("pet-1", request) { r, _ ->
            resultResponse = r
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(response, resultResponse)
    }

    // ==================== Initial state tests ====================

    @Test
    fun `initial state - has default values`() {
        assertTrue(viewModel.orders.value.isEmpty())
        assertFalse(viewModel.isLoading.value)
        assertFalse(viewModel.isRefreshing.value)
        assertNull(viewModel.errorMessage.value)
    }
}
