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
import java.util.Locale

class NextAWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REFRESH,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED -> requestUpdate(context)
            else -> super.onReceive(context, intent)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        Thread {
            try {
                val repository = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java
                ).eventRepository()
                val events = runBlocking { repository.getAllEvents().first() }

                appWidgetIds.forEach { id ->
                    updateWidget(context, appWidgetManager, id, events)
                }
                scheduleNextRefresh(context, events)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        requestUpdate(context)
    }

    override fun onDisabled(context: Context) {
        cancelRefresh(context)
        super.onDisabled(context)
    }

    companion object {
        const val ACTION_REFRESH = "com.nexta.widget.ACTION_REFRESH"
        private const val REQUEST_CODE = 7421
        private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val dayFormatter = DateTimeFormatter.ofPattern("EEE, dd/MM", Locale("vi", "VN"))

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NextAWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                NextAWidgetProvider().onUpdate(context, manager, ids)
            }
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            events: List<Event>
        ) {
            val now = LocalDateTime.now()
            val current = events
                .filter { it.startDateTime <= now && now < it.endDateTime }
                .minByOrNull { it.endDateTime }
            val next = events
                .filter { it.startDateTime > now }
                .minByOrNull { it.startDateTime }
            val event = current ?: next

            val views = RemoteViews(context.packageName, R.layout.nexta_widget)
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))

            if (event == null) {
                views.setTextViewText(R.id.widget_status, "KHÔNG CÒN LỊCH")
                views.setTextViewText(R.id.widget_event_title, "Bạn đã hết lịch")
                views.setTextViewText(R.id.widget_time, "Mở NextA để xem lịch")
            } else {
                views.setTextViewText(
                    R.id.widget_status,
                    if (current != null) "ĐANG DIỄN RA" else "TIẾP THEO"
                )
                views.setTextViewText(R.id.widget_event_title, event.title)
                views.setTextViewText(
                    R.id.widget_time,
                    "${event.startDateTime.format(dayFormatter)}  •  " +
                        "${event.startDateTime.format(timeFormatter)} – ${event.endDateTime.format(timeFormatter)}"
                )
            }

            manager.updateAppWidget(widgetId, views)
        }

        private fun openAppPendingIntent(context: Context): PendingIntent {
            return PendingIntent.getActivity(
                context,
                REQUEST_CODE,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun scheduleNextRefresh(context: Context, events: List<Event>) {
            val now = LocalDateTime.now()
            val current = events
                .filter { it.startDateTime <= now && now < it.endDateTime }
                .minByOrNull { it.endDateTime }
            val nextBoundary = current?.endDateTime
                ?: events.filter { it.startDateTime > now }
                    .minByOrNull { it.startDateTime }
                    ?.startDateTime

            val alarmManager = context.getSystemService(AlarmManager::class.java)
            val pendingIntent = refreshPendingIntent(context)
            alarmManager.cancel(pendingIntent)

            if (nextBoundary != null) {
                val delayMillis = Duration.between(now, nextBoundary)
                    .toMillis()
                    .coerceAtLeast(1_000L)
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + delayMillis,
                    pendingIntent
                )
            }
        }

        private fun cancelRefresh(context: Context) {
            context.getSystemService(AlarmManager::class.java)
                .cancel(refreshPendingIntent(context))
        }

        private fun refreshPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, NextAWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
