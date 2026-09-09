package com.nexta.data.repository

import android.content.Context
import com.nexta.data.local.AlarmDao
import com.nexta.data.local.EventAlarmEntity
import com.nexta.data.local.EventDao
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LegacyAlarmMigrator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val eventDao: EventDao,
    private val alarmDao: AlarmDao
) {
    // In-memory flag: nếu đã chạy trong session này thì skip hoàn toàn,
    // không cần mở SharedPreferences nữa.
    private val doneSeen = AtomicBoolean(false)

    suspend fun migrate() {
        if (doneSeen.get()) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Nếu flag "đã migrate" đã được set từ session trước → skip
        if (prefs.getBoolean(KEY_MIGRATED, false)) {
            doneSeen.set(true)
            return
        }

        val entries = prefs.all.toMap()
        // Không có data legacy → đánh dấu done và thoát
        if (entries.isEmpty()) {
            prefs.edit().putBoolean(KEY_MIGRATED, true).apply()
            doneSeen.set(true)
            return
        }

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

        // Xóa data cũ và đánh dấu done
        prefs.edit().clear().putBoolean(KEY_MIGRATED, true).apply()
        doneSeen.set(true)
    }

    companion object {
        const val PREFS_NAME = "nexta_alarms"
        private const val KEY_MIGRATED = "legacy_migrated"
    }
}
