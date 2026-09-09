package com.nexta.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
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
        val firstSpeech = if (atStart) {
            "Đến giờ ${settings.title}."
        } else {
            "NextA nhắc bạn: còn $remainingText đến ${settings.title}."
        }
        val finalSpeech = if (atStart) {
            buildString {
                append("Đã đến giờ ${settings.title}.")
                if (settings.note.isNotBlank()) append(" Ghi chú: ${settings.note}.")
            }
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
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalSpeech))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSilent(true)
            .setAutoCancel(false)
            .setContentIntent(openIntent)
            .addAction(0, "Đã xác nhận", acknowledgeIntent)
            .build()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(eventId.hashCode(), notification)
        }

        // Attention sequence: TTS -> full default alarm tone -> TTS.
        // Audio focus temporarily interrupts/ducks other media so both speech parts are audible.
        playAttentionSequence(context, firstSpeech, finalSpeech)

        if (!atStart && settings.repeatEnabled && repeatIndex < settings.maxRepeats) {
            val nextAt = minOf(settings.startMillis, now + settings.repeatIntervalMinutes * 60_000L)
            if (nextAt > now) {
                AlarmScheduler(context).scheduleRepeat(eventId, nextAt, repeatIndex + 1)
            }
        }
    }

    private fun playAttentionSequence(context: Context, firstSpeech: String, finalSpeech: String) {
        val pending = goAsync()
        val audioManager = context.getSystemService(AudioManager::class.java)
        val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .build()
        } else null

        val focusGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(focusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }

        fun releaseFocus() {
            if (!focusGranted) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioManager.abandonAudioFocusRequest(focusRequest!!)
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(null)
            }
        }

        speak(context, firstSpeech, object : SequenceCallback {
            override fun onDone() {
                playDefaultAlarm(context, {
                    speak(context, finalSpeech, object : SequenceCallback {
                        override fun onDone() {
                            releaseFocus()
                            pending.finish()
                        }
                    })
                }, {
                    speak(context, finalSpeech, object : SequenceCallback {
                        override fun onDone() {
                            releaseFocus()
                            pending.finish()
                        }
                    })
                })
            }
        })
    }

    private fun playDefaultAlarm(context: Context, onComplete: () -> Unit, onError: () -> Unit) {
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alarmUri == null) {
            onError()
            return
        }

        try {
            val player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(context, alarmUri)
            var completed = false
            fun finish(success: Boolean) {
                if (completed) return
                completed = true
                player.release()
                if (success) onComplete() else onError()
            }
            player.setOnCompletionListener { finish(true) }
            player.setOnErrorListener { _, _, _ -> finish(false); true }
            player.prepare()
            player.start()
            // Do not cap the user's default alarm tone. Let it play to its natural end,
            // then start the second TTS so the speech is never buried under the tone.
        } catch (_: Exception) {
            onError()
        }
    }

    private fun speak(context: Context, text: String, callback: SequenceCallback) {
        var tts: TextToSpeech? = null
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status != TextToSpeech.SUCCESS) {
                callback.onDone()
                return@TextToSpeech
            }

            val engine = tts ?: run {
                callback.onDone()
                return@TextToSpeech
            }
            val languageStatus = engine.setLanguage(Locale("vi", "VN"))
            if (languageStatus == TextToSpeech.LANG_MISSING_DATA || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
                engine.shutdown()
                callback.onDone()
                return@TextToSpeech
            }

            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onError(utteranceId: String?) {
                    engine.shutdown()
                    callback.onDone()
                }
                override fun onDone(utteranceId: String?) {
                    engine.shutdown()
                    callback.onDone()
                }
            })

            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            val result = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, "nexta-alarm-tts-${System.nanoTime()}")
            if (result == TextToSpeech.ERROR) {
                engine.shutdown()
                callback.onDone()
            }
        }
    }

    private interface SequenceCallback {
        fun onDone()
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
                description = "Thông báo cho báo thức và TTS NextA"
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object { const val CHANNEL_ID = "nexta_event_alarm_v5" }
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
