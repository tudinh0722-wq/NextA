package com.nexta.data.repository

import com.nexta.data.local.EventAlarmEntity
import com.nexta.data.local.EventDao
import com.nexta.data.local.EventEntity
import com.nexta.data.local.EventScheduleDao
import com.nexta.data.model.AlarmSettings
import com.nexta.data.model.Event
import com.nexta.data.model.EventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao,
    private val scheduleDao: EventScheduleDao
) {
    fun getAllEvents(): Flow<List<Event>> = eventDao.getAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun getEventById(id: String): Event? = eventDao.getById(id)?.toDomain()

    /**
     * Insert hoặc replace event + alarm trong 1 transaction.
     * Dùng cho cả tạo mới lẫn chỉnh sửa (INSERT OR REPLACE).
     */
    suspend fun upsertEvent(event: Event, alarm: AlarmSettings) =
        scheduleDao.saveEventWithAlarm(event.toEntity(), alarm.toEntity(event.id))

    suspend fun upsertEvents(events: List<Event>, alarms: List<AlarmSettings>) {
        require(events.size == alarms.size) { "Event và alarm phải có cùng số lượng." }
        scheduleDao.saveEventsWithAlarms(
            events.map { it.toEntity() },
            events.zip(alarms).map { (event, alarm) -> alarm.toEntity(event.id) }
        )
    }

    suspend fun deleteEvent(id: String) = scheduleDao.deleteEventWithAlarm(id)
}

private fun EventEntity.toDomain() = Event(
    id,
    title,
    EventType.valueOf(type),
    LocalDateTime.parse(startDateTime),
    LocalDateTime.parse(endDateTime),
    location,
    note,
    priority
)

private fun Event.toEntity() = EventEntity(
    id,
    title,
    type.name,
    startDateTime.toString(),
    endDateTime.toString(),
    location,
    note,
    priority
)

private fun AlarmSettings.toEntity(eventId: String) = EventAlarmEntity(
    eventId = eventId,
    enabled = enabled,
    leadTimeMinutes = leadTimeMinutes,
    repeatEnabled = repeatEnabled,
    repeatIntervalMinutes = repeatIntervalMinutes,
    maxRepeats = maxRepeats,
    acknowledged = false
)
