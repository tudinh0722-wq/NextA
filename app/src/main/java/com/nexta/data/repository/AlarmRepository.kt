package com.nexta.data.repository

import com.nexta.data.local.AlarmDao
import com.nexta.data.local.EventAlarmEntity
import com.nexta.data.local.EventDao
import com.nexta.data.model.AlarmSettings
import com.nexta.data.model.Event
import com.nexta.data.model.EventType
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao,
    private val eventDao: EventDao
) {
    suspend fun getAlarm(eventId: String): AlarmSettings? =
        alarmDao.getByEventId(eventId)?.toDomain()

    suspend fun saveAlarm(eventId: String, settings: AlarmSettings) {
        alarmDao.upsert(settings.toEntity(eventId))
    }

    suspend fun acknowledge(eventId: String) {
        alarmDao.acknowledge(eventId)
    }

    suspend fun getPendingAlarms(): List<AlarmRecord> = buildList {
        alarmDao.getPending().forEach { entity ->
            val event = eventDao.getById(entity.eventId)?.toDomain() ?: return@forEach
            add(AlarmRecord(event, entity.toDomain()))
        }
    }
}

data class AlarmRecord(
    val event: Event,
    val settings: AlarmSettings
)

private fun EventAlarmEntity.toDomain() = AlarmSettings(
    enabled = enabled,
    leadTimeMinutes = leadTimeMinutes,
    repeatEnabled = repeatEnabled,
    repeatIntervalMinutes = repeatIntervalMinutes,
    maxRepeats = maxRepeats,
    acknowledged = acknowledged
)

private fun AlarmSettings.toEntity(eventId: String) = EventAlarmEntity(
    eventId = eventId,
    enabled = enabled,
    leadTimeMinutes = leadTimeMinutes,
    repeatEnabled = repeatEnabled,
    repeatIntervalMinutes = repeatIntervalMinutes,
    maxRepeats = maxRepeats,
    acknowledged = acknowledged
)

private fun com.nexta.data.local.EventEntity.toDomain() = Event(
    id,
    title,
    EventType.valueOf(type),
    LocalDateTime.parse(startDateTime),
    LocalDateTime.parse(endDateTime),
    location,
    note,
    priority
)
