package com.nexta.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nexta.data.model.Event
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)
    private val store = AlarmSettingsStore(context)

    fun schedule(event: Event, settings: AlarmSettings) {
        cancel(event.id)
        store.save(event.id, settings)
        if (!settings.enabled) return

        val triggerAt = event.startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - settings.leadTimeMinutes * 60_000L
        scheduleAt(event.id, triggerAt.coerceAtLeast(System.currentTimeMillis() + 1_000L), 0)
    }

    fun scheduleRepeat(eventId: String, triggerAt: Long, repeatIndex: Int) {
        scheduleAt(eventId, triggerAt, repeatIndex)
    }

    private fun scheduleAt(eventId: String, triggerAt: Long, repeatIndex: Int) {
        val intent = intentFor(eventId, repeatIndex)
        val pending = PendingIntent.getBroadcast(context, requestCode(eventId, repeatIndex), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
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
                    scheduleAt(eventId, (settings.startMillis - settings.leadTimeMinutes * 60_000L).coerceAtLeast(System.currentTimeMillis() + 1_000L), 0)
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
