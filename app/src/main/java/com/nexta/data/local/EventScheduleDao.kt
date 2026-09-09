package com.nexta.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
abstract class EventScheduleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertEvent(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAlarm(alarm: EventAlarmEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAlarms(alarms: List<EventAlarmEntity>)

    @Query("DELETE FROM events WHERE id = :eventId")
    protected abstract suspend fun deleteEvent(eventId: String)

    @Transaction
    open suspend fun saveEventWithAlarm(event: EventEntity, alarm: EventAlarmEntity) {
        insertEvent(event)
        insertAlarm(alarm)
    }

    @Transaction
    open suspend fun saveEventsWithAlarms(events: List<EventEntity>, alarms: List<EventAlarmEntity>) {
        events.forEach { insertEvent(it) }
        insertAlarms(alarms)
    }

    @Transaction
    open suspend fun deleteEventWithAlarm(eventId: String) {
        deleteEvent(eventId)
    }
}
