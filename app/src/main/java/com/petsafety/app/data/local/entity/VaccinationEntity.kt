package com.petsafety.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room cache row for a vaccination record. Offline-first reads come from here
 * when the device is offline; the encrypted cache is wiped on logout via
 * `OfflineDataManager.clearAllData()`. Date fields stay as ISO `String`s — the
 * same wire form as the model — so no conversion logic lives in the cache.
 */
@Entity(tableName = "vaccinations")
data class VaccinationEntity(
    @PrimaryKey val id: String,
    val petId: String,
    val vaccineCode: String,
    val vaccineNameSnapshot: String,
    val administeredAt: String,
    val expiresAt: String?,
    val batchNumber: String?,
    val vetName: String?,
    val vetClinic: String?,
    val certificateUrl: String?,
    val certificateMime: String?,
    val notes: String?,
    val createdAt: String?,
    val lastSyncedAt: Long
)
