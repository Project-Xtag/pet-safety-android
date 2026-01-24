package com.petsafety.app.data.repository

import com.petsafety.app.data.model.Order
import com.petsafety.app.data.model.PaymentIntent
import com.petsafety.app.data.network.ApiService
import com.petsafety.app.data.network.model.CreatePaymentIntentRequest
import com.petsafety.app.data.network.model.CreateTagOrderRequest
import com.petsafety.app.data.network.model.CreateReplacementOrderRequest

class OrdersRepository(private val apiService: ApiService) {
    suspend fun getOrders(): List<Order> =
        apiService.getOrders().data?.orders ?: emptyList()

    suspend fun createTagOrder(request: CreateTagOrderRequest) =
        apiService.createTagOrder(request).data ?: error("Missing order response")

    suspend fun createReplacementOrder(petId: String, request: CreateReplacementOrderRequest) =
        apiService.createReplacementOrder(petId, request).data ?: error("Missing replacement response")

    suspend fun createPaymentIntent(
        orderId: String,
        amount: Double,
        email: String?,
        paymentMethod: String?,
        currency: String?
    ): PaymentIntent {
        val response = apiService.createPaymentIntent(
            CreatePaymentIntentRequest(
                orderId = orderId,
                amount = amount,
                email = email,
                paymentMethod = paymentMethod,
                currency = currency
            )
        )
        return response.data?.paymentIntent ?: error("Missing payment intent")
    }
}
