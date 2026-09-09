package com.nexta.data.repository

import com.nexta.data.local.AlarmDao
import com.nexta.data.local.EventAlarmEntity
import com.nexta.data.model.AlarmSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao
) {
    suspend fun getAlarm(eventId: String): AlarmSettings? =
        alarmDao.getByEventId(eventId)?.toDomain()

    suspend fun saveAlarm(eventId: String, settings: AlarmSettings) {
        alarmDao.upsert(settings.toEntity(eventId))
    }

    suspend fun acknowledge(eventId: String) {
        alarmDao.acknowledge(eventId)
    }

    suspend fun getPendingAlarms(): List<AlarmRecord> =
        alarmDao.getPending().map { AlarmRecord(it.eventId, it.toDomain()) }
}

data class AlarmRecord(
    val eventId: String,
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
