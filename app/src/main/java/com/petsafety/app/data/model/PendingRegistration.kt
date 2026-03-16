package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PendingRegistration(
    @SerialName("order_item_id") val orderItemId: String,
    @SerialName("order_id") val orderId: String,
    @SerialName("pet_name") val petName: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("order_status") val orderStatus: String,
    @SerialName("mpl_tracking_number") val mplTrackingNumber: String? = null,
    @SerialName("delivery_method") val deliveryMethod: String? = null
)
