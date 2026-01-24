package com.petsafety.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val petId: String,
    val userId: String,
    val status: String,
    val lastSeenLocation: String?,
    val lastSeenLatitude: Double?,
    val lastSeenLongitude: Double?,
    val additionalInfo: String?,
    val createdAt: String,
    val updatedAt: String,
    val lastSyncedAt: Long
)
