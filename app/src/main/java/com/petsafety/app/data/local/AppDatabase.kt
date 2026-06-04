package com.petsafety.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.petsafety.app.data.local.dao.AlertDao
import com.petsafety.app.data.local.dao.ActionQueueDao
import com.petsafety.app.data.local.dao.PetDao
import com.petsafety.app.data.local.dao.SuccessStoryDao
import com.petsafety.app.data.local.dao.VaccinationDao
import com.petsafety.app.data.local.entity.AlertEntity
import com.petsafety.app.data.local.entity.ActionQueueEntity
import com.petsafety.app.data.local.entity.PetEntity
import com.petsafety.app.data.local.entity.SuccessStoryEntity
import com.petsafety.app.data.local.entity.VaccinationEntity

@Database(
    entities = [
        PetEntity::class,
        AlertEntity::class,
        ActionQueueEntity::class,
        SuccessStoryEntity::class,
        VaccinationEntity::class
    ],
    // 3 → 4: adds the `vaccinations` cache table.
    // 4 → 5: adds VaccinationEntity.isMandatory (the "Kötelező" flag cached on the
    // record). The DB is configured with fallbackToDestructiveMigration (see
    // AppModule), so this bump wipes the whole encrypted cache on first launch
    // post-update and every entity (pets included) re-syncs from the API.
    // ⚠ This is the bump that ALSO drops any queued offline actions (ActionQueue)
    // on first post-update launch — NOT new exposure (every destructive bump does
    // this), but noting the trigger. Acceptable for a pure cache.
    version = 5,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun alertDao(): AlertDao
    abstract fun actionQueueDao(): ActionQueueDao
    abstract fun successStoryDao(): SuccessStoryDao
    abstract fun vaccinationDao(): VaccinationDao
}
