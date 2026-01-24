package com.petsafety.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.petsafety.app.data.local.entity.SuccessStoryEntity

@Dao
interface SuccessStoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(story: SuccessStoryEntity)

    @Query("SELECT * FROM success_stories WHERE deletedAt IS NULL AND isPublic = 1 AND isConfirmed = 1 ORDER BY foundAt DESC")
    suspend fun getAllPublic(): List<SuccessStoryEntity>

    @Query("SELECT * FROM success_stories WHERE petId = :petId AND deletedAt IS NULL ORDER BY foundAt DESC")
    suspend fun getByPetId(petId: String): List<SuccessStoryEntity>

    @Query("DELETE FROM success_stories WHERE id = :id")
    suspend fun deleteById(id: String)
}
