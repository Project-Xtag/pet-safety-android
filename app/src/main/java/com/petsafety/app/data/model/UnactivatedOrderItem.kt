package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A pet from a tag order that still needs setup — auto-registered
 * (name only) at order time, with no active tag yet. The setup wizard
 * completes the pet's details and activates the scanned tag for it.
 */
@Serializable
data class UnactivatedOrderItem(
    @SerialName("pet_id") val petId: String,
    @SerialName("pet_name") val petName: String
)
