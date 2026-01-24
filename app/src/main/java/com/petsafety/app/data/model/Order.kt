package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("pet_name") val petName: String,
    @SerialName("total_amount") val totalAmount: Double,
    @SerialName("shipping_cost") val shippingCost: Double,
    @SerialName("shipping_address") val shippingAddress: AddressDetails? = null,
    @SerialName("billing_address") val billingAddress: AddressDetails? = null,
    @SerialName("payment_method") val paymentMethod: String,
    @SerialName("payment_status") val paymentStatus: String,
    @SerialName("payment_intent_id") val paymentIntentId: String? = null,
    @SerialName("order_status") val orderStatus: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    val items: List<OrderItem>? = null
)

@Serializable
data class OrderItem(
    val id: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("item_type") val itemType: String,
    val quantity: Int,
    val price: Double,
    @SerialName("pet_id") val petId: String? = null,
    @SerialName("qr_tag_id") val qrTagId: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class AddressDetails(
    val street1: String,
    val street2: String? = null,
    val city: String,
    val province: String? = null,
    val postCode: String,
    val country: String,
    val phone: String? = null
)

@Serializable
data class PaymentIntent(
    val id: String,
    @SerialName("client_secret") val clientSecret: String? = null,
    val amount: Double,
    val currency: String,
    val status: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null
)
