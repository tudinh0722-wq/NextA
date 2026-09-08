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
            ACTION_REFRESH, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED, Intent.ACTION_BOOT_COMPLETED -> refreshWidgets(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) = refreshWidgets(context, ids)
    override fun onEnabled(context: Context) { super.onEnabled(context); refreshWidgets(context) }
    override fun onDisabled(context: Context) { cancelRefresh(context); super.onDisabled(context) }

    companion object {
        const val ACTION_REFRESH = "com.nexta.widget.ACTION_REFRESH"
        private const val REQUEST_CODE = 7421
        private const val COUNTDOWN_DAYS_LIMIT = 14L
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun requestUpdate(context: Context) = refreshWidgets(context)

        private fun refreshWidgets(context: Context, widgetIds: IntArray? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = widgetIds ?: manager.getAppWidgetIds(ComponentName(context, NextAWidgetProvider::class.java))
            if (ids.isEmpty()) { cancelRefresh(context); return }

            // Keep the first RemoteViews payload as simple as possible. A launcher
            // can reject the entire widget if one runtime RemoteViews action fails.
            ids.forEach { id ->
                val loadingViews = RemoteViews(context.packageName, R.layout.nexta_widget)
                bindEmptyState(loadingViews, 1)
                bindEmptyState(loadingViews, 2)
                manager.updateAppWidget(id, loadingViews)
            }

            Thread {
                val repository = try {
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        WidgetEntryPoint::class.java
                    ).eventRepository()
                } catch (_: Throwable) { return@Thread }
                val events = try { runBlocking { repository.getAllEvents().first() } } catch (_: Throwable) { return@Thread }
                val now = LocalDateTime.now()
                val current = events.filter { it.startDateTime <= now && now < it.endDateTime }.minByOrNull { it.endDateTime }
                val upcoming = events.filter { it.startDateTime > now }.sortedBy { it.startDateTime }.take(2)
                val cards = buildList<Pair<Event, Boolean>> {
                    current?.let { add(it to true) }
                    upcoming.forEach { add(it to false) }
                }.take(2)
                ids.forEach { updateWidget(manager, context, it, cards, now) }
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
            // Do not call setBackgroundResource through RemoteViews here.
            // Keep the widget on its XML background until the host is proven stable.
            bindCard(views, 1, cards.getOrNull(0), now)
            bindCard(views, 2, cards.getOrNull(1), now)
            manager.updateAppWidget(widgetId, views)
        }

        private fun bindEmptyState(views: RemoteViews, index: Int) {
            val status = if (index == 1) R.id.widget_status_1 else R.id.widget_status_2
            val title = if (index == 1) R.id.widget_title_1 else R.id.widget_title_2
            val time = if (index == 1) R.id.widget_time_1 else R.id.widget_time_2
            val countdown = if (index == 1) R.id.widget_countdown_1 else R.id.widget_countdown_2
            val location = if (index == 1) R.id.widget_location_1 else R.id.widget_location_2
            val note = if (index == 1) R.id.widget_note_1 else R.id.widget_note_2
            views.setTextViewText(status, "NEXTA")
            views.setTextViewText(title, "Đang tải lịch…")
            views.setTextViewText(time, "")
            views.setTextViewText(countdown, "")
            views.setTextViewText(location, "")
            views.setViewVisibility(note, View.GONE)
        }

        private fun bindCard(views: RemoteViews, index: Int, card: Pair<Event, Boolean>?, now: LocalDateTime) {
            val status = if (index == 1) R.id.widget_status_1 else R.id.widget_status_2
            val title = if (index == 1) R.id.widget_title_1 else R.id.widget_title_2
            val time = if (index == 1) R.id.widget_time_1 else R.id.widget_time_2
            val countdownView = if (index == 1) R.id.widget_countdown_1 else R.id.widget_countdown_2
            val location = if (index == 1) R.id.widget_location_1 else R.id.widget_location_2
            val note = if (index == 1) R.id.widget_note_1 else R.id.widget_note_2

            if (card == null) {
                views.setTextViewText(status, "NEXTA")
                views.setTextViewText(title, "Không còn lịch")
                views.setTextViewText(time, "")
                views.setTextViewText(countdownView, "")
                views.setTextViewText(location, "")
                views.setViewVisibility(note, View.GONE)
                return
            }

            val event = card.first
            val current = card.second
            val target = if (current) event.endDateTime else event.startDateTime
            val duration = Duration.between(now, target)
            views.setTextViewText(status, if (current) "ĐANG DIỄN RA" else "TIẾP THEO")
            views.setTextViewText(title, event.title)
            views.setTextViewText(
                time,
                "${event.startDateTime.format(timeFormatter)} – ${event.endDateTime.format(timeFormatter)}"
            )
            val countdownText = if (duration.toDays() > COUNTDOWN_DAYS_LIMIT) {
                ""
            } else if (current) {
                "Kết thúc sau ${formatDuration(duration)}"
            } else {
                "Bắt đầu sau ${formatDuration(duration)}"
            }
            views.setTextViewText(countdownView, countdownText)
            views.setTextViewText(location, event.location)
            if (event.note.isBlank()) {
                views.setViewVisibility(note, View.GONE)
            } else {
                views.setViewVisibility(note, View.VISIBLE)
                views.setTextViewText(note, event.note)
            }
        }

        private fun scheduleNextRefresh(context: Context, current: Event?, next: Event?, now: LocalDateTime) {
            val boundary = current?.endDateTime ?: next?.startDateTime ?: return
            val remaining = Duration.between(now, boundary)
            val target = when {
                remaining.toDays() > COUNTDOWN_DAYS_LIMIT -> now.plusDays(COUNTDOWN_DAYS_LIMIT)
                else -> maxOf(boundary, now.plusMinutes(1).withSecond(0).withNano(0))
            }
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

        private fun formatDuration(duration: Duration): String {
            val minutes = duration.toMinutes().coerceAtLeast(0)
            val days = duration.toDays()
            return when {
                days >= 1 -> "$days ngày"
                minutes >= 60 -> "${minutes / 60}h ${minutes % 60}m".replace(" 0m", "")
                minutes > 0 -> "${minutes}m"
                else -> "<1m"
            }
        }
    }
}
