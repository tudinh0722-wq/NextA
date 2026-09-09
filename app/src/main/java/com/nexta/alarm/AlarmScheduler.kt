package com.nexta.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nexta.data.model.AlarmSettings
import com.nexta.data.model.Event
import com.nexta.data.repository.AlarmRepository
import java.time.ZoneId

class AlarmScheduler(
    private val context: Context,
    private val alarmRepository: AlarmRepository
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(event: Event, settings: AlarmSettings) {
        cancelSlots(event.id, settings.maxRepeats)
        if (!settings.enabled) return
        val triggerAt = event.startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - settings.leadTimeMinutes * 60_000L
        scheduleAt(event.id, triggerAt.coerceAtLeast(System.currentTimeMillis() + 1_000L), 0)
    }

    fun scheduleRepeat(eventId: String, triggerAt: Long, repeatIndex: Int) {
        scheduleAt(eventId, triggerAt, repeatIndex)
    }

    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        alarmRepository.getPendingAlarms().forEach { record ->
            val startMillis = record.event.startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (startMillis > now) {
                val triggerAt = startMillis - record.settings.leadTimeMinutes * 60_000L
                scheduleAt(record.event.id, triggerAt.coerceAtLeast(now + 1_000L), 0)
            }
        }
    }

    /**
     * Cancel tất cả repeat slots dựa vào maxRepeats từ Room.
     * Slot 0 là alarm chính, slot 1..maxRepeats là các repeat.
     */
    suspend fun cancelFromRoom(eventId: String) {
        val maxRepeats = alarmRepository.getAlarm(eventId)?.maxRepeats ?: MAX_REPEATS_FALLBACK
        cancelSlots(eventId, maxRepeats)
    }

    /**
     * Cancel đồng bộ khi đã biết maxRepeats (vd: vừa save settings).
     */
    fun cancel(eventId: String, maxRepeats: Int = MAX_REPEATS_FALLBACK) {
        cancelSlots(eventId, maxRepeats)
    }

    private fun cancelSlots(eventId: String, maxRepeats: Int) {
        for (index in 0..maxRepeats) {
            val pending = PendingIntent.getBroadcast(
                context,
                requestCode(eventId, index),
                intentFor(eventId, index),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
    }

    private fun scheduleAt(eventId: String, triggerAt: Long, repeatIndex: Int) {
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(eventId, repeatIndex),
            intentFor(eventId, repeatIndex),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    private fun intentFor(eventId: String, repeatIndex: Int): Intent =
        Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_REPEAT_INDEX, repeatIndex)
        }

    /**
     * RequestCode luôn dương — hashCode() có thể âm trên một số ROM.
     * and(0x7FFFFFFF) bỏ sign bit, giữ 31 bit còn lại.
     * Nhân 100 thay vì 10 để chứa tối đa 99 repeat slot mà không overlap.
     */
    private fun requestCode(eventId: String, repeatIndex: Int): Int =
        (eventId.hashCode() and 0x7FFFFFFF) * 100 + repeatIndex

    companion object {
        const val ACTION_ALARM = "com.nexta.action.EVENT_ALARM"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_REPEAT_INDEX = "repeat_index"
        const val MAX_REPEATS_FALLBACK = 10
    }
}
