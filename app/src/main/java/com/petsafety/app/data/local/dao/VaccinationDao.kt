package com.petsafety.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.petsafety.app.data.local.entity.VaccinationEntity

@Dao
interface VaccinationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(vaccination: VaccinationEntity)

    @Query("SELECT * FROM vaccinations WHERE petId = :petId ORDER BY administeredAt DESC")
    suspend fun getForPet(petId: String): List<VaccinationEntity>

    @Query("DELETE FROM vaccinations WHERE petId = :petId")
    suspend fun deleteForPet(petId: String)

    @Query("DELETE FROM vaccinations")
    suspend fun deleteAll()
}
