package com.petsafety.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pets")
data class PetEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val name: String,
    val species: String,
    val breed: String?,
    val color: String?,
    val weight: Double?,
    val microchipNumber: String?,
    val medicalNotes: String?,
    val notes: String?,
    val profileImage: String?,
    val isMissing: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val ageYears: Int?,
    val ageMonths: Int?,
    val ageText: String?,
    val ageIsApproximate: Boolean?,
    val allergies: String?,
    val medications: String?,
    val uniqueFeatures: String?,
    val sex: String?,
    val isNeutered: Boolean?,
    val qrCode: String?,
    val dateOfBirth: String?,
    val ownerName: String?,
    val ownerPhone: String?,
    val ownerEmail: String?,
    val lastSyncedAt: Long
)
