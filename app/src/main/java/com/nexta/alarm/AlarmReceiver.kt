package com.nexta.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nexta.BuildConfig
import com.nexta.MainActivity
import com.nexta.R
import com.nexta.data.repository.EventRepository
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: EventRepository

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_ALARM -> handleAlarm(context, intent)
            AlarmScheduler.ACTION_TEST_CLEANUP -> handleTestCleanup(context, intent)
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.TIME_SET",
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> AlarmScheduler(context).rescheduleAll()
        }
    }

    private fun handleTestCleanup(context: Context, intent: Intent) {
        if (!BuildConfig.DEBUG) return
        val eventId = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID) ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                AlarmScheduler(context).cancel(eventId)
                repository.deleteEvent(eventId)
                NextAWidgetRefresh.refresh(context)
            } finally {
                pending.finish()
            }
        }
    }

    private fun handleAlarm(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID) ?: return
        val repeatIndex = intent.getIntExtra(AlarmScheduler.EXTRA_REPEAT_INDEX, 0)
        val store = AlarmSettingsStore(context)
        val settings = store.get(eventId) ?: return
        if (!settings.enabled || store.isAcknowledged(eventId)) return

        val now = System.currentTimeMillis()
        val remaining = (settings.startMillis - now).coerceAtLeast(0L)
        val remainingText = formatRemaining(remaining)
        val atStart = remaining <= 30_000L
        val spoken = if (atStart) {
            "Đã đến giờ ${settings.title}."
        } else {
            buildString {
                append("Còn $remainingText đến ${settings.title}.")
                if (settings.note.isNotBlank()) append(" Ghi chú: ${settings.note}.")
            }
        }

        createChannel(context)
        val openIntent = PendingIntent.getActivity(context, eventId.hashCode(), Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val acknowledgeIntent = PendingIntent.getBroadcast(context, eventId.hashCode() + 100_000, Intent(context, AlarmActionReceiver::class.java).apply {
            action = AlarmActionReceiver.ACTION_ACKNOWLEDGE
            putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(if (atStart) "Đến giờ" else "$remainingText nữa")
            .setContentText(settings.title + if (settings.note.isNotBlank()) " · ${settings.note}" else "")
            .setStyle(NotificationCompat.BigTextStyle().bigText(spoken))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setContentIntent(openIntent)
            .addAction(0, "Đã xác nhận", acknowledgeIntent)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(eventId.hashCode(), notification)
        }

        speak(context, spoken)

        if (!atStart && settings.repeatEnabled && repeatIndex < settings.maxRepeats) {
            val nextAt = minOf(settings.startMillis, now + settings.repeatIntervalMinutes * 60_000L)
            if (nextAt > now) {
                AlarmScheduler(context).scheduleRepeat(eventId, nextAt, repeatIndex + 1)
            }
        }
    }

    private fun speak(context: Context, text: String) {
        val pending = goAsync()
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                pending.finish()
                return@TextToSpeech
            }

            val engine = tts ?: run {
                pending.finish()
                return@TextToSpeech
            }
            val languageStatus = engine.setLanguage(Locale("vi", "VN"))
            if (languageStatus == TextToSpeech.LANG_MISSING_DATA || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.shutdown()
                pending.finish()
                return@TextToSpeech
            }

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onError(utteranceId: String?) {
                    engine.shutdown()
                    pending.finish()
                }
                override fun onDone(utteranceId: String?) {
                    engine.shutdown()
                    pending.finish()
                }
            })
            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "nexta-alarm")
            if (result == TextToSpeech.ERROR) {
                engine.shutdown()
                pending.finish()
            }
        }
    }

    private fun formatRemaining(millis: Long): String {
        val minutes = Duration.ofMillis(millis).toMinutes()
        return when {
            minutes >= 1440 -> "${minutes / 1440} ngày ${minutes % 1440 / 60} giờ"
            minutes >= 60 -> "${minutes / 60} giờ ${minutes % 60} phút"
            else -> "$minutes phút"
        }
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(CHANNEL_ID, "Nhắc sự kiện", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Âm báo và nhắc TTS cho sự kiện NextA"
                setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object { const val CHANNEL_ID = "nexta_event_alarm" }
}

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ACKNOWLEDGE) {
            val eventId = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID) ?: return
            AlarmScheduler(context).acknowledge(eventId)
            NotificationManagerCompat.from(context).cancel(eventId.hashCode())
        }
    }
    companion object { const val ACTION_ACKNOWLEDGE = "com.nexta.action.ALARM_ACKNOWLEDGE" }
}

private object NextAWidgetRefresh {
    fun refresh(context: Context) {
        com.nexta.widget.NextAWidgetProvider.requestUpdate(context)
        com.nexta.widget.NextAFocusWidgetProvider.requestUpdate(context)
    }
}
