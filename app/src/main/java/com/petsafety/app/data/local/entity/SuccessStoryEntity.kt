package com.petsafety.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "success_stories")
data class SuccessStoryEntity(
    @PrimaryKey val id: String,
    val alertId: String?,
    val petId: String,
    val ownerId: String,
    val reunionCity: String?,
    val reunionLatitude: Double?,
    val reunionLongitude: Double?,
    val storyText: String?,
    val isPublic: Boolean,
    val isConfirmed: Boolean,
    val missingSince: String?,
    val foundAt: String,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val lastSyncedAt: Long
)
