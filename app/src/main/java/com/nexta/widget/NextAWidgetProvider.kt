package com.nexta.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.nexta.MainActivity
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
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> {
                refreshWidgets(context)
                scheduleMinuteRefresh(context)
            }
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Put a valid RemoteViews on the launcher immediately. Database access is
        // deliberately kept out of the AppWidgetProvider callback.
        appWidgetIds.forEach { id ->
            runCatching {
                appWidgetManager.updateAppWidget(id, emptyWidget(context))
            }
        }
        refreshWidgets(context, appWidgetIds)
        scheduleMinuteRefresh(context)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleMinuteRefresh(context)
    }

    override fun onDisabled(context: Context) {
        cancelMinuteRefresh(context)
        super.onDisabled(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.nexta.widget.ACTION_REFRESH"
        private const val REQUEST_CODE = 7421
        private const val REFRESH_REQUEST_CODE = 7422
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun requestUpdate(context: Context) = refreshWidgets(context)

        private fun emptyWidget(context: Context): RemoteViews =
            RemoteViews(context.packageName, R.layout.nexta_widget)

        private fun refreshWidgets(context: Context, widgetIds: IntArray? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = widgetIds ?: manager.getAppWidgetIds(
                ComponentName(context, NextAWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return

            Thread {
                try {
                    val repository = EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        WidgetEntryPoint::class.java
                    ).eventRepository()
                    val events = runBlocking { repository.getAllEvents().first() }
                    val now = LocalDateTime.now()
                    val ordered = events.sortedBy { it.startDateTime }
                    val current = ordered.firstOrNull {
                        !now.isBefore(it.startDateTime) && now.isBefore(it.endDateTime)
                    }
                    val upcoming = ordered.filter { it.startDateTime.isAfter(now) }
                    val displayEvents = if (current != null) {
                        listOf(current) + upcoming.take(1)
                    } else {
                        upcoming.take(2)
                    }

                    ids.forEach { id ->
                        runCatching {
                            updateWidget(context, manager, id, displayEvents, now)
                        }
                    }
                } catch (_: Throwable) {
                    // Keep the launcher widget alive even if application data is unavailable.
                }
            }.start()
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            events: List<Event>,
            now: LocalDateTime
        ) {
            val views = RemoteViews(context.packageName, R.layout.nexta_widget)
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))
            bindEvent(views, 1, events.getOrNull(0), now)
            bindEvent(views, 2, events.getOrNull(1), now)
            manager.updateAppWidget(widgetId, views)
        }

        private fun bindEvent(views: RemoteViews, slot: Int, event: Event?, now: LocalDateTime) {
            val statusId = if (slot == 1) R.id.widget_status_1 else R.id.widget_status_2
            val locationId = if (slot == 1) R.id.widget_location_1 else R.id.widget_location_2
            val titleId = if (slot == 1) R.id.widget_title_1 else R.id.widget_title_2
            val timeId = if (slot == 1) R.id.widget_time_1 else R.id.widget_time_2
            val countdownId = if (slot == 1) R.id.widget_countdown_1 else R.id.widget_countdown_2
            val noteId = if (slot == 1) R.id.widget_note_1 else R.id.widget_note_2

            if (event == null) {
                views.setTextViewText(statusId, "")
                views.setTextViewText(locationId, "")
                views.setTextViewText(titleId, if (slot == 1) "Không có lịch sắp tới" else "")
                views.setTextViewText(timeId, "")
                views.setTextViewText(countdownId, "")
                views.setTextViewText(noteId, "")
                views.setViewVisibility(noteId, View.GONE)
                return
            }

            val isLive = !now.isBefore(event.startDateTime) && now.isBefore(event.endDateTime)
            views.setTextViewText(statusId, if (isLive) "ĐANG DIỄN RA" else "TIẾP THEO")
            views.setTextViewText(locationId, event.location.takeIf { it.isNotBlank() } ?: "")
            views.setTextViewText(titleId, event.title)
            views.setTextViewText(
                timeId,
                "${event.startDateTime.format(timeFormatter)} – ${event.endDateTime.format(timeFormatter)}"
            )

            val remaining = if (isLive) {
                Duration.between(now, event.endDateTime)
            } else {
                Duration.between(now, event.startDateTime)
            }.coerceAtLeast(Duration.ZERO)
            val countdown = formatRemaining(remaining)
            views.setTextViewText(countdownId, if (isLive) "Còn $countdown" else "Bắt đầu sau $countdown")

            if (event.note.isNotBlank()) {
                views.setTextViewText(noteId, event.note)
                views.setViewVisibility(noteId, View.VISIBLE)
            } else {
                views.setTextViewText(noteId, "")
                views.setViewVisibility(noteId, View.GONE)
            }
        }

        private fun formatRemaining(duration: Duration): String {
            val totalMinutes = duration.toMinutes()
            val days = totalMinutes / (24 * 60)
            val hours = (totalMinutes % (24 * 60)) / 60
            val minutes = totalMinutes % 60
            return when {
                days > 0 -> "${days}d ${hours}h"
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m"
            }
        }

        private fun scheduleMinuteRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NextAWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REFRESH_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val firstMinute = ((System.currentTimeMillis() / 60_000L) + 1) * 60_000L
            alarmManager.setInexactRepeating(
                AlarmManager.RTC,
                firstMinute,
                60_000L,
                pendingIntent
            )
        }

        private fun cancelMinuteRefresh(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NextAWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                REFRESH_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }

        private fun openAppPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
