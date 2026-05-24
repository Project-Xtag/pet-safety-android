package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A pet from a tag order that still needs setup. Post the 2026-05-24
 * auto-create revert, the pet row does NOT yet exist — petId will be
 * null and the wizard's atomic /qr-tags/activate call (with a petData
 * payload) creates the pet AND activates the tag in one shot. Older
 * placeholder pets that survived the cleanup migration (because the
 * user touched them) still return a non-null petId; the wizard falls
 * back to update-then-activate for those.
 */
@Serializable
data class UnactivatedOrderItem(
    @SerialName("pet_id") val petId: String? = null,
    @SerialName("pet_name") val petName: String
)
