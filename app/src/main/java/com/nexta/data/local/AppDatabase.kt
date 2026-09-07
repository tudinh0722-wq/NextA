package com.nexta.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EventEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao
}
