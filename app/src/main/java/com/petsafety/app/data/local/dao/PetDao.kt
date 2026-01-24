package com.petsafety.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.petsafety.app.data.local.entity.PetEntity

@Dao
interface PetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pet: PetEntity)

    @Query("SELECT * FROM pets ORDER BY name ASC")
    suspend fun getAll(): List<PetEntity>

    @Query("SELECT * FROM pets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PetEntity?

    @Query("DELETE FROM pets WHERE id = :id")
    suspend fun deleteById(id: String)
}
