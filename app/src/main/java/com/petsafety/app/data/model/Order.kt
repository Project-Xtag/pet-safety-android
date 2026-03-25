package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("pet_name") val petName: String? = null,
    @Serializable(with = FlexibleDoubleSerializer::class) @SerialName("total_amount") val totalAmount: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) @SerialName("shipping_cost") val shippingCost: Double = 0.0,
    @SerialName("shipping_address") val shippingAddress: AddressDetails? = null,
    @SerialName("billing_address") val billingAddress: AddressDetails? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    @SerialName("payment_status") val paymentStatus: String? = null,
    @SerialName("payment_intent_id") val paymentIntentId: String? = null,
    @SerialName("order_status") val orderStatus: String? = null,
    val currency: String = "eur",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val items: List<OrderItem>? = null,
    @SerialName("is_gift") val isGift: Boolean = false,
    @SerialName("order_number") val orderNumber: String? = null,
    @SerialName("mpl_tracking_number") val mplTrackingNumber: String? = null,
    @SerialName("mpl_shipment_status") val mplShipmentStatus: String? = null,
    @SerialName("delivery_method") val deliveryMethod: String? = null,
    @SerialName("shipping_payment_status") val shippingPaymentStatus: String? = null
)

@Serializable
data class OrderItem(
    val id: String,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("item_type") val itemType: String = "",
    val quantity: Int = 1,
    @Serializable(with = FlexibleDoubleSerializer::class) val price: Double = 0.0,
    @SerialName("pet_id") val petId: String? = null,
    @SerialName("pet_name") val petName: String? = null,
    @SerialName("qr_tag_id") val qrTagId: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AddressDetails(
    val street1: String? = null,
    val street2: String? = null,
    val city: String? = null,
    val province: String? = null,
    val postCode: String? = null,
    val country: String? = null,
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
