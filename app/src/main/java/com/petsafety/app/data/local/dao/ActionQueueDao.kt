package com.petsafety.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.petsafety.app.data.local.entity.ActionQueueEntity

@Dao
interface ActionQueueDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: ActionQueueEntity)

    @Query("SELECT * FROM action_queue WHERE status = 'pending' ORDER BY createdAt ASC")
    suspend fun getPending(): List<ActionQueueEntity>

    @Query("SELECT COUNT(*) FROM action_queue WHERE status = 'pending'")
    suspend fun countPending(): Int

    @Query("SELECT * FROM action_queue WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ActionQueueEntity?

    @Query("DELETE FROM action_queue WHERE id = :id")
    suspend fun deleteById(id: String)
}
