package com.petsafety.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "action_queue")
data class ActionQueueEntity(
    @PrimaryKey val id: String,
    val actionType: String,
    val actionDataJson: String,
    val createdAt: Long,
    val status: String,
    val retryCount: Int,
    val errorMessage: String?
)
