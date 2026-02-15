package com.petsafety.app.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive

/** Deserializes reward_amount as String whether the API sends a string or a number. */
object FlexibleStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder ?: return decoder.decodeString()
        val element = jsonDecoder.decodeJsonElement()
        if (element is JsonPrimitive) {
            if (element.isString) {
                val s = element.content
                return s.ifBlank { null }
            }
            element.doubleOrNull?.let { d ->
                return if (d % 1.0 == 0.0) d.toLong().toString() else d.toString()
            }
        }
        return null
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value != null) encoder.encodeString(value) else encoder.encodeNull()
    }
}

@Serializable
data class MissingPetAlert(
    val id: String,
    @SerialName("pet_id") val petId: String,
    @SerialName("user_id") val userId: String,
    val status: String,
    @SerialName("last_seen_location") val lastSeenLocation: String? = null,
    @SerialName("last_seen_latitude") val lastSeenLatitude: Double? = null,
    @SerialName("last_seen_longitude") val lastSeenLongitude: Double? = null,
    @SerialName("additional_info") val additionalInfo: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    val pet: Pet? = null,
    val sightings: List<Sighting>? = null,
    @SerialName("description") val legacyDescription: String? = null,
    @SerialName("last_seen_address") val legacyLastSeenAddress: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    // Flat pet fields returned by nearby alerts endpoint
    @SerialName("pet_name") val flatPetName: String? = null,
    val species: String? = null,
    val breed: String? = null,
    val color: String? = null,
    @SerialName("profile_image") val flatProfileImage: String? = null,
    @SerialName("distance_km") val distanceKm: Double? = null,
    @SerialName("qr_code") val qrCode: String? = null,
    @Serializable(with = FlexibleStringSerializer::class)
    @SerialName("reward_amount") val rewardAmount: String? = null,
    @SerialName("alert_radius_km") val alertRadiusKm: Double? = null,
    @SerialName("found_at") val foundAt: String? = null
) {
    val resolvedLastSeenLocation: String?
        get() = legacyLastSeenAddress ?: lastSeenLocation

    val resolvedLatitude: Double?
        get() = lastSeenLatitude ?: lat

    val resolvedLongitude: Double?
        get() = lastSeenLongitude ?: lng

    val resolvedPetName: String?
        get() = pet?.name ?: flatPetName

    val resolvedPetPhoto: String?
        get() = pet?.profileImage ?: flatProfileImage

    val resolvedSpecies: String?
        get() = pet?.species ?: species
}

@Serializable
data class Sighting(
    val id: String,
    @SerialName("alert_id") val alertId: String,
    @SerialName("reporter_name") val reporterName: String? = null,
    @SerialName("reporter_phone") val reporterPhone: String? = null,
    @SerialName("reporter_email") val reporterEmail: String? = null,
    @SerialName("sighting_location") val sightingLocation: String? = null,
    @SerialName("sighting_latitude") val sightingLatitude: Double? = null,
    @SerialName("sighting_longitude") val sightingLongitude: Double? = null,
    @SerialName("sighting_notes") val sightingNotes: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("description") val legacyDescription: String? = null,
    @SerialName("sighting_address") val legacySightingAddress: String? = null,
    @SerialName("reported_at") val legacyReportedAt: String? = null
) {
    val resolvedLocation: String?
        get() = legacySightingAddress ?: sightingLocation

    val resolvedNotes: String?
        get() = sightingNotes ?: legacyDescription

    val resolvedCreatedAt: String
        get() = createdAt.ifBlank { legacyReportedAt ?: createdAt }
}

@Serializable
data class LocationCoordinate(
    val lat: Double,
    val lng: Double
)
