package com.nexta.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.nexta.R

class NextAWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Diagnostic version: keep the provider completely synchronous and
        // independent from Hilt, Room, AlarmManager and PendingIntent.
        // If this widget can be added, the previous failure is in the runtime
        // data/update path rather than the launcher metadata.
        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.nexta_widget).apply {
                setTextViewText(R.id.widget_status_1, "NEXTA")
                setTextViewText(R.id.widget_title_1, "Widget hoạt động")
                setTextViewText(R.id.widget_time_1, "Kiểm tra RemoteViews")
                setTextViewText(R.id.widget_countdown_1, "Đã kết nối")
                setTextViewText(R.id.widget_location_1, "")
                setTextViewText(R.id.widget_note_1, "")

                setTextViewText(R.id.widget_status_2, "TEST")
                setTextViewText(R.id.widget_title_2, "Widget NextA")
                setTextViewText(R.id.widget_time_2, "4 x 2")
                setTextViewText(R.id.widget_countdown_2, "Render OK")
                setTextViewText(R.id.widget_location_2, "")
                setTextViewText(R.id.widget_note_2, "")
            }
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_REFRESH = "com.nexta.widget.ACTION_REFRESH"

        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, NextAWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                NextAWidgetProvider().onUpdate(context, manager, ids)
            }
        }
    }
}
