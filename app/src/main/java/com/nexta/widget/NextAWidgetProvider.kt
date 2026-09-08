package com.nexta.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nexta.MainActivity
import com.nexta.R
import com.nexta.data.model.Event
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class NextAWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
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
            if (ids.isEmpty()) return

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

                val today = LocalDate.now()
                val todayEvents = events
                    .filter { it.startDateTime.toLocalDate() == today }
                    .sortedBy { it.startDateTime }
                    .take(4)

                ids.forEach { id -> updateWidget(context, manager, id, todayEvents) }
            }.start()
        }

        private fun updateWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            events: List<Event>
        ) {
            val now = LocalDateTime.now()
            val views = RemoteViews(context.packageName, R.layout.nexta_widget)
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent(context))

            views.setTextViewText(
                R.id.widget_status,
                "${weekdayLabel(now.dayOfWeek)} · ${now.dayOfMonth.toString().padStart(2, '0')}/" +
                    now.monthValue.toString().padStart(2, '0')
            )
            views.setTextViewText(R.id.widget_location, "${events.size} MỤC")
            views.setTextViewText(R.id.widget_countdown, "")

            if (events.isEmpty()) {
                views.setTextViewText(R.id.widget_event_title, "Hôm nay không có lịch")
                views.setTextViewText(R.id.widget_time, "Chạm để mở NextA")
            } else {
                val lines = events.joinToString("\n") { event ->
                    "${event.startDateTime.format(timeFormatter)}  ${event.title}"
                }
                val locations = events.mapNotNull { it.location.takeIf(String::isNotBlank) }
                    .distinct()
                    .take(3)
                    .joinToString("  ·  ")

                views.setTextViewText(R.id.widget_event_title, lines)
                views.setTextViewText(
                    R.id.widget_time,
                    locations.ifBlank { "Lịch hôm nay · ${events.size} sự kiện" }
                )
            }

            manager.updateAppWidget(widgetId, views)
        }

        private fun weekdayLabel(day: DayOfWeek): String = when (day) {
            DayOfWeek.MONDAY -> "T2"
            DayOfWeek.TUESDAY -> "T3"
            DayOfWeek.WEDNESDAY -> "T4"
            DayOfWeek.THURSDAY -> "T5"
            DayOfWeek.FRIDAY -> "T6"
            DayOfWeek.SATURDAY -> "T7"
            DayOfWeek.SUNDAY -> "CN"
        }

        private fun openAppPendingIntent(context: Context): PendingIntent = PendingIntent.getActivity(
            context,
            REQUEST_CODE,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
