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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
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
        AlarmPlaybackController.stop(eventId)
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
        val openIntent = PendingIntent.getActivity(
            context,
            eventId.hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val acknowledgeIntent = PendingIntent.getBroadcast(
            context,
            eventId.hashCode() + 100_000,
            Intent(context, AlarmActionReceiver::class.java).apply {
                action = AlarmActionReceiver.ACTION_ACKNOWLEDGE
                putExtra(AlarmScheduler.EXTRA_EVENT_ID, eventId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

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

        // TTS -> full default alarm tone -> TTS. The whole sequence is cancellable by ACK.
        AlarmPlaybackController.start(context, eventId, firstSpeech, finalSpeech)

        if (!atStart && settings.repeatEnabled && repeatIndex < settings.maxRepeats) {
            val nextAt = minOf(settings.startMillis, now + settings.repeatIntervalMinutes * 60_000L)
            if (nextAt > now) {
                AlarmScheduler(context).scheduleRepeat(eventId, nextAt, repeatIndex + 1)
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
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nhắc sự kiện",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo cho báo thức và TTS NextA"
                setSound(null, null)
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object { const val CHANNEL_ID = "nexta_event_alarm_v5" }
}

/**
 * Keeps the currently playing TTS/media objects addressable by eventId so the
 * notification ACK can interrupt the sequence immediately instead of waiting
 * for the alarm tone to finish.
 */
private object AlarmPlaybackController {
    private val sessions = ConcurrentHashMap<String, PlaybackSession>()

    fun start(context: Context, eventId: String, firstSpeech: String, finalSpeech: String) {
        stop(eventId)
        val session = PlaybackSession(context.applicationContext, eventId, firstSpeech, finalSpeech)
        sessions[eventId] = session
        session.start {
            sessions.remove(eventId, session)
        }
    }

    fun stop(eventId: String) {
        sessions.remove(eventId)?.stop()
    }

    private class PlaybackSession(
        private val context: Context,
        private val eventId: String,
        private val firstSpeech: String,
        private val finalSpeech: String
    ) {
        private val stopped = AtomicBoolean(false)
        private var tts: TextToSpeech? = null
        private var player: MediaPlayer? = null
        private var audioManager: AudioManager? = null
        private var focusRequest: AudioFocusRequest? = null
        private var focusGranted = false
        private var onFinished: (() -> Unit)? = null

        fun start(onFinished: () -> Unit) {
            this.onFinished = onFinished
            requestFocus()
            speak(firstSpeech) {
                if (stopped.get()) return@speak
                playDefaultAlarm {
                    if (stopped.get()) return@playDefaultAlarm
                    speak(finalSpeech) {
                        finish()
                    }
                }
            }
        }

        fun stop() {
            if (!stopped.compareAndSet(false, true)) return
            try { tts?.stop() } catch (_: Exception) {}
            try { tts?.shutdown() } catch (_: Exception) {}
            tts = null
            try { player?.stop() } catch (_: Exception) {}
            try { player?.release() } catch (_: Exception) {}
            player = null
            releaseFocus()
            onFinished?.invoke()
            onFinished = null
        }

        private fun finish() {
            if (!stopped.compareAndSet(false, true)) return
            try { tts?.shutdown() } catch (_: Exception) {}
            tts = null
            try { player?.release() } catch (_: Exception) {}
            player = null
            releaseFocus()
            onFinished?.invoke()
            onFinished = null
        }

        private fun requestFocus() {
            val manager = context.getSystemService(AudioManager::class.java) ?: return
            audioManager = manager
            focusGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .build()
                focusRequest = request
                manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                manager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_ALARM,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        }

        private fun releaseFocus() {
            if (!focusGranted) return
            val manager = audioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                focusRequest?.let { manager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                manager.abandonAudioFocus(null)
            }
            focusGranted = false
        }

        private fun speak(text: String, onDone: () -> Unit) {
            if (stopped.get()) return
            val engine = TextToSpeech(context) { status ->
                if (stopped.get()) {
                    try { engine.shutdown() } catch (_: Exception) {}
                    return@TextToSpeech
                }
                if (status != TextToSpeech.SUCCESS) {
                    onDone()
                    return@TextToSpeech
                }
                tts = engine
                val languageStatus = engine.setLanguage(Locale("vi", "VN"))
                if (languageStatus == TextToSpeech.LANG_MISSING_DATA || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
                    engine.shutdown()
                    tts = null
                    onDone()
                    return@TextToSpeech
                }
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onError(utteranceId: String?) {
                        if (tts === engine) tts = null
                        try { engine.shutdown() } catch (_: Exception) {}
                        if (!stopped.get()) onDone()
                    }
                    override fun onDone(utteranceId: String?) {
                        if (tts === engine) tts = null
                        try { engine.shutdown() } catch (_: Exception) {}
                        if (!stopped.get()) onDone()
                    }
                })
                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                val result = engine.speak(
                    text,
                    TextToSpeech.QUEUE_FLUSH,
                    params,
                    "nexta-alarm-$eventId-${System.nanoTime()}"
                )
                if (result == TextToSpeech.ERROR) {
                    if (tts === engine) tts = null
                    engine.shutdown()
                    if (!stopped.get()) onDone()
                }
            }
            tts = engine
        }

        private fun playDefaultAlarm(onComplete: () -> Unit) {
            if (stopped.get()) return
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alarmUri == null) {
                onComplete()
                return
            }
            try {
                val mediaPlayer = MediaPlayer()
                player = mediaPlayer
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mediaPlayer.setDataSource(context, alarmUri)
                mediaPlayer.setOnCompletionListener {
                    if (player === mediaPlayer) player = null
                    try { mediaPlayer.release() } catch (_: Exception) {}
                    if (!stopped.get()) onComplete()
                }
                mediaPlayer.setOnErrorListener { _, _, _ ->
                    if (player === mediaPlayer) player = null
                    try { mediaPlayer.release() } catch (_: Exception) {}
                    if (!stopped.get()) onComplete()
                    true
                }
                mediaPlayer.prepare()
                if (!stopped.get()) {
                    mediaPlayer.start()
                } else {
                    try { mediaPlayer.release() } catch (_: Exception) {}
                    player = null
                }
            } catch (_: Exception) {
                player = null
                if (!stopped.get()) onComplete()
            }
        }
    }
}

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_ACKNOWLEDGE) {
            val eventId = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID) ?: return

            // Stop the currently running TTS/alarm tone first. This is immediate
            // even when the default alarm sound is in the middle of playback.
            AlarmPlaybackController.stop(eventId)
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
