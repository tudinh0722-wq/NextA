package com.nexta.data.repository

import android.content.Context
import com.nexta.data.local.AlarmDao
import com.nexta.data.local.EventDao
import com.nexta.data.local.EventAlarmEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyAlarmMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventDao: EventDao,
    private val alarmDao: AlarmDao
) {
    suspend fun migrate() {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val entries = prefs.all.toMap()
        if (entries.isEmpty()) return

        entries.forEach { (eventId, raw) ->
            if (raw !is String) return@forEach
            val event = eventDao.getById(eventId) ?: return@forEach
            runCatching {
                val json = JSONObject(raw)
                alarmDao.upsert(
                    EventAlarmEntity(
                        eventId = event.id,
                        enabled = json.optBoolean("enabled", true),
                        leadTimeMinutes = json.optInt("lead", 15),
                        repeatEnabled = json.optBoolean("repeat", true),
                        repeatIntervalMinutes = json.optInt("interval", 5),
                        maxRepeats = json.optInt("max", 3),
                        acknowledged = json.optBoolean("ack", false)
                    )
                )
            }
        }

        prefs.edit().clear().apply()
    }

    companion object {
        const val PREFS_NAME = "nexta_alarms"
    }
}
