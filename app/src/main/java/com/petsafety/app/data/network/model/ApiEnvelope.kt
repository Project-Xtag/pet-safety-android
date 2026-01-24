package com.petsafety.app.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiEnvelope<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
    val code: String? = null,
    val details: Map<String, JsonElement>? = null
)

@Serializable
data class ErrorResponse(
    val error: String,
    val code: String? = null,
    val details: Map<String, JsonElement>? = null
)

@Serializable
data class EmptyResponse(val placeholder: String? = null)
