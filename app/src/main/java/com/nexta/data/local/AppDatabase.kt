package com.nexta.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EventEntity::class, EventAlarmEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun alarmDao(): AlarmDao
    abstract fun eventScheduleDao(): EventScheduleDao
}
