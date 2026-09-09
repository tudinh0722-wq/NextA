package com.nexta.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nexta.data.model.Event
import java.time.Duration
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val store = AlarmSettingsStore(context)

    fun schedule(event: Event, settings: AlarmSettings) {
        cancel(event.id)
        store.save(event.id, settings)
        if (!settings.enabled) return

        val triggerAt = event.startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - settings.leadTimeMinutes * 60_000L
        val now = System.currentTimeMillis()
        val safeTrigger = triggerAt.coerceAtLeast(now + 1_000L)
        val intent = intentFor(event.id, 0)
        val pending = PendingIntent.getBroadcast(context, requestCode(event.id, 0), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTrigger, pending)
        }
    }

    fun cancel(eventId: String) {
        repeat(5) { index ->
            val pending = PendingIntent.getBroadcast(context, requestCode(eventId, index), intentFor(eventId, index), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
        store.remove(eventId)
    }

    fun acknowledge(eventId: String) {
        repeat(5) { index ->
            val pending = PendingIntent.getBroadcast(context, requestCode(eventId, index), intentFor(eventId, index), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (pending != null) {
                alarmManager.cancel(pending)
                pending.cancel()
            }
        }
        store.acknowledge(eventId)
    }

    fun rescheduleAll() {
        store.allIds().forEach { eventId ->
            store.get(eventId)?.let { settings ->
                if (settings.enabled && !store.isAcknowledged(eventId) && settings.startMillis > System.currentTimeMillis()) {
                    val event = Event(
                        eventId,
                        settings.title,
                        com.nexta.data.model.EventType.OTHER,
                        java.time.Instant.ofEpochMilli(settings.startMillis).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                        java.time.Instant.ofEpochMilli(settings.startMillis).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                        "",
                        settings.note,
                        0
                    )
                    schedule(event, settings)
                }
            }
        }
    }

    private fun intentFor(eventId: String, repeatIndex: Int): Intent = Intent(context, AlarmReceiver::class.java).apply {
        action = ACTION_ALARM
        putExtra(EXTRA_EVENT_ID, eventId)
        putExtra(EXTRA_REPEAT_INDEX, repeatIndex)
    }

    private fun requestCode(eventId: String, repeatIndex: Int): Int = eventId.hashCode() * 10 + repeatIndex

    companion object {
        const val ACTION_ALARM = "com.nexta.action.EVENT_ALARM"
        const val EXTRA_EVENT_ID = "event_id"
        const val EXTRA_REPEAT_INDEX = "repeat_index"
    }
}
