package com.nexta.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.nexta.R
import com.nexta.data.model.Event
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NextAWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> refreshWidgets(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refreshWidgets(context, ids)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        refreshWidgets(context)
    }

    override fun onDisabled(context: Context) {
        cancelRefresh(context)
        super.onDisabled(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.nexta.widget.ACTION_REFRESH"
        private const val REQUEST_CODE = 7421
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun requestUpdate(context: Context) = refreshWidgets(context)

        private fun refreshWidgets(context: Context, widgetIds: IntArray? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = widgetIds ?: manager.getAppWidgetIds(
                ComponentName(context, NextAWidgetProvider::class.java)
            )
            if (ids.isEmpty()) {
                cancelRefresh(context)
                return
            }

            Thread {
                val repository = try {
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        WidgetEntryPoint::class.java
                    ).eventRepository()
                } catch (_: Throwable) {
                    return@Thread
                }

                val events = try {
                    runBlocking { repository.getAllEvents().first() }
                } catch (_: Throwable) {
                    return@Thread
                }

                val now = LocalDateTime.now()
                val current = events
                    .filter { it.startDateTime <= now && now < it.endDateTime }
                    .minByOrNull { it.endDateTime }
                val upcoming = events
                    .filter { it.startDateTime > now }
                    .sortedBy { it.startDateTime }
                    .take(2)

                val cards = buildList<Pair<Event, Boolean>> {
                    current?.let { add(it to true) }
                    upcoming.forEach { add(it to false) }
                }.take(2)

                ids.forEach { id -> updateWidget(manager, context, id, cards, now) }
                scheduleNextRefresh(context, current, upcoming.firstOrNull(), now)
            }.start()
        }

        private fun updateWidget(
            manager: AppWidgetManager,
            context: Context,
            widgetId: Int,
            cards: List<Pair<Event, Boolean>>,
            now: LocalDateTime
        ) {
            val views = RemoteViews(context.packageName, R.layout.nexta_widget)
            bindCard(views, 1, cards.getOrNull(0), now)
            bindCard(views, 2, cards.getOrNull(1), now)
            manager.updateAppWidget(widgetId, views)
        }

        private fun bindCard(
            views: RemoteViews,
            index: Int,
            card: Pair<Event, Boolean>?,
            now: LocalDateTime
        ) {
            val title = if (index == 1) R.id.widget_title_1 else R.id.widget_title_2
            val startTime = if (index == 1) R.id.widget_start_time_1 else R.id.widget_start_time_2
            val endTime = if (index == 1) R.id.widget_end_time_1 else R.id.widget_end_time_2
            val countdown = if (index == 1) R.id.widget_countdown_1 else R.id.widget_countdown_2
            val location = if (index == 1) R.id.widget_location_1 else R.id.widget_location_2
            val note = if (index == 1) R.id.widget_note_1 else R.id.widget_note_2
            val progress = if (index == 1) R.id.widget_progress_1 else R.id.widget_progress_2
            val progressDot = if (index == 1) R.id.widget_progress_dot_1 else R.id.widget_progress_dot_2

            if (card == null) {
                views.setTextViewText(title, "Trống")
                views.setTextViewText(startTime, "")
                views.setTextViewText(endTime, "")
                views.setTextViewText(countdown, "—")
                views.setTextViewText(location, "")
                views.setViewVisibility(note, View.GONE)
                views.setProgressBar(progress, 100, 0, false)
                views.setViewVisibility(progressDot, View.GONE)
                return
            }

            val event = card.first
            val current = card.second
            val target = if (current) event.endDateTime else event.startDateTime
            val minutes = Duration.between(now, target).toMinutes().coerceAtLeast(0)

            val totalMinutes = Duration.between(event.startDateTime, event.endDateTime)
                .toMinutes()
                .coerceAtLeast(1)
            val elapsedMinutes = Duration.between(event.startDateTime, now)
                .toMinutes()
                .coerceIn(0, totalMinutes)
            val progressPercent = if (current) {
                ((elapsedMinutes * 100) / totalMinutes).toInt().coerceIn(0, 100)
            } else {
                0
            }

            views.setTextViewText(title, event.title)
            views.setTextViewText(startTime, event.startDateTime.format(timeFormatter))
            views.setTextViewText(endTime, event.endDateTime.format(timeFormatter))
            views.setProgressBar(progress, 100, progressPercent, false)
            views.setViewVisibility(progressDot, View.VISIBLE)
            views.setTextViewText(
                countdown,
                if (current) "Kết thúc sau ${formatDuration(minutes)}"
                else "Bắt đầu sau ${formatDuration(minutes)}"
            )
            views.setTextViewText(location, event.location)

            if (event.note.isBlank()) {
                views.setViewVisibility(note, View.GONE)
            } else {
                views.setViewVisibility(note, View.VISIBLE)
                views.setTextViewText(note, event.note)
            }
        }

        private fun scheduleNextRefresh(
            context: Context,
            current: Event?,
            next: Event?,
            now: LocalDateTime
        ) {
            val boundary = current?.endDateTime ?: next?.startDateTime
            val minuteTick = now.plusMinutes(1).withSecond(0).withNano(0)
            val target = when {
                boundary == null -> null
                boundary.isBefore(minuteTick) -> boundary
                else -> minOf(boundary, minuteTick)
            } ?: return
            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val pendingIntent = refreshPendingIntent(context)
            alarmManager.cancel(pendingIntent)
            val delay = Duration.between(now, target).toMillis().coerceAtLeast(1_000L)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + delay,
                pendingIntent
            )
        }

        private fun cancelRefresh(context: Context) = context.getSystemService(AlarmManager::class.java)
            .cancel(refreshPendingIntent(context))

        private fun refreshPendingIntent(context: Context) = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, NextAWidgetProvider::class.java).apply { action = ACTION_REFRESH },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun formatDuration(minutes: Long): String = when {
            minutes >= 24 * 60 -> "${minutes / (24 * 60)} ngày"
            minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m".replace(" 0m", "")
            minutes > 0 -> "${minutes}m"
            else -> "<1m"
        }
    }
}
