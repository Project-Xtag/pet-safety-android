package com.petsafety.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class TagScannedEvent(
    val petId: String,
    val petName: String,
    val qrCode: String,
    val location: Location,
    val address: String? = null,
    val scannedAt: String
) {
    @Serializable
    data class Location(val lat: Double, val lng: Double)
}

@Serializable
data class SightingReportedEvent(
    val alertId: String,
    val petId: String,
    val petName: String,
    val sightingId: String,
    val location: Location,
    val address: String? = null,
    val reportedAt: String,
    val reporterName: String? = null
) {
    @Serializable
    data class Location(val lat: Double, val lng: Double)
}

@Serializable
data class PetFoundEvent(
    val petId: String,
    val petName: String,
    val alertId: String? = null,
    val foundAt: String
)

@Serializable
data class AlertCreatedEvent(
    val alertId: String,
    val petId: String,
    val petName: String,
    val location: Location,
    val address: String,
    val createdAt: String
) {
    @Serializable
    data class Location(val lat: Double, val lng: Double)
}

@Serializable
data class AlertUpdatedEvent(
    val alertId: String,
    val petId: String,
    val petName: String,
    val status: String,
    val updatedAt: String
)

@Serializable
data class ConnectionEvent(
    val userId: String,
    val connectedAt: String
)
