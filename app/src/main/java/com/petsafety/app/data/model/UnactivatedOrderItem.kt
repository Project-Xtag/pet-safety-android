package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnactivatedOrderItem(
    @SerialName("order_item_id") val orderItemId: String,
    @SerialName("pet_name") val petName: String? = null,
    @SerialName("qr_code") val qrCode: String? = null,
    @SerialName("tag_status") val tagStatus: String? = null
)
