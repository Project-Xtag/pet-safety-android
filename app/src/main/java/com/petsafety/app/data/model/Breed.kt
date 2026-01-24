package com.petsafety.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Breed(
    val id: String,
    val name: String,
    val species: String,
    @SerialName("alternate_names") val alternateNames: List<String>? = null
)
