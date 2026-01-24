package com.petsafety.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.petsafety.app.data.local.entity.AlertEntity

@Dao
interface AlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alert: AlertEntity)

    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    suspend fun getAll(): List<AlertEntity>

    @Query("SELECT * FROM alerts WHERE petId = :petId ORDER BY createdAt DESC")
    suspend fun getByPetId(petId: String): List<AlertEntity>

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteById(id: String)
}
