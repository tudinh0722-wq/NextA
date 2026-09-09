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
import com.nexta.MainActivity
import com.nexta.R
import com.nexta.data.repository.AlarmRepository
import com.nexta.data.repository.LegacyAlarmMigrator
import dagger.hilt.android.AndroidEntryPoint
import java.time.Duration
import java.time.ZoneId
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Scope helper: tạo Job riêng cho mỗi goAsync() call.
 * Job bị cancel khi pending.finish() được gọi trong finally,
 * tránh coroutine sống lâu hơn BroadcastReceiver window (10s).
 */
private fun BroadcastReceiver.PendingResult.launchAsync(block: suspend CoroutineScope.() -> Unit): Job {
    val job = Job()
    CoroutineScope(Dispatchers.IO + job).launch {
        try {
            block()
        } finally {
            finish()
            job.cancel()
        }
    }
    return job
}

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var legacyAlarmMigrator: LegacyAlarmMigrator

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AlarmScheduler.ACTION_ALARM -> goAsync().launchAsync {
                legacyAlarmMigrator.migrate()
                handleAlarm(context, intent)
            }
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.TIME_SET",
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED" -> goAsync().launchAsync {
                legacyAlarmMigrator.migrate()
                AlarmScheduler(context, alarmRepository).rescheduleAll()
            }
        }
    }

    private suspend fun handleAlarm(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID) ?: return
        val repeatIndex = intent.getIntExtra(AlarmScheduler.EXTRA_REPEAT_INDEX, 0)
        val settings = alarmRepository.getAlarm(eventId) ?: return
        val event = alarmRepository.getEvent(eventId) ?: return
        if (!settings.enabled || settings.acknowledged) return

        val now = System.currentTimeMillis()
        val startMillis = event.startDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val remaining = (startMillis - now).coerceAtLeast(0L)
        val remainingText = formatRemaining(remaining)
        val atStart = remaining <= 30_000L
        val firstSpeech = if (atStart) "Đến giờ ${event.title}." else "NextA nhắc bạn: còn $remainingText đến ${event.title}."
        val finalSpeech = if (atStart) {
            buildString {
                append("Đã đến giờ ${event.title}.")
                if (event.note.isNotBlank()) append(" Ghi chú: ${event.note}.")
            }
        } else {
            buildString {
                append("Còn $remainingText đến ${event.title}.")
                if (event.note.isNotBlank()) append(" Ghi chú: ${event.note}.")
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
            .setContentText(event.title + if (event.note.isNotBlank()) " · ${event.note}" else "")
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

        AlarmPlaybackController.start(context, eventId, firstSpeech, finalSpeech)

        if (!atStart && settings.repeatEnabled && repeatIndex < settings.maxRepeats) {
            val nextAt = minOf(startMillis, now + settings.repeatIntervalMinutes * 60_000L)
            if (nextAt > now) AlarmScheduler(context, alarmRepository).scheduleRepeat(eventId, nextAt, repeatIndex + 1)
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
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Nhắc sự kiện", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Thông báo cho báo thức và TTS NextA"
                    setSound(null, null)
                }
            )
        }
    }

    companion object { const val CHANNEL_ID = "nexta_event_alarm_v5" }
}

private object AlarmPlaybackController {
    private val sessions = ConcurrentHashMap<String, PlaybackSession>()

    fun start(context: Context, eventId: String, firstSpeech: String, finalSpeech: String) {
        stop(eventId)
        val session = PlaybackSession(context.applicationContext, eventId, firstSpeech, finalSpeech)
        sessions[eventId] = session
        session.start { sessions.remove(eventId, session) }
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
                    speak(finalSpeech) { finish() }
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
                manager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
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
            var engine: TextToSpeech? = null
            engine = TextToSpeech(context) { status ->
                val current = engine ?: return@TextToSpeech
                if (stopped.get()) {
                    try { current.shutdown() } catch (_: Exception) {}
                    return@TextToSpeech
                }
                if (status != TextToSpeech.SUCCESS) {
                    onDone()
                    return@TextToSpeech
                }
                tts = current
                val languageStatus = current.setLanguage(Locale("vi", "VN"))
                if (languageStatus == TextToSpeech.LANG_MISSING_DATA || languageStatus == TextToSpeech.LANG_NOT_SUPPORTED) {
                    current.shutdown()
                    if (tts === current) tts = null
                    onDone()
                    return@TextToSpeech
                }
                current.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onError(utteranceId: String?) {
                        if (tts === current) tts = null
                        try { current.shutdown() } catch (_: Exception) {}
                        if (!stopped.get()) onDone()
                    }
                    override fun onDone(utteranceId: String?) {
                        if (tts === current) tts = null
                        try { current.shutdown() } catch (_: Exception) {}
                        if (!stopped.get()) onDone()
                    }
                })
                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                val result = current.speak(text, TextToSpeech.QUEUE_FLUSH, params, "nexta-alarm-$eventId-${System.nanoTime()}")
                if (result == TextToSpeech.ERROR) {
                    if (tts === current) tts = null
                    current.shutdown()
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
                if (!stopped.get()) mediaPlayer.start() else {
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

@AndroidEntryPoint
class AlarmActionReceiver : BroadcastReceiver() {
    @Inject lateinit var alarmRepository: AlarmRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_ACKNOWLEDGE) return
        val eventId = intent.getStringExtra(AlarmScheduler.EXTRA_EVENT_ID) ?: return
        AlarmPlaybackController.stop(eventId)
        goAsync().launchAsync {
            alarmRepository.acknowledge(eventId)
            NotificationManagerCompat.from(context).cancel(eventId.hashCode())
        }
    }

    companion object { const val ACTION_ACKNOWLEDGE = "com.nexta.action.ALARM_ACKNOWLEDGE" }
}
