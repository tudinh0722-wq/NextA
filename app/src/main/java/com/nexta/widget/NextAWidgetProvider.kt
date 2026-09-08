package com.nexta.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.nexta.R
import com.nexta.data.model.Event
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class NextAWidgetProvider : AppWidgetProvider() {

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

                val events = runBlocking {
                    repository.getAllEvents().first()
                }

                appWidgetIds.forEach { appWidgetId ->
                    updateWidget(context, appWidgetManager, appWidgetId, events)
                }
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        requestUpdate(context)
    }

    companion object {
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

            val views = RemoteViews(context.packageName, R.layout.nexta_widget)
            views.setTextViewText(R.id.widget_title, "NextA")

            val mainEvent = current ?: next
            if (mainEvent == null) {
                views.setTextViewText(R.id.widget_status, "Hôm nay không còn lịch")
                views.setTextViewText(R.id.widget_event_title, "Không có sự kiện sắp tới")
                views.setTextViewText(R.id.widget_time, "")
                views.setTextViewText(R.id.widget_location, "")
            } else {
                views.setTextViewText(
                    R.id.widget_status,
                    if (current != null) "ĐANG DIỄN RA" else "TIẾP THEO"
                )
                views.setTextViewText(R.id.widget_event_title, mainEvent.title)
                views.setTextViewText(
                    R.id.widget_time,
                    "${mainEvent.startDateTime.format(dayFormatter)}  •  " +
                        "${mainEvent.startDateTime.format(timeFormatter)} – ${mainEvent.endDateTime.format(timeFormatter)}"
                )
                views.setTextViewText(R.id.widget_location, mainEvent.location)
            }

            manager.updateAppWidget(widgetId, views)
        }
    }
}
