package com.nexta.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    @Query("SELECT * FROM event_alarms WHERE eventId = :eventId")
    suspend fun getByEventId(eventId: String): EventAlarmEntity?

    @Query("SELECT * FROM event_alarms WHERE enabled = 1 AND acknowledged = 0")
    suspend fun getPending(): List<EventAlarmEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alarm: EventAlarmEntity)

    @Query("UPDATE event_alarms SET acknowledged = 1 WHERE eventId = :eventId")
    suspend fun acknowledge(eventId: String)
}
