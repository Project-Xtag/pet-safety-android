package com.petsafety.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.petsafety.app.data.local.dao.AlertDao
import com.petsafety.app.data.local.dao.ActionQueueDao
import com.petsafety.app.data.local.dao.PetDao
import com.petsafety.app.data.local.dao.SuccessStoryDao
import com.petsafety.app.data.local.entity.AlertEntity
import com.petsafety.app.data.local.entity.ActionQueueEntity
import com.petsafety.app.data.local.entity.PetEntity
import com.petsafety.app.data.local.entity.SuccessStoryEntity

@Database(
    entities = [
        PetEntity::class,
        AlertEntity::class,
        ActionQueueEntity::class,
        SuccessStoryEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun alertDao(): AlertDao
    abstract fun actionQueueDao(): ActionQueueDao
    abstract fun successStoryDao(): SuccessStoryDao
}
