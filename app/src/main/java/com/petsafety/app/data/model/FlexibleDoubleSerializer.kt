package com.petsafety.app.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonPrimitive

/**
 * Serializer that handles Double values from PostgreSQL DECIMAL columns,
 * which may arrive as either JSON numbers (3.90) or strings ("3.90").
 */
object FlexibleDoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleDouble", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double {
        return when (decoder) {
            is JsonDecoder -> {
                val element = decoder.decodeJsonElement()
                element.jsonPrimitive.content.toDoubleOrNull() ?: 0.0
            }
            else -> decoder.decodeDouble()
        }
    }

    override fun serialize(encoder: Encoder, value: Double) {
        encoder.encodeDouble(value)
    }
}
