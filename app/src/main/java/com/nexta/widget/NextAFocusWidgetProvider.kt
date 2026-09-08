package com.nexta.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
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

class NextAFocusWidgetProvider : AppWidgetProvider() {
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

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) = refreshWidgets(context, appWidgetIds)

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        refreshWidgets(context)
    }

    override fun onDisabled(context: Context) {
        cancelRefresh(context)
        super.onDisabled(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.nexta.widget.ACTION_FOCUS_REFRESH"
        private const val REQUEST_CODE = 7422
        private const val COUNTDOWN_DAYS_LIMIT = 14L
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        fun requestUpdate(context: Context) = refreshWidgets(context)

        private fun refreshWidgets(context: Context, widgetIds: IntArray? = null) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = widgetIds ?: manager.getAppWidgetIds(
                ComponentName(context, NextAFocusWidgetProvider::class.java)
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
                val next = events
                    .filter { it.startDateTime > now }
                    .minByOrNull { it.startDateTime }

                ids.forEach { id -> updateWidget(context, manager, id, current, next, now) }
                scheduleNextRefresh(context, current, next, now)
            }.start()
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            current: Event?,
            next: Event?,
            now: LocalDateTime
        ) {
            val views = RemoteViews(context.packageName, R.layout.nexta_focus_widget)
            views.setInt(R.id.focus_widget_root, "setBackgroundResource", backgroundForHour(now.hour))
            views.setOnClickPendingIntent(R.id.focus_widget_root, openAppPendingIntent(context))

            val event = current ?: next
            if (event == null) {
                views.setTextViewText(R.id.focus_status, "DONE")
                views.setTextViewText(R.id.focus_location, "NEXTA")
                views.setTextViewText(R.id.focus_countdown, "—")
                views.setTextViewText(R.id.focus_title, "Không còn lịch")
                views.setTextViewText(R.id.focus_time, "Hôm nay đã hết lịch")
                views.setTextViewText(R.id.focus_next, "Mở NextA để thêm sự kiện")
            } else {
                val target = if (current != null) event.endDateTime else event.startDateTime
                val duration = Duration.between(now, target)
                views.setTextViewText(R.id.focus_status, if (current != null) "LIVE" else "NEXT")
                views.setTextViewText(
                    R.id.focus_location,
                    event.location.takeIf { it.isNotBlank() } ?: "SCHEDULE"
                )
                views.setTextViewText(R.id.focus_countdown, formatCountdown(duration))
                views.setTextViewText(R.id.focus_title, event.title)
                views.setTextViewText(
                    R.id.focus_time,
                    "${event.startDateTime.format(timeFormatter)} – ${event.endDateTime.format(timeFormatter)}"
                )

                val nextHint = if (current != null && next != null) {
                    "NEXT  ${next.startDateTime.format(timeFormatter)}  ${next.title}"
                } else if (current != null) {
                    "NEXT  Không còn lịch sau sự kiện này"
                } else {
                    "FOCUS  Sự kiện sắp tới của bạn"
                }
                views.setTextViewText(R.id.focus_next, nextHint)
            }

            manager.updateAppWidget(widgetId, views)
        }

        private fun scheduleNextRefresh(
            context: Context,
            current: Event?,
            next: Event?,
            now: LocalDateTime
        ) {
            val boundary = current?.endDateTime ?: next?.startDateTime
            val target = when {
                boundary == null -> null
                Duration.between(now, boundary).toDays() > COUNTDOWN_DAYS_LIMIT -> now.plusDays(COUNTDOWN_DAYS_LIMIT)
                boundary.isBefore(now.plusMinutes(1)) -> boundary
                else -> now.plusMinutes(1).withSecond(0).withNano(0)
            }

            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val pendingIntent = refreshPendingIntent(context)
            alarmManager.cancel(pendingIntent)

            if (target != null) {
                val delay = Duration.between(now, target).toMillis().coerceAtLeast(1_000L)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delay,
                    pendingIntent
                )
            }
        }

        private fun backgroundForHour(hour: Int): Int = when (hour) {
            in 5..10 -> R.drawable.nexta_widget_background_morning
            in 11..16 -> R.drawable.nexta_widget_background_day
            in 17..20 -> R.drawable.nexta_widget_background_evening
            else -> R.drawable.nexta_widget_background_night
        }

        private fun cancelRefresh(context: Context) {
            context.getSystemService(AlarmManager::class.java)
                .cancel(refreshPendingIntent(context))
        }

        private fun refreshPendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, NextAFocusWidgetProvider::class.java).apply { action = ACTION_REFRESH },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        private fun formatCountdown(duration: Duration): String {
            val minutes = duration.toMinutes().coerceAtLeast(0)
            if (minutes < 1) return "NOW"
            val days = duration.toDays()
            val hours = minutes / 60
            val remaining = minutes % 60
            return when {
                days > COUNTDOWN_DAYS_LIMIT -> ""
                days >= 1 -> "$days ngày"
                hours > 0 && remaining > 0 -> "${hours}h ${remaining}m"
                hours > 0 -> "${hours}h"
                else -> "${minutes}m"
            }
        }

        private fun openAppPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
